#!/bin/bash

# AWS Setup Script for Fourth Lab Proxy PAD
# This script creates all necessary AWS resources for deployment

set -e

# Configuration
REGION="eu-north-1"
PROJECT_NAME="fourth-lab"
ECR_REPO="${PROJECT_NAME}-proxy"
ECS_CLUSTER="${PROJECT_NAME}-cluster"
ECS_SERVICE="${PROJECT_NAME}-service"
LOG_GROUP="/ecs/${PROJECT_NAME}"
ALB_NAME="${PROJECT_NAME}-alb"
TG_NAME="${PROJECT_NAME}-tg"

echo "🚀 Starting AWS resource setup for ${PROJECT_NAME}"
echo "Region: ${REGION}"
echo ""

# Function to check if resource exists
check_resource() {
    local resource_type=$1
    local resource_name=$2
    echo "Checking if ${resource_type} '${resource_name}' exists..."
}

# 1. Create IAM Roles
echo "📋 Step 1: Creating IAM Roles"
echo "----------------------------"

# Check if ecsTaskExecutionRole exists
if aws iam get-role --role-name ecsTaskExecutionRole --region ${REGION} 2>/dev/null; then
    echo "✓ ecsTaskExecutionRole already exists"
else
    echo "Creating ecsTaskExecutionRole..."
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
        --region ${REGION}

    aws iam attach-role-policy \
        --role-name ecsTaskExecutionRole \
        --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy \
        --region ${REGION}
    echo "✓ Created ecsTaskExecutionRole"
fi

# Check if ecsTaskRole exists
if aws iam get-role --role-name ecsTaskRole --region ${REGION} 2>/dev/null; then
    echo "✓ ecsTaskRole already exists"
else
    echo "Creating ecsTaskRole..."
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
        --region ${REGION}
    echo "✓ Created ecsTaskRole"
fi

# 2. Create ECR Repository
echo ""
echo "🐳 Step 2: Creating ECR Repository"
echo "----------------------------"
if aws ecr describe-repositories --repository-names ${ECR_REPO} --region ${REGION} 2>/dev/null; then
    echo "✓ ECR repository '${ECR_REPO}' already exists"
else
    aws ecr create-repository \
        --repository-name ${ECR_REPO} \
        --region ${REGION}
    echo "✓ Created ECR repository '${ECR_REPO}'"
fi

# Get ECR URI
ECR_URI=$(aws ecr describe-repositories \
    --repository-names ${ECR_REPO} \
    --query "repositories[0].repositoryUri" \
    --output text \
    --region ${REGION})
echo "ECR URI: ${ECR_URI}"

# 3. Create CloudWatch Log Group
echo ""
echo "📊 Step 3: Creating CloudWatch Log Group"
echo "----------------------------"
if aws logs describe-log-groups --log-group-name-prefix ${LOG_GROUP} --region ${REGION} | grep -q ${LOG_GROUP}; then
    echo "✓ Log group '${LOG_GROUP}' already exists"
else
    aws logs create-log-group \
        --log-group-name ${LOG_GROUP} \
        --region ${REGION}
    echo "✓ Created log group '${LOG_GROUP}'"
fi

# 4. Create ECS Cluster
echo ""
echo "🎯 Step 4: Creating ECS Cluster"
echo "----------------------------"
if aws ecs describe-clusters --clusters ${ECS_CLUSTER} --region ${REGION} | grep -q "ACTIVE"; then
    echo "✓ ECS cluster '${ECS_CLUSTER}' already exists"
else
    aws ecs create-cluster \
        --cluster-name ${ECS_CLUSTER} \
        --region ${REGION}
    echo "✓ Created ECS cluster '${ECS_CLUSTER}'"
fi

# 5. Get VPC and Subnet Information
echo ""
echo "🌐 Step 5: Getting VPC and Subnet Information"
echo "----------------------------"
VPC_ID=$(aws ec2 describe-vpcs \
    --filters "Name=isDefault,Values=true" \
    --query "Vpcs[0].VpcId" \
    --output text \
    --region ${REGION})
echo "VPC ID: ${VPC_ID}"

SUBNET_IDS=$(aws ec2 describe-subnets \
    --filters "Name=vpc-id,Values=${VPC_ID}" \
    --query "Subnets[*].SubnetId" \
    --output text \
    --region ${REGION})
