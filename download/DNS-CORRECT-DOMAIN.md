# DNS Records — dreamkoreasmartclass.com (CORRECT DOMAIN)

## Add these records at VPSCore for dreamkoreasmartclass.com

### Option A: Switch nameservers to AWS (recommended — enables HTTPS automation)

At VPSCore, find the "Nameservers" setting for `dreamkoreasmartclass.com` and replace with these 4:

```
ns-1124.awsdns-12.org
ns-1685.awsdns-18.co.uk
ns-653.awsdns-17.net
ns-463.awsdns-57.com
```

Then I'll add ALL DNS records (Resend + web app) on AWS Route 53 automatically. You don't need to do anything else.

---

### Option B: Keep DNS at VPSCore — add these 5 records manually

If you want to keep DNS at VPSCore, add these 5 records:

#### Record 1 — DKIM (TXT) — for Resend email verification

| Field | Value |
|-------|-------|
| Type | `TXT` |
| Name / Host | `resend._domainkey` |
| Value | `p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCj2gT2b37zr/5GxQ+Ymzp0StblJCVHMbUEVEAR1nEm0aWdMp1h0O0UBKt06bwvTJvI+mVdcfEirpYMEx5jWiLgULnv6/LcgIDZEd0GF04JhJfTs6/BJJYd8opLK26IVvK15j1+tJCqxAYvt4kOb3pycnBbWW+z++yhk7tWPr1rkwIDAQAB` |
| TTL | Auto |

#### Record 2 — MX (for bounce feedback)

| Field | Value |
|-------|-------|
| Type | `MX` |
| Name / Host | `send` |
| Mail Server | `feedback-smtp.us-east-1.amazonses.com` |
| Priority | `10` |
| TTL | Auto |

#### Record 3 — SPF (TXT)

| Field | Value |
|-------|-------|
| Type | `TXT` |
| Name / Host | `send` |
| Value | `v=spf1 include:amazonses.com ~all` |
| TTL | Auto |

#### Record 4 — A record for api subdomain

| Field | Value |
|-------|-------|
| Type | `A` |
| Name / Host | `api` |
| Value | `13.127.43.189` |
| TTL | 300 |

#### Record 5 — A record for root domain

| Field | Value |
|-------|-------|
| Type | `A` |
| Name / Host | `@` (or leave blank) |
| Value | `13.127.43.189` |
| TTL | 300 |

---

## Quick summary table

```
Type  | Host/Name              | Value                                                                      | Priority | TTL
------|------------------------|----------------------------------------------------------------------------|----------|------
TXT   | resend._domainkey      | p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCj2gT2b37zr/5GxQ+Ymzp0StblJ...     | -        | Auto
MX    | send                   | feedback-smtp.us-east-1.amazonses.com                                       | 10       | Auto
TXT   | send                   | v=spf1 include:amazonses.com ~all                                          | -        | Auto
A     | api                    | 13.127.43.189                                                              | -        | 300
A     | @                      | 13.127.43.189                                                              | -        | 300
```

---

## After adding records

Tell me "DNS added" — I will:
1. Verify Resend domain status flips to `verified`
2. Verify `api.dreamkoreasmartclass.com` resolves to the AWS load balancer
3. Request ACM TLS certificate for HTTPS
4. Attach cert to load balancer
5. Web app accessible at **https://dreamkoreasmartclass.com**
6. API accessible at **https://api.dreamkoreasmartclass.com**
7. OTP emails delivered from **noreply@dreamkoreasmartclass.com**

---

## If you chose Option A (switch nameservers)

After switching nameservers, tell me "nameservers switched" and I'll:
- Add the 3 Resend DNS records to Route 53 (DKIM, MX, SPF)
- Add A records for `api` and root domain pointing to the ALB
- Everything else is already configured in Route 53
