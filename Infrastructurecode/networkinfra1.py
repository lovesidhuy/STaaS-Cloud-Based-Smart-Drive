import boto3
import time

# Initialize Boto3 clients for ca-central-1 region
region = "ca-central-1"
ec2_client = boto3.client("ec2", region_name=region)
ssm_client = boto3.client("ssm", region_name=region)

def get_latest_ami():
    response = ssm_client.get_parameter(Name="/aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-x86_64-gp2")
    ami_id = response["Parameter"]["Value"]
    print(f"Latest Amazon Linux 2 AMI ID: {ami_id}")
    return ami_id

def create_vpc():
    vpc = ec2_client.create_vpc(CidrBlock="10.0.0.0/16")
    vpc_id = vpc["Vpc"]["VpcId"]
    print(f"VPC Created with ID: {vpc_id}")
    ec2_client.modify_vpc_attribute(VpcId=vpc_id, EnableDnsSupport={"Value": True})
    ec2_client.modify_vpc_attribute(VpcId=vpc_id, EnableDnsHostnames={"Value": True})    
    return vpc_id

def create_subnets(vpc_id):
    public_subnet = ec2_client.create_subnet(VpcId=vpc_id, CidrBlock="10.0.1.0/24", AvailabilityZone="ca-central-1a")
    public_subnet_id = public_subnet["Subnet"]["SubnetId"]
    print(f"Public Subnet Created with ID: {public_subnet_id}")
    ec2_client.modify_subnet_attribute(SubnetId=public_subnet_id, MapPublicIpOnLaunch={"Value": True})
    
    private_subnet = ec2_client.create_subnet(VpcId=vpc_id, CidrBlock="10.0.2.0/24", AvailabilityZone="ca-central-1a")
    private_subnet_id = private_subnet["Subnet"]["SubnetId"]
    print(f"Private Subnet Created with ID: {private_subnet_id}")
    
    return public_subnet_id, private_subnet_id

def create_internet_gateway(vpc_id):
    igw = ec2_client.create_internet_gateway()
    igw_id = igw["InternetGateway"]["InternetGatewayId"]
    ec2_client.attach_internet_gateway(InternetGatewayId=igw_id, VpcId=vpc_id)
    print(f"Internet Gateway Created and Attached with ID: {igw_id}")
    return igw_id

def create_route_tables(vpc_id, public_subnet_id, private_subnet_id, igw_id):
    public_rt = ec2_client.create_route_table(VpcId=vpc_id)
    public_rt_id = public_rt["RouteTable"]["RouteTableId"]
    ec2_client.create_route(RouteTableId=public_rt_id, DestinationCidrBlock="0.0.0.0/0", GatewayId=igw_id)
    ec2_client.associate_route_table(RouteTableId=public_rt_id, SubnetId=public_subnet_id)
    print(f"Public Route Table Created with ID: {public_rt_id}")
    
    private_rt = ec2_client.create_route_table(VpcId=vpc_id)
    private_rt_id = private_rt["RouteTable"]["RouteTableId"]
    # Note: NAT Gateway route will be added in Part 2
    ec2_client.associate_route_table(RouteTableId=private_rt_id, SubnetId=private_subnet_id)
    print(f"Private Route Table Created with ID: {private_rt_id}")
    return private_rt_id

