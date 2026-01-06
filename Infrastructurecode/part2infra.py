import boto3

# Initialize Boto3 clients for ca-central-1 region
region = "ca-central-1"
ec2_client = boto3.client("ec2", region_name=region)
cf_client = boto3.client("cloudformation", region_name=region)

def create_nat_gateway_with_cloudformation(private_route_table_id, public_subnet_id):
    template = """{
        "AWSTemplateFormatVersion": "2010-09-09",
        "Resources": {
            "NatGateway": {
                "Type": "AWS::EC2::NatGateway",
                "Properties": {
                    "AllocationId": { "Fn::GetAtt": ["ElasticIP", "AllocationId"] },
                    "SubnetId": "%s"
                }
            },
            "ElasticIP": {
                "Type": "AWS::EC2::EIP",
                "Properties": {
                    "Domain": "vpc"
                }
            },
            "PrivateRoute": {
                "Type": "AWS::EC2::Route",
                "Properties": {
                    "RouteTableId": "%s",
                    "DestinationCidrBlock": "0.0.0.0/0",
                    "NatGatewayId": { "Ref": "NatGateway" }
                }
            }
        }
    }""" % (public_subnet_id, private_route_table_id) 

    stack_name = "NATGatewayStack"
    try:
        # Create the CloudFormation stack
        cf_client.create_stack(StackName=stack_name, TemplateBody=template)
        print(f"CloudFormation stack {stack_name} creation initiated.")
        
        # Wait until the stack creation is complete
        waiter = cf_client.get_waiter("stack_create_complete")
        waiter.wait(StackName=stack_name)
        
        print(f"CloudFormation stack {stack_name} is now complete.")
    except Exception as e:
        # Fetch detailed error events from the stack for better debugging if a failure occurs
        events = cf_client.describe_stack_events(StackName=stack_name)["StackEvents"]
        print("\n--- CloudFormation Rollback Details ---")
        for event in events:
            if "ResourceStatusReason" in event and event["ResourceStatus"] in ["CREATE_FAILED", "ROLLBACK_IN_PROGRESS"]:
                print(f"FAILED: {event['LogicalResourceId']} ({event['ResourceType']}) - Reason: {event['ResourceStatusReason']}")
        print("---------------------------------------")
        
        print(f"Error: {e}")
        raise


def launch_bastion_host(public_subnet_id, bastion_sg_id, ami_id, private_ec2_ip):
    user_data_bastion = f"""#!/bin/bash
yum update -y
yum install -y docker
service docker start
chkconfig docker on
usermod -a -G docker ec2-user

# Create Nginx config directory
mkdir -p /tmp/nginx_conf

# Create reverse proxy config
cat > /tmp/nginx_conf/default.conf << EOF
server {{
    listen 80;
    server_name _;
    location / {{
        # Proxy traffic to the private EC2 instance
        proxy_pass http://{private_ec2_ip}:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }}
}}
EOF

# Pull and run Nginx Docker container, mapping port 80
docker run -d --name nginx-reverse-proxy -p 80:80 -v /tmp/nginx_conf:/etc/nginx/conf.d nginx:latest
echo "Bastion setup complete" > /var/log/bastion-setup.log
"""
    
    ec2_resource = boto3.resource("ec2", region_name=region)
    bastion_host = ec2_resource.create_instances(
        ImageId=ami_id,
        MinCount=1,
        MaxCount=1,
        InstanceType="t3.micro",
        KeyName="your-key-pair",  
        SubnetId=public_subnet_id,
        SecurityGroupIds=[bastion_sg_id],
        UserData=user_data_bastion,
        TagSpecifications=[{"ResourceType": "instance", "Tags": [{"Key": "Name", "Value": "Bastion-Host-Docker-ReverseProxy"}]}]
    )
    
    bastion_host_id = bastion_host[0].id
    print(f"Bastion Host Launched with ID: {bastion_host_id}")
    
    # Wait until the Bastion Host is running
    bastion_host[0].wait_until_running()
    bastion_host[0].reload()

    # Allocate Elastic IP
    eip = ec2_client.allocate_address(Domain="vpc")
    allocation_id = eip["AllocationId"]
    eip_address = eip["PublicIp"]
    
    association = ec2_client.associate_address(InstanceId=bastion_host_id, AllocationId=allocation_id)
    print(f"Elastic IP {eip_address} associated with Bastion Host {bastion_host_id}")
    
    print(f"Bastion Host {bastion_host_id} running with Elastic IP: {eip_address}")
    
    return bastion_host_id, eip_address


def main():
    # Replace with your own values
    public_subnet_id = "your-public-subnet-id"
    private_route_table_id = "your-private-route-table-id"
    bastion_sg_id = "your-bastion-sg-id"
    ami_id = "your-ami-id"
    private_ip = "your-private-ip"
    
    print("=" * 80)
    print("Part 2: AWS Bastion Host Setup with Dockerized Reverse Proxy")
    print("=" * 80)
    
    try:
        # This gives the private EC2 instance internet access via the Public Subnet
        create_nat_gateway_with_cloudformation(private_route_table_id, public_subnet_id)
        
        # Launch Bastion Host with reverse proxy (Nginx)
        # This gives web access to the private EC2 instance via the Bastion's public IP
        bastion_host_id, bastion_public_ip = launch_bastion_host(public_subnet_id, bastion_sg_id, ami_id, private_ip)
        
        # Final setup details
        print("\nSETUP COMPLETE!  Infrastructure Ready.")
        print(f"Bastion (with EIP and Dockerized Nginx): {bastion_host_id} (EIP: {bastion_public_ip})")
        print(f"\nWeb Access: http://{bastion_public_ip}")
        print(f"SSH Bastion: ssh -i your-key-pair.pem ec2-user@{bastion_public_ip}")
        print(f"SSH Private: from Bastion: ssh -i your-key-pair.pem ec2-user@{private_ip}")

    except Exception as e:
        print(f"Final Execution Error: {e}")
        pass


if __name__ == "__main__":
    main()
