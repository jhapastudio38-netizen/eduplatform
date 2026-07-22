# DNS Records to Add at VPSCore — Updated July 21, 2026

You said you updated the nameservers, but they're still showing `ns1.mysecurecloudhost.com` (VPSCore's). That's actually fine — we can keep DNS at VPSCore and just add these records there.

## Add these 5 records at VPSCore DNS panel for `dreamkoreansmartclass.com`:

### Record 1 — DKIM (for Resend email verification)

| Field | Value |
|-------|-------|
| Type | `TXT` |
| Name / Host | `resend._domainkey` |
| Value | `p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDkeZHoxsUXrFIAR6kcW6hwhPmcfIq5n0KpgBiQDeJ3Darq8+yghxjEw3JNr9lqvThZOqtxNvyB80a0TPZS/NgBcXMEY2s3+L7IXSIEf95rWnINyPczC2aOWpEb8q+Oq3r7HuJ1lEvB7951yM7h1GnLEoiYQh1eyjjENATYuMEm6QIDAQAB` |
| TTL | Auto (or 3600) |

### Record 2 — MX (for bounce feedback)

| Field | Value |
|-------|-------|
| Type | `MX` |
| Name / Host | `send` |
| Mail Server | `feedback-smtp.us-east-1.amazonses.com` |
| Priority | `10` |
| TTL | Auto |

### Record 3 — SPF (TXT)

| Field | Value |
|-------|-------|
| Type | `TXT` |
| Name / Host | `send` |
| Value | `v=spf1 include:amazonses.com ~all` |
| TTL | Auto |

### Record 4 — A record for `api` subdomain → AWS Load Balancer

| Field | Value |
|-------|-------|
| Type | `A` |
| Name / Host | `api` |
| Value | `13.127.43.189` (ALB IP — or use CNAME to `eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com`) |
| TTL | Auto (or 300) |

**Better option for record 4**: Use a CNAME instead of A record:
| Field | Value |
|-------|-------|
| Type | `CNAME` |
| Name / Host | `api` |
| Value | `eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com` |
| TTL | Auto |

### Record 5 — A record for root domain → AWS Load Balancer

| Field | Value |
|-------|-------|
| Type | `A` |
| Name / Host | `@` (or leave blank for root) |
| Value | `13.127.43.189` |
| TTL | Auto (or 300) |

**Or use CNAME** (some registrars don't allow CNAME at root — use ALIAS or A record if so):
| Field | Value |
|-------|-------|
| Type | `CNAME` (or `ALIAS`) |
| Name / Host | `@` |
| Value | `eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com` |

## Quick summary table

```
Type   | Host/Name              | Value                                                                      | Priority | TTL
-------|------------------------|----------------------------------------------------------------------------|----------|------
TXT    | resend._domainkey      | p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDkeZHoxsUXrFIAR6kcW6hwhPmcfIq5n... | -        | Auto
MX     | send                   | feedback-smtp.us-east-1.amazonses.com                                       | 10       | Auto
TXT    | send                   | v=spf1 include:amazonses.com ~all                                          | -        | Auto
CNAME  | api                    | eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com                     | -        | Auto
CNAME  | @                      | eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com                     | -        | Auto
```

## After you add these records

1. Wait 5-30 min for DNS propagation
2. Tell me "DNS added" — I'll verify everything is working
3. Once verified:
   - OTP emails will arrive in real inboxes from `noreply@dreamkoreansmartclass.com`
   - Web app accessible at `https://dreamkoreansmartclass.com` (after I add HTTPS cert)
   - API accessible at `https://api.dreamkoreansmartclass.com`

## Note about nameservers

Your nameservers are still showing `ns1.mysecurecloudhost.com` (VPSCore's). This is fine — we'll keep DNS at VPSCore. Just add the 5 records above and everything will work.

If you want to switch nameservers to AWS Route 53 instead (better for HTTPS cert automation), let me know and I'll give you those instructions. But it's not required.
