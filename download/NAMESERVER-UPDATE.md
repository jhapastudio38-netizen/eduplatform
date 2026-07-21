# Domain Nameserver Update — Required at VPSCore

## What I did on AWS
I created a Route 53 hosted zone for `dreamkoreansmartclass.com`. AWS gave me 4 nameservers that I need you to set at VPSCore (your domain registrar / DNS provider).

## The 4 AWS nameservers you need to set

Log in to VPSCore, find `dreamkoreansmartclass.com`, and look for "Nameservers" or "DNS Nameservers" settings. Replace whatever is there with these 4:

```
ns-832.awsdns-40.net
ns-267.awsdns-33.com
ns-1184.awsdns-20.org
ns-1728.awsdns-24.co.uk
```

## Important notes

1. **Replace ALL existing nameservers** — don't add these alongside the VPSCore ones. Set them as the only 4.

2. **Wait for propagation** — after saving, DNS propagation takes 15 minutes to 24 hours (usually under 1 hour). You can check at https://dnschecker.org — search for `dreamkoreansmartclass.com` and look at the NS records.

3. **VPSCore DNS records will stop working** — once nameservers switch to AWS, any DNS records you had at VPSCore (like the Resend DKIM/SPF records) need to be re-created in AWS Route 53. **I will do this for you** — just tell me when nameservers are switched.

4. **The Resend DNS records you added at VPSCore** — these will need to be re-added in Route 53 once nameservers switch. I have them saved and will create them in AWS automatically.

## After you switch the nameservers

Send me a message saying "nameservers switched" — I will:
1. Verify the switch took effect (via `dig NS dreamkoreansmartclass.com`)
2. Create the Resend DNS records in Route 53 (DKIM, MX, SPF for the `send` subdomain)
3. Create the `api.dreamkoreansmartclass.com` A record pointing to the AWS load balancer
4. Create the `dreamkoreansmartclass.com` (root) A record pointing to the load balancer
5. Request an ACM TLS certificate for HTTPS
6. Attach the certificate to the load balancer

## What you'll be able to access

Once everything is set up (usually within 1-2 hours of you switching nameservers):
- **https://dreamkoreansmartclass.com** — teacher/admin web app
- **https://api.dreamkoreansmartclass.com** — backend API (used by the student mobile app)
- OTP emails will arrive in real inboxes from `noreply@dreamkoreansmartclass.com`

## Why we're switching nameservers to AWS

- **HTTPS certificates**: AWS Certificate Manager (ACM) provides free TLS certs but they require DNS validation. With Route 53, validation is automatic.
- **DNS-based load balancing**: Route 53 alias records point directly to the ALB with health checks.
- **Single source of truth**: All DNS, certs, and infrastructure live in AWS — easier to manage.
- **Cost**: Route 53 is $0.50/month per hosted zone + $0.40 per million queries. Negligible.

## If you don't want to switch nameservers

You can keep DNS at VPSCore and just add A records pointing to the AWS load balancer:
- `api.dreamkoreansmartclass.com` → A record → `eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com`
- `dreamkoreansmartclass.com` → A record → `eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com`

But you'd lose:
- Free automatic HTTPS (you'd need to get certs from Let's Encrypt or buy from a CA)
- Automatic DNS validation for ACM certs
- Health-checked routing

**Recommendation:** Switch nameservers. It's a 5-minute change at VPSCore and saves a lot of hassle.

## Quick summary — what to do right now

1. Log in to VPSCore
2. Find domain `dreamkoreansmartclass.com`
3. Find "Nameservers" setting
4. Replace existing nameservers with:
   ```
   ns-832.awsdns-40.net
   ns-267.awsdns-33.com
   ns-1184.awsdns-20.org
   ns-1728.awsdns-24.co.uk
   ```
5. Save
6. Tell me "nameservers switched"