SUBNET_1=$(echo ${SUBNET_IDS} | awk '{print $1}')
SUBNET_2=$(echo ${SUBNET_IDS} | awk '{print $2}')
echo "Subnet 1: ${SUBNET_1}"
echo "Subnet 2: ${SUBNET_2}"

# 6. Create Security Groups
echo ""
echo "🔒 Step 6: Creating Security Groups"
echo "----------------------------"

# ALB Security Group
ALB_SG=$(aws ec2 describe-security-groups \
    --filters "Name=group-name,Values=${PROJECT_NAME}-alb-sg" "Name=vpc-id,Values=${VPC_ID}" \
    --query "SecurityGroups[0].GroupId" \
    --output text \
    --region ${REGION} 2>/dev/null)

if [ "${ALB_SG}" != "None" ] && [ -n "${ALB_SG}" ]; then
    echo "✓ ALB security group already exists: ${ALB_SG}"
else
    ALB_SG=$(aws ec2 create-security-group \
        --group-name ${PROJECT_NAME}-alb-sg \
        --description "Security group for ${PROJECT_NAME} ALB" \
        --vpc-id ${VPC_ID} \
        --region ${REGION} \
        --query "GroupId" \
        --output text)

    aws ec2 authorize-security-group-ingress \
        --group-id ${ALB_SG} \
        --protocol tcp \
        --port 80 \
        --cidr 0.0.0.0/0 \
        --region ${REGION}

    echo "✓ Created ALB security group: ${ALB_SG}"
fi

# ECS Security Group
ECS_SG=$(aws ec2 describe-security-groups \
    --filters "Name=group-name,Values=${PROJECT_NAME}-ecs-sg" "Name=vpc-id,Values=${VPC_ID}" \
    --query "SecurityGroups[0].GroupId" \
    --output text \
    --region ${REGION} 2>/dev/null)

if [ "${ECS_SG}" != "None" ] && [ -n "${ECS_SG}" ]; then
    echo "✓ ECS security group already exists: ${ECS_SG}"
else
    ECS_SG=$(aws ec2 create-security-group \
        --group-name ${PROJECT_NAME}-ecs-sg \
        --description "Security group for ${PROJECT_NAME} ECS tasks" \
        --vpc-id ${VPC_ID} \
        --region ${REGION} \
        --query "GroupId" \
        --output text)

    # Allow traffic from ALB
    aws ec2 authorize-security-group-ingress \
        --group-id ${ECS_SG} \
        --protocol tcp \
        --port 8080 \
        --source-group ${ALB_SG} \
        --region ${REGION}

    # Allow all traffic within the security group
    aws ec2 authorize-security-group-ingress \
        --group-id ${ECS_SG} \
        --protocol -1 \
        --source-group ${ECS_SG} \
        --region ${REGION}

    echo "✓ Created ECS security group: ${ECS_SG}"
fi

# 7. Create Application Load Balancer
echo ""
echo "⚖️  Step 7: Creating Application Load Balancer"
echo "----------------------------"
ALB_ARN=$(aws elbv2 describe-load-balancers \
    --names ${ALB_NAME} \
    --query "LoadBalancers[0].LoadBalancerArn" \
    --output text \
    --region ${REGION} 2>/dev/null || echo "")

if [ -n "${ALB_ARN}" ] && [ "${ALB_ARN}" != "None" ]; then
    echo "✓ ALB already exists: ${ALB_NAME}"
else
    ALB_ARN=$(aws elbv2 create-load-balancer \
        --name ${ALB_NAME} \
        --subnets ${SUBNET_1} ${SUBNET_2} \
        --security-groups ${ALB_SG} \
        --region ${REGION} \
        --query "LoadBalancers[0].LoadBalancerArn" \
        --output text)
    echo "✓ Created ALB: ${ALB_NAME}"
    echo "Waiting for ALB to become active..."
    sleep 30
fi

ALB_DNS=$(aws elbv2 describe-load-balancers \
    --load-balancer-arns ${ALB_ARN} \
    --query "LoadBalancers[0].DNSName" \
    --output text \
    --region ${REGION})
echo "ALB DNS: ${ALB_DNS}"

