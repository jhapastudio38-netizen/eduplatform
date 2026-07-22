# Status Update — Major Milestones Reached

## ✅ Web App is LIVE on AWS

**The Next.js teacher/admin web app is fully deployed and working on AWS.**

### Working URLs (HTTP for now, HTTPS after nameserver switch)
- **Web app (teacher/admin)**: http://eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com/
- **API health check**: http://eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com/api/health
- **OTP request**: `POST /api/auth/request-otp` with `{"contact":"admin@dreamkoreansmartclass.com"}` — returns `{"ok":true,"channel":"dev","devCode":"544411"}`

### What's deployed
- **ECS Fargate cluster**: `eduplatform-prod` (2 tasks running, healthy)
- **Application Load Balancer**: `eduplatform-alb` (HTTP:80 → port 3000)
- **RDS Postgres 18**: `eduplatform-db.clkk64yekaq9.ap-south-1.rds.amazonaws.com:5432`
  - Database: `eduplatform`
  - Schema pushed successfully (all Prisma tables created)
- **ECR repo**: `755395261031.dkr.ecr.ap-south-1.amazonaws.com/eduplatform-web`
- **Route 53 hosted zone**: `Z02571043AVFFU64NB9CV` (created for dreamkoreansmartclass.com)
- **DNS records created** (in Route 53, awaiting nameserver switch):
  - `dreamkoreansmartclass.com` → ALB
  - `api.dreamkoreansmartclass.com` → ALB
- **SSM Parameter Store**: stores RESEND_API_KEY and GROQ_API_KEY securely

### GitHub Actions CI/CD
- **Deploy Web to AWS** workflow runs on every push to main
- Builds Docker image, pushes to ECR, pushes Prisma schema to RDS, deploys to ECS
- Run #7 succeeded ✅

## ⏳ Android APK Build

**Build #26 is in progress with all Rust compile fixes applied.**

Latest fixes:
1. ✅ Slint syntax (LinearGradient, multi-line ||, callback bindings, character-count)
2. ✅ Rust: Arc-wrapped tokio Handle for cloning into closures
3. ✅ Rust: Arc-wrapped RwLock for ApiClient token
4. ✅ Rust: removed unused generic on send_empty()

Build #26 is at "Build Rust .so" step — first compile takes 10-15 min for Slint + all deps.

Live URL: https://github.com/jhapastudio38-netizen/eduplatform/actions/runs/29857878103

## 📋 What You Need to Do (PRIORITY ORDER)

### 1. Switch nameservers at VPSCore (5 min)
Open: `/home/z/my-project/download/NAMESERVER-UPDATE.md`

Set these 4 nameservers at VPSCore for `dreamkoreansmartclass.com`:
```
ns-832.awsdns-40.net
ns-267.awsdns-33.com
ns-1184.awsdns-20.org
ns-1728.awsdns-24.co.uk
```

After switching, tell me "nameservers switched" and I'll:
- Create the Resend DNS records in Route 53 (DKIM/MX/SPF)
- Request ACM TLS certificate for HTTPS
- Attach cert to ALB
- Web app becomes available at https://dreamkoreansmartclass.com

### 2. Wait for Android APK build to finish
The APK will be uploaded as a GitHub artifact at:
https://github.com/jhapastudio38-netizen/eduplatform/actions

Once it's done, you can download the APK directly from the workflow run page (scroll down to "Artifacts" section, click `eduplatform-student-debug-apk`).

### 3. (Later) iOS IPA
Once APK is working, we'll do iOS. Requires Apple Developer Account ($99/year).

## 🎯 Summary

| Component | Status |
|-----------|--------|
| Web app (teacher/admin) | ✅ Live on AWS |
| API backend | ✅ Live, OTP working |
| Postgres DB | ✅ Created, schema pushed |
| Route 53 hosted zone | ✅ Created |
| DNS records (api + root) | ✅ Created in Route 53 (pending nameserver switch) |
| ECS Fargate cluster | ✅ 2 tasks running, healthy |
| ALB | ✅ Active, HTTP listener on 80 |
| GitHub Actions CI/CD | ✅ Deploy Web succeeds, APK in progress |
| Android APK | ⏳ Building (Rust compile step) |
| iOS IPA | ⏸️ Postponed (per your request) |
| Resend domain | ⏳ Pending VPSCore DNS (you added records, Resend shows pending) |
| HTTPS certs | ⏸️ Waiting for nameserver switch |

## 🔐 AWS Resources Created (in your account)

| Resource | ID/Name |
|----------|---------|
| AWS Account | 755395261031 |
| IAM User | eduplatform-deploy |
| Region | ap-south-1 (Mumbai) |
| VPC | vpc-0581d8ea764296948 (default) |
| ECS Cluster | eduplatform-prod |
| ECS Service | eduplatform-web (2 tasks, Fargate) |
| Task Definition | eduplatform-web:5 (latest) |
| ALB | eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com |
| Target Group | eduplatform-tg (port 3000, health /api/health) |
| RDS Instance | eduplatform-db (Postgres 18, db.t4g.micro, 20GB) |
| RDS Endpoint | eduplatform-db.clkk64yekaq9.ap-south-1.rds.amazonaws.com:5432 |
| ECR Repo | 755395261031.dkr.ecr.ap-south-1.amazonaws.com/eduplatform-web |
| S3 Bucket | eduplatform-backups-755395261031 |
| Route 53 Zone | Z02571043AVFFU64NB9CV |
| SSM Parameters | /eduplatform/resend-api-key, /eduplatform/groq-api-key |
| Security Groups | eduplatform-alb-sg, eduplatform-ecs-sg, eduplatform-db-sg |

## 💰 Monthly Cost Estimate

| Service | Cost |
|---------|------|
| ECS Fargate (2 tasks × 0.5 vCPU × 1GB) | ~$60 |
| RDS Postgres (db.t4g.micro, 20GB) | ~$14 |
| ALB | ~$16 |
| ECR (1 image) | ~$1 |
| Route 53 | ~$1 |
| S3 | ~$0.50 |
| CloudWatch logs | ~$3 |
| **Total** | **~$95/month** |

(For 100k users, scale ECS to 4-8 tasks → ~$200/month)

## 📞 What's Next

Once you switch nameservers and the APK build finishes:
1. Web app at https://dreamkoreansmartclass.com (with HTTPS)
2. API at https://api.dreamkoreansmartclass.com
3. APK download link from GitHub Actions
4. OTP emails delivered via Resend from noreply@dreamkoreansmartclass.com

Tell me when you've switched the nameservers and I'll handle the rest.
