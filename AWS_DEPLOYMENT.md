# AWS Deployment Guide for Proxy PAD

This guide will help you deploy the Proxy PAD application to AWS ECS using GitHub Actions.

## Architecture

The application is deployed on AWS using the following services:

- **Amazon ECS (Fargate)** - Container orchestration
- **Amazon ECR** - Docker image registry
- **Application Load Balancer (ALB)** - Traffic distribution
- **CloudWatch** - Logging and monitoring
- **VPC** - Network isolation

### Container Architecture

The deployment includes three containers in a single ECS task:

1. **postgres** - PostgreSQL database (port 5432)
2. **redis** - Redis cache (port 6379)
3. **proxy-pad-app** - Main application with:
   - Proxy service (port 8080) - Main entry point
   - Movie API Node 1 (port 9001)
   - Movie API Node 2 (port 9002)

## Prerequisites

Before deploying, ensure you have:

1. ✅ AWS Account (ID: 427547500777)
2. ✅ AWS CLI installed and configured
3. ✅ GitHub repository with proper access
4. ✅ IAM user with necessary permissions

## Step 1: Create AWS Resources

### 1.1 Create IAM Roles

You need two IAM roles:

#### ecsTaskExecutionRole

This role allows ECS to pull images from ECR and write logs to CloudWatch.

```bash
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }' \
  --region eu-north-1

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy \
  --region eu-north-1
```

#### ecsTaskRole

This role allows your application containers to access other AWS services.

```bash
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }' \
  --region eu-north-1
```

### 1.2 Create ECR Repository

```bash
aws ecr create-repository \
  --repository-name fourth-lab-proxy \
  --region eu-north-1
```

### 1.3 Create CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /ecs/fourth-lab \
  --region eu-north-1
```

### 1.4 Create ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name fourth-lab-cluster \
  --region eu-north-1
```

### 1.5 Create Application Load Balancer (Optional but Recommended)

First, get your VPC and subnet IDs:

```bash
# Get default VPC
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=isDefault,Values=true" \
  --query "Vpcs[0].VpcId" \
  --output text \
  --region eu-north-1)

# Get subnet IDs
SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query "Subnets[*].SubnetId" \
  --output text \
  --region eu-north-1)

# Create security group for ALB
ALB_SG=$(aws ec2 create-security-group \
  --group-name fourth-lab-alb-sg \
  --description "Security group for Fourth Lab ALB" \
  --vpc-id $VPC_ID \
  --region eu-north-1 \
  --query "GroupId" \
  --output text)

# Allow HTTP traffic
aws ec2 authorize-security-group-ingress \
  --group-id $ALB_SG \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0 \
  --region eu-north-1

# Create ALB
aws elbv2 create-load-balancer \
  --name fourth-lab-alb \
  --subnets $SUBNET_IDS \
  --security-groups $ALB_SG \
  --region eu-north-1
```

### 1.6 Create Security Group for ECS Tasks

```bash
# Create security group for ECS tasks
ECS_SG=$(aws ec2 create-security-group \
  --group-name fourth-lab-ecs-sg \
  --description "Security group for Fourth Lab ECS tasks" \
  --vpc-id $VPC_ID \
  --region eu-north-1 \
  --query "GroupId" \
  --output text)

# Allow traffic from ALB
aws ec2 authorize-security-group-ingress \
  --group-id $ECS_SG \
  --protocol tcp \
  --port 8080 \
  --source-group $ALB_SG \
  --region eu-north-1

# Allow all traffic within the security group (for container communication)
aws ec2 authorize-security-group-ingress \
  --group-id $ECS_SG \
  --protocol -1 \
  --source-group $ECS_SG \
  --region eu-north-1
```

### 1.7 Create Target Group and Listener

```bash
# Get ALB ARN
ALB_ARN=$(aws elbv2 describe-load-balancers \
  --names fourth-lab-alb \
  --query "LoadBalancers[0].LoadBalancerArn" \
  --output text \
  --region eu-north-1)

# Create target group
TG_ARN=$(aws elbv2 create-target-group \
  --name fourth-lab-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path /actuator/health \
  --health-check-interval-seconds 30 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3 \
  --region eu-north-1 \
  --query "TargetGroups[0].TargetGroupArn" \
  --output text)

# Create listener
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn=$TG_ARN \
  --region eu-north-1
```

