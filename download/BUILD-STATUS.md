# Build Status — what's been fixed, what's pending

## GitHub repo
**https://github.com/jhapastudio38-netizen/eduplatform**

All code is pushed. Workflows trigger automatically on push to `main`.

## Workflow runs (latest)

| Run | Workflow | Status | Notes |
|-----|----------|--------|-------|
| #8 | Build Android APK | **In progress** — Rust compile step | Building .so for aarch64 Android |
| #7 | Build iOS | **In progress** — Rust compile step | Building .a for aarch64-apple-ios |

Live URLs:
- Android: https://github.com/jhapastudio38-netizen/eduplatform/actions/workflows/build-android-and-ios.yml
- iOS:     https://github.com/jhapastudio38-netizen/eduplatform/actions/workflows/build-ios.yml

## What I fixed

1. **Android SDK install** — switched from broken `packages: |` YAML multiline to direct `sdkmanager` CLI calls
2. **cargo-mobile2 repo URL** — the repo moved from `BrainiumLLC/` to `tauri-apps/` (the old URL returns 404)
3. **Git credential helper on macOS runners** — bypassed with `GIT_TERMINAL_PROMPT=0` + `GIT_ASKPASS=/bin/true`
4. **cargo-mobile2 CLI incompatibility** — tauri-apps fork removed `--platform` from `init` and is now Tauri-only. Pivoted to building the Rust static library directly with `cargo build --target` + the NDK linker
5. **Resend domain added** — `dreamkoreansmartclass.com` registered with Resend, DNS records generated (see `DNS-SETUP-VPSCore.md`)

## GitHub Secrets configured

| Secret | Value | Set? |
|--------|-------|------|
| `GROQ_API_KEY` | gsk_WIQ9Y4... | ✅ |
| `RESEND_API_KEY` | re_DQ5HeC... | ✅ |
| `EDUPLATFORM_API_URL` | https://api.dreamkoreansmartclass.com | ✅ |
| `RESEND_FROM` | EduPlatform <noreply@dreamkoreansmartclass.com> | ✅ |

Still need: `AWS_*`, `APPLE_*`, `PLAY_STORE_SERVICE_ACCOUNT_JSON`, `EDUPLATFORM_KEYSTORE_BASE64`

## What you'll get from CI

### From Android build (#8)
- Artifact: `eduplatform-student-android-so` (the compiled Rust .so for aarch64)
- The .so can be wrapped into an APK with a minimal Gradle project (next commit)

### From iOS build (#7)
- Artifact: `eduplatform-student-ios-a` (compiled Rust .a for aarch64-apple-ios)
- Artifact: `eduplatform-student-ios-sim-a` (for iOS Simulator)
- These can be wrapped into an .ipa with an Xcode project + Apple Developer cert

## Honest limitations

1. **APK** — The .so alone isn't installable. You need a wrapping Android project (AndroidManifest.xml + MainActivity + resources). I'll add a template `android/` folder in the next commit. Once that's in, CI will produce a real `.apk` you can sideload.

2. **IPA** — Cannot be produced at all without:
   - Apple Developer Account ($99/year)
   - Distribution certificate + provisioning profile
   - macOS runner (we have this)
   
   Once you enroll in Apple Developer Program and provide the secrets, the workflow is ready — just needs to be uncommented.

3. **Slint compile** — This is the first time the Slint UI files are being compiled. If there are syntax errors in `.slint` files, this run will surface them and I'll fix iteratively.

## What I need from you

### For Resend email delivery (urgent — affects OTP)
Add 3 DNS records at VPSCore (see `DNS-SETUP-VPSCore.md` for exact values):
1. TXT record: `resend._domainkey` → DKIM public key
2. MX record: `send` → `feedback-smtp.us-east-1.amazonses.com` (priority 10)
3. TXT record: `send` → `v=spf1 include:amazonses.com ~all`

### For AWS deployment
See `AWS-REQUIREMENTS.md` — need:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION` (suggest `ap-south-1` Mumbai)
- Confirmation on the 4 sanity-check questions in that doc

### For Play Store release (later)
- Google Play Console account ($25)
- Service account JSON
- Release keystore (I'll give you the command)

### For App Store release (later)
- Apple Developer Program enrollment ($99/year)
- Distribution cert + provisioning profile
- App Store Connect API key

## Recommended next actions (priority order)

1. **Add the 3 Resend DNS records at VPSCore** → tell me when done, I'll verify
2. **Provide AWS credentials** → I'll deploy the backend
3. **Wait for build #8/#7 to finish** → I'll fix any Slint errors that surface
4. **Enroll in Apple Developer Program** (if you want IPA)
5. **Generate a release keystore** (if you want signed APK for Play Store)
