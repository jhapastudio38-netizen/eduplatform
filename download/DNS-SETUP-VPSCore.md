# DNS Setup — dreamkoreansmartclass.com on VPSCore

You need to add **3 DNS records** at your domain provider (VPSCore) to verify `dreamkoreansmartclass.com` with Resend. Once verified, OTP emails will be delivered from `noreply@dreamkoreansmartclass.com` instead of falling back to dev mode.

## Step 1 — Log in to VPSCore DNS panel

Log in to your VPSCore dashboard, find the domain `dreamkoreansmartclass.com`, and open its DNS management page.

## Step 2 — Add these 3 records

### Record 1 — DKIM (TXT)

| Field | Value |
|-------|-------|
| **Type** | `TXT` |
| **Name / Host** | `resend._domainkey` |
| **Value / Content** | `p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDkeZHoxsUXrFIAR6kcW6hwhPmcfIq5n0KpgBiQDeJ3Darq8+yghxjEw3JNr9lqvThZOqtxNvyB80a0TPZS/NgBcXMEY2s3+L7IXSIEf95rWnINyPczC2aOWpEb8q+Oq3r7HuJ1lEvB7951yM7h1GnLEoiYQh1eyjjENATYuMEm6QIDAQAB` |
| **TTL** | `Auto` (or `3600` if Auto is not available) |

> **Note:** If your DNS panel shows the full record name as `resend._domainkey.dreamkoreansmartclass.com`, that is correct — most panels append the domain automatically. Just enter `resend._domainkey` in the Name field.

### Record 2 — MX (for bounce feedback)

| Field | Value |
|-------|-------|
| **Type** | `MX` |
| **Name / Host** | `send` |
| **Mail Server / Value** | `feedback-smtp.us-east-1.amazonses.com` |
| **Priority** | `10` |
| **TTL** | `Auto` (or `3600`) |

> This creates the subdomain `send.dreamkoreansmartclass.com`. Resend uses it to receive bounce/complaint notifications from email providers.

### Record 3 — SPF (TXT)

| Field | Value |
|-------|-------|
| **Type** | `TXT` |
| **Name / Host** | `send` |
| **Value / Content** | `v=spf1 include:amazonses.com ~all` |
| **TTL** | `Auto` (or `3600`) |

> This authorizes Amazon SES (Resend's email backend) to send mail on behalf of `send.dreamkoreansmartclass.com`. Do NOT add this to the root domain — it must go on the `send` subdomain.

## Step 3 — Verify in Resend

After saving all 3 records in VPSCore:

1. Wait 5-30 minutes for DNS propagation (you can check at https://dnschecker.org — search for `resend._domainkey.dreamkoreansmartclass.com`).
2. Tell me "DNS added" — I'll trigger the Resend verification API.
3. Once Resend shows status `verified`, OTP emails will start flowing to real inboxes.

## Optional — Recommended additional records (not required by Resend)

To improve email deliverability and brand trust, you may also want to add these to the **root domain**:

### Root SPF (lets any service send from @dreamkoreansmartclass.com)
```
Type:  TXT
Name:  @  (or leave blank)
Value: v=spf1 include:amazonses.com ~all
```

### DMARC (tells receivers what to do with mail that fails SPF/DKIM)
```
Type:  TXT
Name:  _dmarc
Value: v=DMARC1; p=quarantine; rua=mailto:admin@dreamkoreansmartclass.com; ruf=none; adkim=r; aspf=r
```

---

## Summary — what to paste in VPSCore

```
Type  | Host/Name                  | Value                                                                      | Priority | TTL
------|----------------------------|----------------------------------------------------------------------------|----------|------
TXT   | resend._domainkey          | p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDkeZHoxsUXrFIAR6kcW6hwhPmcfIq5n... | -        | Auto
MX    | send                       | feedback-smtp.us-east-1.amazonses.com                                       | 10       | Auto
TXT   | send                       | v=spf1 include:amazonses.com ~all                                          | -        | Auto
```

(The full DKIM value is in Record 1 above — copy it carefully, it's one long line.)

---

## What happens after verification

- Resend status will flip from `not_started` → `verified`
- OTP emails will be delivered from `noreply@dreamkoreansmartclass.com` to ANY recipient
- The current dev-mode fallback (codes printed to server log + shown as toast) will stop
- You can also send broadcast emails, notifications, etc. from this domain

Email me back here once you've added the records, and I'll verify them on the Resend side.
