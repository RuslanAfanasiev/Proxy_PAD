# Quick Start Guide - AWS Deployment

## Prerequisites

- AWS Account (427547500777)
- AWS CLI installed and configured
- GitHub repository access
- Region: eu-north-1

## 1. One-Command Setup

Run the automated setup script:

```bash
cd /path/to/Proxy_PAD
chmod +x .aws/setup-aws-resources.sh
./.aws/setup-aws-resources.sh
```

This script will create:
- ✅ IAM Roles (ecsTaskExecutionRole, ecsTaskRole)
- ✅ ECR Repository (fourth-lab-proxy)
- ✅ ECS Cluster (fourth-lab-cluster)
- ✅ Application Load Balancer
- ✅ Target Groups
- ✅ Security Groups
- ✅ CloudWatch Log Groups
- ✅ ECS Service (fourth-lab-service)

## 2. Configure GitHub Secrets

Create an IAM user for GitHub Actions:

```bash
# Create user
aws iam create-user --user-name github-actions-fourth-lab --region eu-north-1

# Create access key
aws iam create-access-key --user-name github-actions-fourth-lab --region eu-north-1
```

Copy the `AccessKeyId` and `SecretAccessKey`.

Attach permissions:

```bash
aws iam attach-user-policy \
  --user-name github-actions-fourth-lab \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser

aws iam attach-user-policy \
  --user-name github-actions-fourth-lab \
  --policy-arn arn:aws:iam::aws:policy/AmazonECS_FullAccess
```

Add secrets to GitHub:
1. Go to your repository → Settings → Secrets and variables → Actions
2. Add `AWS_ACCESS_KEY_ID` = your access key
3. Add `AWS_SECRET_ACCESS_KEY` = your secret key

## 3. Deploy

Push to main branch:

```bash
git add .
git commit -m "Configure AWS deployment"
git push origin main
```

Or manually trigger the workflow:
- Go to Actions tab
- Select "Deploy to Amazon ECS"
- Click "Run workflow"

## 4. Access Your Application

Get the ALB DNS name:

```bash
aws elbv2 describe-load-balancers \
  --names fourth-lab-alb \
  --query "LoadBalancers[0].DNSName" \
  --output text \
  --region eu-north-1
```

Test the application:

```bash
# Replace with your ALB DNS
ALB_DNS="your-alb-dns.eu-north-1.elb.amazonaws.com"

# Get all movies
curl http://$ALB_DNS/api/movies

# Create a movie
curl -X POST http://$ALB_DNS/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"The Matrix","rating":8.7}'
```

## 5. Monitor Deployment

View logs:

```bash
aws logs tail /ecs/fourth-lab --follow --region eu-north-1
```

Check service status:

```bash
aws ecs describe-services \
  --cluster fourth-lab-cluster \
  --services fourth-lab-service \
  --region eu-north-1
```

## Troubleshooting

### Deployment fails?

Check GitHub Actions logs:
1. Go to Actions tab
2. Click on the failed workflow run
3. Review the logs

### Service not starting?

Check CloudWatch logs:
```bash
aws logs tail /ecs/fourth-lab --follow --region eu-north-1
```

### Can't access the application?

1. Check security groups allow traffic on port 80
2. Verify ALB is active
3. Check ECS service is running

## Clean Up

To delete all resources:

```bash
# Delete ECS service
aws ecs delete-service \
  --cluster fourth-lab-cluster \
  --service fourth-lab-service \
  --force \
  --region eu-north-1

# Wait for service to be deleted
sleep 30

# Delete ECS cluster
aws ecs delete-cluster \
  --cluster fourth-lab-cluster \
  --region eu-north-1

# Get ALB ARN
ALB_ARN=$(aws elbv2 describe-load-balancers \
  --names fourth-lab-alb \
  --query "LoadBalancers[0].LoadBalancerArn" \
  --output text \
  --region eu-north-1)

# Delete ALB
aws elbv2 delete-load-balancer \
  --load-balancer-arn $ALB_ARN \
  --region eu-north-1

# Get target group ARN
TG_ARN=$(aws elbv2 describe-target-groups \
  --names fourth-lab-tg \
  --query "TargetGroups[0].TargetGroupArn" \
  --output text \
  --region eu-north-1)

# Wait for ALB deletion
sleep 30

# Delete target group
aws elbv2 delete-target-group \
  --target-group-arn $TG_ARN \
  --region eu-north-1

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

## Support

For detailed documentation, see [AWS_DEPLOYMENT.md](../AWS_DEPLOYMENT.md)