### 1.8 Create ECS Service

```bash
# Get subnet IDs (comma-separated)
SUBNET_1=$(echo $SUBNET_IDS | awk '{print $1}')
SUBNET_2=$(echo $SUBNET_IDS | awk '{print $2}')

# Register task definition first (this will be updated by GitHub Actions)
aws ecs register-task-definition \
  --cli-input-json file://.aws/task-definition.json \
  --region eu-north-1

# Create ECS service
aws ecs create-service \
  --cluster fourth-lab-cluster \
  --service-name fourth-lab-service \
  --task-definition fourth-lab-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_1,$SUBNET_2],securityGroups=[$ECS_SG],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=$TG_ARN,containerName=proxy-pad-app,containerPort=8080" \
  --region eu-north-1
```

## Step 2: Configure GitHub Secrets

Go to your GitHub repository settings and add the following secrets:

### Required Secrets

1. **AWS_ACCESS_KEY_ID**
   - Your AWS IAM user access key ID
   - Get it from AWS IAM Console

2. **AWS_SECRET_ACCESS_KEY**
   - Your AWS IAM user secret access key
   - Get it from AWS IAM Console

### How to Create IAM User for GitHub Actions

```bash
# Create IAM user
aws iam create-user --user-name github-actions-fourth-lab

# Create access key
aws iam create-access-key --user-name github-actions-fourth-lab
```

Save the AccessKeyId and SecretAccessKey from the output.

### Attach Required Policies

```bash
# Attach ECR permissions
aws iam attach-user-policy \
  --user-name github-actions-fourth-lab \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser

# Attach ECS permissions
aws iam attach-user-policy \
  --user-name github-actions-fourth-lab \
  --policy-arn arn:aws:iam::aws:policy/AmazonECS_FullAccess
```

## Step 3: Deploy

### Automatic Deployment

Once you push to the `main` branch, GitHub Actions will automatically:

1. ✅ Build the Docker image
2. ✅ Push it to Amazon ECR
3. ✅ Update the ECS task definition
4. ✅ Deploy to ECS service
5. ✅ Wait for service stability

### Manual Deployment

You can also trigger deployment manually:

1. Go to GitHub Actions tab
2. Select "Deploy to Amazon ECS" workflow
3. Click "Run workflow"
4. Select the branch and click "Run workflow"

## Step 4: Verify Deployment

### Check ECS Service Status

```bash
aws ecs describe-services \
  --cluster fourth-lab-cluster \
  --services fourth-lab-service \
  --region eu-north-1
```

### Check Task Status

```bash
aws ecs list-tasks \
  --cluster fourth-lab-cluster \
  --service-name fourth-lab-service \
  --region eu-north-1
```

### View Logs

```bash
# Get task ARN
TASK_ARN=$(aws ecs list-tasks \
  --cluster fourth-lab-cluster \
  --service-name fourth-lab-service \
  --query "taskArns[0]" \
  --output text \
  --region eu-north-1)

# View proxy logs
aws logs tail /ecs/fourth-lab --follow --region eu-north-1 --filter-pattern "proxy"

# View postgres logs
aws logs tail /ecs/fourth-lab --follow --region eu-north-1 --filter-pattern "postgres"

# View redis logs
aws logs tail /ecs/fourth-lab --follow --region eu-north-1 --filter-pattern "redis"
```

### Get ALB DNS Name

```bash
aws elbv2 describe-load-balancers \
  --names fourth-lab-alb \
  --query "LoadBalancers[0].DNSName" \
  --output text \
  --region eu-north-1
```

Test the application:

```bash
# Replace with your ALB DNS name
ALB_DNS="your-alb-dns-name.eu-north-1.elb.amazonaws.com"

# Test proxy endpoint
curl http://$ALB_DNS/api/movies

# Create a movie
curl -X POST http://$ALB_DNS/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Inception","rating":8.8}'

# Get all movies
curl http://$ALB_DNS/api/movies
```

## Configuration

### Environment Variables

The following environment variables are configured in the ECS task definition:

