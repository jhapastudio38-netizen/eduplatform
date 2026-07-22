# EduPlatform — Build & Deploy Guide

This guide covers:
1. Running the app locally
2. Building the production web bundle / PWA
3. Wrapping the PWA into native Android (.apk / .aab) and iOS (.ipa) binaries
4. Publishing to Google Play and Apple App Store
5. Deploying the backend to AWS for 100k concurrent users

---

## 1. Local development

```bash
bun install
cp .env.example .env      # fill in RESEND_API_KEY, GROQ_API_KEY
bun run db:push
bun run scripts/seed.ts   # creates demo admin/teacher/student accounts
bun run dev               # http://localhost:3000
```

### Demo accounts (after seeding)
- Admin:   `admin@eduplatform.app`
- Teacher: `teacher@eduplatform.app`
- Student: `student@eduplatform.app`

In dev mode without `RESEND_API_KEY`, OTP codes are printed to the server log and surfaced as a toast.

---

## 2. Web / PWA build

```bash
./scripts/build-native.sh pwa
# Output: ./out/  (static export, deployable to S3 + CloudFront)
```

The PWA is installable from any modern browser. It satisfies Google Play's PWA-to-TWA requirements.

---

## 3. Android APK / AAB

### One-time setup
1. Install [Android Studio](https://developer.android.com/studio) (includes JDK 17 + Android SDK).
2. Accept SDK licenses: `yes | sdkmanager --licenses`
3. Generate a release keystore:
   ```bash
   keytool -genkeypair -v -keystore release.keystore -alias eduplatform \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
4. Fill the keystore path / passwords in `capacitor.config.json` → `android.buildOptions`.

### Build
```bash
./scripts/build-native.sh android-release
```

Outputs:
- `android/app/build/outputs/apk/release/app-release.apk` — for internal testing
- `android/app/build/outputs/bundle/release/app-release.aab` — for Play Store upload

---

## 4. iOS IPA

### One-time setup (macOS only)
1. Install Xcode 15+ from the Mac App Store.
2. Install CocoaPods: `sudo gem install cocoapods`
3. Enroll in the [Apple Developer Program](https://developer.apple.com/programs/) ($99/year).
4. In Apple Developer portal, create:
   - App ID: `app.eduplatform.mobile`
   - App Store signing certificate
   - Provisioning profile (App Store distribution)
5. Open `ios/App/App.xcworkspace` in Xcode → set signing team & bundle ID.

### Build
```bash
./scripts/build-native.sh ios-release
```

Output: `ios/App/build/App.ipa` — upload via Xcode Organizer or `xcrun altool`.

---

## 5. Google Play Store submission

1. Create a [Play Console](https://play.google.com/console) account ($25 one-time).
2. Create a new application → fill store listing:
   - **App name:** EduPlatform — Learn, Teach, Manage
   - **Package name:** `app.eduplatform.mobile`
   - **Category:** Education
   - **Content rating:** Everyone (submit the IARC questionnaire)
3. Upload the `.aab` to **Production** (or **Internal testing** first).
4. Required assets:
   - App icon: 512×512 PNG
   - Feature graphic: 1024×500 PNG
   - Phone screenshots: min 2, 1080×1920 PNG
   - Privacy Policy URL (host the file in `public/privacy-policy.md`)
5. Complete **Data safety** form (we collect email/phone + usage data; no financial, health, or location data).
6. Complete **Government apps / declare ads / target audience** sections.
7. Submit for review (typically 1–3 days).

### Play Store compliance checklist
- ✅ `androidScheme: "https"` in capacitor config
- ✅ Privacy policy URL accessible (deploy `public/privacy-policy.md`)
- ✅ Target SDK 34+ (Android 14)
- ✅ 64-bit binaries (default with Capacitor 5+)
- ✅ App uses HTTPS only
- ✅ No background location tracking
- ✅ No request for unnecessary permissions
- ✅ Data safety form matches actual data collection
- ✅ Export compliance (encryption is exempt for standard HTTPS)

---

## 6. Apple App Store submission

1. In [App Store Connect](https://appstoreconnect.apple.com), create a new app:
   - **Bundle ID:** `app.eduplatform.mobile`
   - **SKU:** eduplatform
   - **Primary language:** English
   - **Category:** Education
2. Upload the `.ipa` via Xcode → Organizer → Distribute App → App Store Connect.
3. In App Store Connect, complete:
   - App information, description, keywords, support URL
   - Privacy Policy URL
   - Screenshots (6.7" iPhone, 12.9" iPad required)
   - App Review Information (demo account credentials: `student@eduplatform.app`)
4. Complete **App Privacy** questionnaire (same data as Play Store data safety).
5. Submit for review (typically 24–48 hours).

### App Store compliance checklist
- ✅ iOS deployment target 14.0+
- ✅ App Tracking Transparency: not required (we don't track)
- ✅ Privacy manifest (`PrivacyInfo.xcprivacy`) — see Apple docs
- ✅ No use of deprecated APIs
- ✅ Sign in with Apple NOT required (we use email/phone OTP, not social login)
- ✅ Account deletion option available in Profile
- ✅ No hidden features
- ✅ Reasonable data minimization

---

## 7. AWS deployment (100k concurrent users)

### Architecture
```
                        ┌─────────────────┐
                        │   Route 53      │  DNS
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │   CloudFront    │  CDN + WAF + TLS
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
       ┌──────▼─────┐    ┌───────▼──────┐   ┌──────▼──────┐
       │  S3 (PWA)  │    │  ALB         │   │  API GW     │
       │  static    │    │  → Fargate   │   │  → Lambda   │
       └────────────┘    │  (Next.js)   │   │  (mini svc) │
                         └───────┬──────┘   └─────────────┘
                                 │
                  ┌──────────────┼──────────────┐
                  │              │              │
            ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐
            │  Aurora   │  │  ElastiCache │  │  SNS     │
            │  Postgres │  │  Redis       │  │  (OTP)   │
            └───────────┘  └─────────────┘  └──────────┘
```

### Step-by-step

1. **Database** — Provision Aurora Postgres Serverless v2.
   - Update `prisma/schema.prisma` `datasource.db` provider to `postgresql`.
   - Update `DATABASE_URL` to the Aurora connection string.
   - Run `bun run db:push`.

2. **Cache / rate limiter** — Provision ElastiCache Redis.
   - Replace `src/lib/rate-limit.ts` with Redis-backed implementation (`ioredis`).
   - Use Redis for session storage too (replace DB-backed sessions).

3. **Web app** — Containerize Next.js:
   ```dockerfile
   FROM oven/bun:1 AS build
   WORKDIR /app
   COPY package.json bun.lock ./
   RUN bun install --frozen-lockfile
   COPY . .
   RUN bun run build

   FROM oven/bun:1 AS run
   WORKDIR /app
   COPY --from=build /app/.next/standalone ./
   COPY --from=build /app/.next/static ./.next/static
   COPY --from=build /app/public ./public
   EXPOSE 3000
   CMD ["bun", "server.js"]
   ```

4. **Compute** — Push container to ECR, deploy on Fargate behind an ALB.
   - Min capacity: 4 tasks (across 2 AZs)
   - Auto-scale on CPU 70% and ALB request count
   - Target: 500 req/s per task → 4 tasks = 2000 req/s → comfortable for 100k DAU

5. **WebSocket live class** — Deploy the `mini-services/live-class-service` as a separate Fargate service on port 3003.
   - Add `@socket.io/redis-adapter` for multi-instance message fan-out.
   - Put behind the same ALB on a path-based rule.

6. **Edge / CDN** — CloudFront in front of everything:
   - Cache static assets (`/_next/static/*`) for 1 year
   - Cache public API (`/api/student/subjects`, `/api/student/tests`) for 60s at edge
   - WAF: rate-limit 100 req/10s per IP, block known malicious IPs

7. **Email / SMS** —
   - Email OTP: Resend (already integrated)
   - SMS OTP: AWS SNS or Pinpoint (replace `sendOtpPhone` in `src/lib/otp.ts`)

8. **AI** — Store `GROQ_API_KEY` in AWS Secrets Manager, fetch at startup.

9. **Observability** —
   - CloudWatch Logs + Metrics
   - X-Ray for distributed tracing
   - Sentry for client-side error tracking

10. **Backups** — Aurora automated backups + daily snapshot to S3 cross-region.

### Cost estimate (100k DAU)
- Aurora Postgres Serverless v2 (min 0.5 ACU): ~$60/mo
- Fargate (4 tasks, 1 vCPU / 2 GB each): ~$120/mo
- CloudFront (1 TB egress): ~$85/mo
- ElastiCache Redis (cache.t3.micro): ~$12/mo
- S3 + SNS + Route 53: ~$15/mo
- **Total: ~$290/mo** at this scale, scaling linearly beyond.

---

## 8. CI/CD (recommended)

GitHub Actions workflow:
1. On push to `main`: lint, test, build, push to ECR.
2. On tag `v*`: deploy to staging Fargate, run security audit, promote to production.
3. On release: trigger native builds on macOS runner (for iOS), upload .aab / .ipa to Play Console / App Store Connect via fastlane.

---

## 9. Scripts reference

| Script | Purpose |
|--------|---------|
| `bun run dev` | Start dev server |
| `bun run lint` | ESLint check |
| `bun run db:push` | Push Prisma schema to DB |
| `bun run scripts/seed.ts` | Seed demo data |
| `bun run scripts/security-audit.ts` | Run security vulnerability tests |
| `bun run scripts/load-test.ts` | Run load test |
| `bun run scripts/gen-icons.ts` | Regenerate PWA icons |
| `./scripts/build-native.sh pwa` | Build static PWA |
| `./scripts/build-native.sh android-release` | Build .apk / .aab |
| `./scripts/build-native.sh ios-release` | Build .ipa (macOS only) |
