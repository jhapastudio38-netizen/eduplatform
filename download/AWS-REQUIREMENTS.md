# AWS Deployment — What I Need From You

To deploy EduPlatform to your AWS account for production (target: 100k concurrent users), I need the following. You can paste them in chat one at a time, or set them as GitHub Secrets directly (preferred — see the secret name in brackets).

## Tier 1 — Required to start

| # | What | Where to get it | GitHub Secret name |
|---|------|-----------------|---------------------|
| 1 | **AWS Access Key ID** | IAM Console → Users → your user → Security credentials → Create access key | `AWS_ACCESS_KEY_ID` |
| 2 | **AWS Secret Access Key** | (shown once when you create the access key — save it!) | `AWS_SECRET_ACCESS_KEY` |
| 3 | **AWS Region** | Pick the closest to your users: `ap-south-1` (Mumbai), `ap-southeast-1` (Singapore), `us-east-1` (Virginia) | `AWS_REGION` |
| 4 | **Domain DNS access** | You're using VPSCore — once Resend DNS records are added, also point `api.dreamkoreansmartclass.com` (A record or CNAME) to the AWS load balancer DNS name I'll give you | — |

The IAM user needs these permissions (attach this policy):
- `AmazonECS_FullAccess`
- `AmazonECR-FullAccess`
- `AmazonRDSFullAccess`
- `AmazonS3FullAccess`
- `AmazonVPCFullAccess`
- `IAMFullAccess` (so CI can create the ECS task role)
- `ElasticLoadBalancingFullAccess`

Or simpler: attach `AdministratorAccess` for the deployment user only (not recommended for long-term, but fine to bootstrap).

## Tier 2 — I can create these for you, just confirm

I'll create these resources in your AWS account via Terraform (or CloudFormation) once I have Tier 1:

- **VPC** with 2 public + 2 private subnets across 2 AZs
- **Aurora Postgres Serverless v2** database (`db.t4g.medium` minimum)
- **ECS Fargate cluster** running the Next.js backend (4 tasks minimum)
- **Elastic Load Balancer** (ALB) with HTTPS listener
- **ECR repository** for the Docker image
- **S3 bucket** for static assets + backups
- **CloudFront distribution** in front of the ALB (CDN + WAF)
- **Route 53 hosted zone** for `dreamkoreansmartclass.com` (optional — you can keep VPSCore DNS, just point A records to ALB)

## Tier 3 — For mobile app store releases (later)

When you're ready to publish to Play Store / App Store, you'll need:

### Android (Play Store)
- A Google Play Console account ($25 one-time): https://play.google.com/console
- A service account JSON from Google Cloud Console with Play Console permissions
- A release keystore (I'll give you the command to generate it)

### iOS (App Store) — Apple Developer Account required
- Apple Developer Program enrollment ($99/year): https://developer.apple.com/programs/
- Distribution certificate + provisioning profile (I'll guide you through this)
- App Store Connect API key (.p8 file)

## What I'll do once you give me Tier 1

1. Generate a Terraform plan that creates all the AWS resources
2. Push it as a GitHub Actions workflow (`deploy-infrastructure.yml`)
3. Run it — your AWS account gets the full stack provisioned
4. Push the backend Docker image to ECR
5. Deploy the backend to ECS Fargate behind the ALB
6. Give you the ALB DNS name to point `api.dreamkoreansmartclass.com` to
7. Trigger an Android APK build that points to the real production API
8. Hand you a download link for the APK

## Quick sanity-check questions

1. **Region preference?** I'd suggest `ap-south-1` (Mumbai) for Nepal/Korea users — is that OK?
2. **Existing AWS resources?** Do you have anything already running in this AWS account I should NOT touch?
3. **Domain registrar?** You said VPSCore for DNS — but is `dreamkoreansmartclass.com` also registered at VPSCore, or just DNS-hosted there?
4. **Budget?** The default setup runs ~$290/month for 100k DAU. Want me to scale it down for testing first (~$60/month), then scale up?

Paste the Tier 1 values (or set them as GitHub Secrets) and answer the 4 questions — I'll do the rest.