# 8. Create Target Group
echo ""
echo "🎯 Step 8: Creating Target Group"
echo "----------------------------"
TG_ARN=$(aws elbv2 describe-target-groups \
    --names ${TG_NAME} \
    --query "TargetGroups[0].TargetGroupArn" \
    --output text \
    --region ${REGION} 2>/dev/null || echo "")

if [ -n "${TG_ARN}" ] && [ "${TG_ARN}" != "None" ]; then
    echo "✓ Target group already exists: ${TG_NAME}"
else
    TG_ARN=$(aws elbv2 create-target-group \
        --name ${TG_NAME} \
        --protocol HTTP \
        --port 8080 \
        --vpc-id ${VPC_ID} \
        --target-type ip \
        --health-check-path / \
        --health-check-interval-seconds 30 \
        --healthy-threshold-count 2 \
        --unhealthy-threshold-count 3 \
        --region ${REGION} \
        --query "TargetGroups[0].TargetGroupArn" \
        --output text)
    echo "✓ Created target group: ${TG_NAME}"
fi

# 9. Create ALB Listener
echo ""
echo "👂 Step 9: Creating ALB Listener"
echo "----------------------------"
LISTENER_ARN=$(aws elbv2 describe-listeners \
    --load-balancer-arn ${ALB_ARN} \
    --query "Listeners[0].ListenerArn" \
    --output text \
    --region ${REGION} 2>/dev/null || echo "")

if [ -n "${LISTENER_ARN}" ] && [ "${LISTENER_ARN}" != "None" ]; then
    echo "✓ Listener already exists"
else
    aws elbv2 create-listener \
        --load-balancer-arn ${ALB_ARN} \
        --protocol HTTP \
        --port 80 \
        --default-actions Type=forward,TargetGroupArn=${TG_ARN} \
        --region ${REGION}
    echo "✓ Created listener"
fi

# 10. Register Task Definition
echo ""
echo "📝 Step 10: Registering ECS Task Definition"
echo "----------------------------"
aws ecs register-task-definition \
    --cli-input-json file://.aws/task-definition.json \
    --region ${REGION}
echo "✓ Registered task definition"

# 11. Create ECS Service
echo ""
echo "🚀 Step 11: Creating ECS Service"
echo "----------------------------"
if aws ecs describe-services \
    --cluster ${ECS_CLUSTER} \
    --services ${ECS_SERVICE} \
    --region ${REGION} | grep -q "ACTIVE"; then
    echo "✓ ECS service already exists: ${ECS_SERVICE}"
    echo "Updating service..."
    aws ecs update-service \
        --cluster ${ECS_CLUSTER} \
        --service ${ECS_SERVICE} \
        --task-definition fourth-lab-task \
        --region ${REGION}
else
    aws ecs create-service \
        --cluster ${ECS_CLUSTER} \
        --service-name ${ECS_SERVICE} \
        --task-definition fourth-lab-task \
        --desired-count 1 \
        --launch-type FARGATE \
        --network-configuration "awsvpcConfiguration={subnets=[${SUBNET_1},${SUBNET_2}],securityGroups=[${ECS_SG}],assignPublicIp=ENABLED}" \
        --load-balancers "targetGroupArn=${TG_ARN},containerName=proxy-pad-app,containerPort=8080" \
        --region ${REGION}
    echo "✓ Created ECS service: ${ECS_SERVICE}"
fi

# Summary
echo ""
echo "✅ Setup Complete!"
echo "===================="
echo ""
echo "Resources created:"
echo "  - ECR Repository: ${ECR_URI}"
echo "  - ECS Cluster: ${ECS_CLUSTER}"
echo "  - ECS Service: ${ECS_SERVICE}"
echo "  - Load Balancer: ${ALB_DNS}"
echo "  - Log Group: ${LOG_GROUP}"
echo ""
echo "Next steps:"
echo "  1. Configure GitHub Secrets (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)"
echo "  2. Push to main branch to trigger deployment"
echo "  3. Access application at: http://${ALB_DNS}"
echo ""
echo "To view logs:"
echo "  aws logs tail ${LOG_GROUP} --follow --region ${REGION}"
echo ""
echo "To check service status:"
echo "  aws ecs describe-services --cluster ${ECS_CLUSTER} --services ${ECS_SERVICE} --region ${REGION}"
echo ""