- `SPRING_DATA_REDIS_HOST=localhost`
- `SPRING_DATA_REDIS_PORT=6379`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/proxydb`
- `SPRING_DATASOURCE_USERNAME=admin`
- `SPRING_DATASOURCE_PASSWORD=adminpass`
- `PROXY_DATAWAREHOUSE_NODES=http://localhost:9001,http://localhost:9002`

### Resource Allocation

- **CPU**: 1 vCPU (1024 CPU units)
- **Memory**: 2 GB (2048 MB)

### Scaling

To scale the service:

```bash
aws ecs update-service \
  --cluster fourth-lab-cluster \
  --service fourth-lab-service \
  --desired-count 2 \
  --region eu-north-1
```

## Troubleshooting

### Container Fails to Start

1. Check CloudWatch logs:
   ```bash
   aws logs tail /ecs/fourth-lab --follow --region eu-north-1
   ```

2. Check task definition:
   ```bash
   aws ecs describe-task-definition \
     --task-definition fourth-lab-task \
     --region eu-north-1
   ```

### Service Fails to Deploy

1. Check service events:
   ```bash
   aws ecs describe-services \
     --cluster fourth-lab-cluster \
     --services fourth-lab-service \
     --region eu-north-1 \
     --query "services[0].events[0:10]"
   ```

2. Check task stopped reason:
   ```bash
   aws ecs describe-tasks \
     --cluster fourth-lab-cluster \
     --tasks TASK_ARN \
     --region eu-north-1
   ```

### Health Check Failures

The application includes health checks:

- **Redis**: `redis-cli ping`
- **PostgreSQL**: `pg_isready`
- **Application**: `wget http://localhost:8080/actuator/health`

Make sure to add Spring Boot Actuator dependency if health check endpoint is needed.

### Database Connection Issues

If the application can't connect to PostgreSQL:

1. Check if PostgreSQL container is running
2. Verify environment variables
3. Check CloudWatch logs for connection errors

## Cost Optimization

### Development Environment

For development, you can reduce costs by:

1. Using smaller task sizes (512 CPU, 1024 Memory)
2. Running only one task (desired count = 1)
3. Using Fargate Spot for non-production environments

### Production Environment

For production:

1. Enable auto-scaling based on CPU/Memory
2. Use Application Load Balancer health checks
3. Enable container insights for monitoring
4. Set up CloudWatch alarms

## Cleanup

To delete all resources:

```bash
# Delete ECS service
aws ecs delete-service \
  --cluster fourth-lab-cluster \
  --service fourth-lab-service \
  --force \
  --region eu-north-1

# Delete ECS cluster
aws ecs delete-cluster \
  --cluster fourth-lab-cluster \
  --region eu-north-1

# Delete ALB
aws elbv2 delete-load-balancer \
  --load-balancer-arn $ALB_ARN \
  --region eu-north-1

# Delete target group
aws elbv2 delete-target-group \
  --target-group-arn $TG_ARN \
  --region eu-north-1

# Delete security groups
aws ec2 delete-security-group --group-id $ECS_SG --region eu-north-1
aws ec2 delete-security-group --group-id $ALB_SG --region eu-north-1

# Delete ECR repository
aws ecr delete-repository \
  --repository-name fourth-lab-proxy \
  --force \
  --region eu-north-1

# Delete log group
aws logs delete-log-group \
  --log-group-name /ecs/fourth-lab \
  --region eu-north-1
```

## Security Best Practices

1. ✅ Use IAM roles instead of hardcoded credentials
2. ✅ Enable CloudWatch logging for all containers
3. ✅ Use security groups to restrict network access
4. ✅ Store sensitive data in AWS Secrets Manager
5. ✅ Enable VPC Flow Logs for network monitoring
6. ✅ Use HTTPS with ACM certificates for production
7. ✅ Implement least privilege access for IAM roles

## Additional Resources

- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS ECR Documentation](https://docs.aws.amazon.com/ecr/)
- [GitHub Actions AWS Deployment](https://github.com/aws-actions)
- [Spring Boot on AWS](https://aws.amazon.com/blogs/opensource/spring-boot-on-aws/)

## Support

For issues or questions:
- Check CloudWatch logs first
- Review GitHub Actions workflow runs
- Consult AWS ECS service events
- Check this documentation for troubleshooting steps
