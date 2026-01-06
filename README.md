
```markdown
# STaaS - Cloud-Based Smart Drive

A cloud-based smart drive application with Java Spring Boot backend, MongoDB, AWS S3 integration, and a web frontend. Includes Python infrastructure scripts for AWS provisioning.

---

## Prerequisites

- Java 22, Maven, Docker  
- Python 3 + Boto3 (for infrastructure scripts)  
- AWS account with credentials configured  
- S3 bucket in target region (default: `ca-central-1`)  

---

## Quick Start

### 1. Start MongoDB

```bash
docker run -d --name staas-mongo -p 27017:27017 mongo:7
```

### 2. Build & Run Backend

```bash
cd applicationcode/project
docker build -t onlinedrive .
docker run -p 8080:8080 \
  -e AWS_ACCESS_KEY_ID=<your-key> \
  -e AWS_SECRET_ACCESS_KEY=<your-secret> \
  -e S3_BUCKET=<your-bucket> \
  -e AWS_REGION=ca-central-1 \
  onlinedrive
```

### 3. Access Frontend

Open your browser: `http://localhost:8080/smartdrive-frontend/`

- Register or login
- Create folders
- Upload and download files (stored in S3, metadata in MongoDB)

---

## Optional: AWS Infrastructure Provisioning

### Install Dependencies

```bash
pip install boto3
export AWS_PROFILE=<your-profile>
# or export AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
```

### Step 1: Network Setup + Private EC2

```bash
python Infrastructurecode/networkinfra1.py
```

Save the printed outputs: `public_subnet_id`, `private_route_table_id`, `bastion_sg_id`, `ami_id`, `private_ip`

### Step 2: NAT Gateway + Bastion Host + Nginx

1. Edit `Infrastructurecode/part2infra.py` and replace placeholders with Step 1 outputs
2. Run:

```bash
python Infrastructurecode/part2infra.py
```

3. Use the bastion Elastic IP to access the private EC2 through Nginx

---

## Architecture

```
Browser → (optional) Bastion/Nginx → Spring Boot API
Spring Boot → MongoDB (metadata), AWS S3 (files)
AWS VPC → public + private subnets, NAT gateway, security groups
```

---

## Docker Summary

| Service  | Container/Image | Notes                                      |
|----------|----------------|--------------------------------------------|
| Backend  | `onlinedrive`  | Requires AWS credentials + S3 bucket       |
| MongoDB  | `mongo:7`      | Port 27017, connect via host.docker.internal |
| Nginx    | Part2infra.py  | Optional bastion proxy                     |

---

## Features

- File and folder management
- Containerized deployment
- Cloud storage integration (AWS S3)
- Secure AWS infrastructure with VPC, subnets, and bastion host
- Pre-signed URLs for direct S3 uploads

---