def create_security_groups(vpc_id):
    private_sg = ec2_client.create_security_group(GroupName="PrivateEC2SecurityGroup", Description="Private SG", VpcId=vpc_id)
    private_sg_id = private_sg["GroupId"]
    print(f"Private Security Group Created with ID: {private_sg_id}")
    
    bastion_sg = ec2_client.create_security_group(GroupName="BastionSecurityGroup", Description="Bastion SG", VpcId=vpc_id)
    bastion_sg_id = bastion_sg["GroupId"]
    print(f"Bastion Security Group Created with ID: {bastion_sg_id}")
    
    ec2_client.authorize_security_group_ingress(GroupId=bastion_sg_id, IpPermissions=[
        {"IpProtocol": "tcp", "FromPort": 22, "ToPort": 22, "IpRanges": [{"CidrIp": "0.0.0.0/0"}]},
        {"IpProtocol": "tcp", "FromPort": 80, "ToPort": 80, "IpRanges": [{"CidrIp": "0.0.0.0/0"}]}
    ])
    
    ec2_client.authorize_security_group_ingress(GroupId=private_sg_id, IpPermissions=[
        {"IpProtocol": "tcp", "FromPort": 80, "ToPort": 80, "UserIdGroupPairs": [{"GroupId": bastion_sg_id}]},
        {"IpProtocol": "tcp", "FromPort": 22, "ToPort": 22, "UserIdGroupPairs": [{"GroupId": bastion_sg_id}]}
    ])
    
    return private_sg_id, bastion_sg_id

def launch_private_ec2_instance(private_subnet_id, private_sg_id, ami_id):
    user_data = """#!/bin/bash
yum update -y
yum install -y docker
service docker start
chkconfig docker on
usermod -a -G docker ec2-user

# Create HTML content
mkdir -p /tmp/html
echo '<html><body><h1>Hello World from Private EC2 (Dockerized)!</h1><p>This server is running Apache in a Docker container in a private subnet.</p></body></html>' > /tmp/html/index.html

# Pull and run Apache Docker container
docker run -d --name apache -p 80:80 -v /tmp/html:/usr/local/apache2/htdocs/ httpd:2.4
"""
    ec2_resource = boto3.resource("ec2", region_name=region)
    instance = ec2_resource.create_instances(ImageId=ami_id, MinCount=1, MaxCount=1, InstanceType="t3.micro", KeyName="your-key-pair", SubnetId=private_subnet_id, SecurityGroupIds=[private_sg_id], UserData=user_data, TagSpecifications=[{"ResourceType": "instance", "Tags": [{"Key": "Name", "Value": "Private-EC2-Docker-WebServer"}]}])
    instance_id = instance[0].id
    print(f"Private EC2 Instance Launched with ID: {instance_id}")
    instance[0].wait_until_running()
    instance[0].reload()
    private_ip = instance[0].private_ip_address
    print(f"Instance {instance_id} running with Private IP: {private_ip}")
    return instance_id, private_ip

def main():
    print("=" * 80)
    print("Part 1: AWS VPC Infrastructure Setup (with Dockerized Private EC2)")
    print("=" * 80)
    
    try:
        ami_id = get_latest_ami()
        vpc_id = create_vpc()
        public_subnet_id, private_subnet_id = create_subnets(vpc_id)
        igw_id = create_internet_gateway(vpc_id)
        private_route_table_id = create_route_tables(vpc_id, public_subnet_id, private_subnet_id, igw_id)
        private_sg_id, bastion_sg_id = create_security_groups(vpc_id)
        instance_id, private_ip = launch_private_ec2_instance(private_subnet_id, private_sg_id, ami_id)
        
        print("\nPart 1 Complete! Infrastructure Created.")
        print(f"VPC: {vpc_id}")
        print(f"Public Subnet: {public_subnet_id}")
        print(f"Private Subnet: {private_subnet_id}")
        print(f"Private Route Table: {private_route_table_id}")
        print(f"Private EC2: {instance_id} (IP: {private_ip})")
        print(f"Security Groups: Private={private_sg_id}, Bastion={bastion_sg_id}")
        print("\nCopy these values to run Part 2:")
        print(f"- public_subnet_id: {public_subnet_id}")
        print(f"- private_route_table_id: {private_route_table_id}")
        print(f"- bastion_sg_id: {bastion_sg_id}")
        print(f"- ami_id: {ami_id}")
        print(f"- private_ip: {private_ip}")
        
        # Wait for private EC2 init (shared with Part 2)
        time.sleep(30)
        
    except Exception as e:
        print(f"Error: {e}")
        raise

if __name__ == "__main__":
    main()
