# DreamKorea SmartClass

> Korean language learning platform — admin panel, student Android app, and public website.

**Live URLs:**
- 🌐 **Public Website:** https://my-project-five-sepia.vercel.app/
- 🔧 **Admin Panel:** https://my-project-five-sepia.vercel.app/admin
- 📱 **Android App:** See `download/DreamKorea-SmartClass-v1.5.0.apk`

**Admin Login:** `admin` / `DreamKorea@2026`

---

## 📋 Table of Contents

1. [Architecture](#architecture)
2. [What's Done](#whats-done)
3. [What's Planned](#whats-planned)
4. [Tech Stack](#tech-stack)
5. [Project Structure](#project-structure)
6. [Environment Setup](#environment-setup)
7. [Local Development](#local-development)
8. [Deployment](#deployment)
9. [Android App Build](#android-app-build)
10. [API Reference](#api-reference)
11. [Credentials](#credentials)

---

## 🏗 Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Admin Panel    │     │   Student App    │     │  Public Website │
│  (Next.js web)   │     │  (Android/Kotlin)│     │  (Next.js web)  │
└────────┬────────┘     └────────┬─────────┘     └────────┬────────┘
         │                       │                         │
         └───────────┬───────────┴─────────────────────────┘
                     │
          ┌──────────▼──────────┐
          │   Vercel (Next.js)   │
          │   API Routes (TS)    │
          └──────────┬──────────┘
                     │
          ┌──────────┼──────────┐
          │          │          │
   ┌──────▼──┐ ┌────▼────┐ ┌──▼──────┐
   │Supabase │ │  R2     │ │  Groq   │
   │Postgres │ │Storage  │ │   AI    │
   └─────────┘ └─────────┘ └─────────┘
```

- **Frontend:** Next.js 16 (App Router) + React 19 + Tailwind CSS + shadcn/ui
- **Backend:** Next.js API Routes (Serverless, deployed on Vercel)
- **Database:** Supabase (PostgreSQL) via Prisma ORM
- **File Storage:** Cloudflare R2 (S3-compatible) — images, audio, PDFs, videos
- **AI:** Groq (llama-3.3-70b) for question generation + translation
- **Email:** Resend for OTP delivery
- **Mobile:** Native Android app (Kotlin + Jetpack Compose)
- **Hosting:** Vercel (web) + APK distribution (manual)

---

## ✅ What's Done

### Admin Panel (web)
- [x] **Login** — cookie-based admin auth (`admin` / `DreamKorea@2026`)
- [x] **Dashboard** — overview with stats tiles
- [x] **Exams & Tests** — multi-step exam creator (Step 1: details → Step 2: unlimited questions)
- [x] **Question Bank** — standalone practice questions (MCQ, image, audio+loop, fill-blank, true/false)
- [x] **AI Question Generation** — Groq-powered, generates questions from topic/difficulty
- [x] **File Upload** — upload images/audio/videos/PDFs from device to R2 (not just URLs)
- [x] **Books** — PDF + cover image management with file upload
- [x] **Home Cards** — student home screen cards with image upload
- [x] **Audio Lessons** — audio file upload + transcript/translation
- [x] **Video Lessons** — YouTube URL OR video file upload from device
- [x] **User Management** — create teachers, view students, ban users
- [x] **Push Notifications** — send notifications to all student devices
- [x] **Student Results** — view exam submissions and scores
- [x] **English→Korean Translation** — AI-powered translator tool

### Student App (Android)
- [x] **OTP Login** — email + OTP (no password, 2-step flow)
- [x] **Home Screen** — image cards grouped by section (Tests, Resources, Premium)
- [x] **Question Bank** — browse by category, practice one-at-a-time with instant feedback
- [x] **Tests & Exams** — filtered lists (UBT, Free Practice, Batch, All)
- [x] **Exam Taking** — timer, progress bar, audio player with loop, image questions
- [x] **Exam Results** — score, pass/fail, answer review with explanations
- [x] **Books** — browse + open PDF reader
- [x] **Videos** — YouTube embeds + uploaded video playback
- [x] **Audio Lessons** — list + play
- [x] **Profile** — stats (exams taken, avg score, streak, etc.)
- [x] **Settings** — theme color, dark mode, text size, animations toggle
- [x] **Push Notifications** — receives admin broadcasts (60s polling)
- [x] **Stale-While-Revalidate Caching** — instant screen loads (like Twitter/Instagram)
- [x] **Coil Image Loading** — admin-uploaded images display properly
- [x] **DreamKorea Logo Icons** — custom launcher icon at all densities
- [x] **Landscape Support** — question/answer side-by-side in landscape mode
- [x] **Error Handling** — retry buttons, timeout, empty states, actual API errors shown
- [x] **Crash Protection** — global uncaught exception handler
- [x] **Smooth Animations** — fade transitions, bouncy cards, staggered list items

### Public Website
- [x] Landing page with DreamKorea branding
- [x] SEO-optimized (meta tags, OpenGraph, Twitter cards)
- [x] Mobile-responsive

### Backend / API
- [x] **Auth:** OTP request + verify, session cookies, admin credentials
- [x] **Student API:** tests, question-bank, books, videos, audio, stats, home-cards, notifications
- [x] **Admin API:** CRUD for all content, file upload, notifications broadcast, AI generate
- [x] **File Serving:** `/api/files/[...path]` serves R2 files with caching
- [x] **Database Schema:** 25+ models (User, Test, Question, Book, VideoLesson, AudioLesson, Notification, etc.)
- [x] **Rate Limiting:** on OTP, login, password set
- [x] **Audit Logging:** all admin actions logged

---

## 🚧 What's Planned

### High Priority
- [ ] **Push notification tap-to-navigate** — tapping a notification opens the relevant screen
- [ ] **Exam scheduling** — admin sets start/end time, students see countdown
- [ ] **Batch exams** — assign exams to specific student batches
- [ ] **Leaderboard** — rank students by score/exams completed
- [ ] **Dark mode in admin panel** — currently light only
- [ ] **Image cropping** — crop/resize images before upload in admin

### Medium Priority
- [ ] **Live class audio/video** — WebRTC integration for live rooms
- [ ] **Chat/messaging** — student-teacher direct messages
- [ ] **Offline mode** — cache tests/questions for offline practice
- [ ] **Progress tracking** — per-lesson completion, per-chapter progress
- [ ] **Spaced repetition** — smart question review based on past performance
- [ ] **Achievements/badges** — gamification (streaks, perfect scores, etc.)
- [ ] **PDF annotation** — highlight/notes in book reader

### Low Priority
- [ ] **iOS app** — Swift/SwiftUI port
- [ ] **Multi-language** — Nepali, Korean UI translations
- [ ] **Payment integration** — premium content subscriptions
- [ ] **Analytics dashboard** — admin sees engagement metrics
- [ ] **Export results** — CSV/PDF export of student scores
- [ ] **Bulk question import** — CSV/Excel upload for questions

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Web Framework | Next.js 16 (App Router) |
| Language | TypeScript 5 |
| UI | React 19 + Tailwind CSS 4 + shadcn/ui |
| Database | PostgreSQL (Supabase) |
| ORM | Prisma 6 |
| File Storage | Cloudflare R2 (S3-compatible) |
| AI | Groq (llama-3.3-70b-versatile) |
| Email | Resend |
| Mobile | Kotlin + Jetpack Compose (Android) |
| Image Loading | Coil 2.6 (Android) |
| Networking | Retrofit 2 + OkHttp 4 (Android) |
| Hosting | Vercel |
| Version Control | Git + GitHub |

---

## 📁 Project Structure

```
dreamkorea-smartclass/
├── src/                              # Next.js web app
│   ├── app/                          # App Router pages
│   │   ├── admin/                    # Admin login page
│   │   ├── admin-panel/              # Admin SPA wrapper
│   │   ├── api/                      # API routes
│   │   │   ├── admin/                # Admin-only endpoints
│   │   │   │   ├── tests/            # Exam CRUD
│   │   │   │   ├── questions/        # Question CRUD
│   │   │   │   ├── books/            # Book CRUD
│   │   │   │   ├── home-cards/       # Home card CRUD
│   │   │   │   ├── audio-lessons/    # Audio CRUD
│   │   │   │   ├── video-lessons/    # Video CRUD
│   │   │   │   ├── notifications/    # Push notification broadcast
│   │   │   │   ├── file-upload/      # Universal file upload to R2
│   │   │   │   ├── ai/               # AI question generation
│   │   │   │   ├── users/            # User management
│   │   │   │   └── ...
│   │   │   ├── auth/                 # Auth endpoints (OTP, credentials)
│   │   │   ├── student/              # Student-facing endpoints
│   │   │   │   ├── tests/            # Test list + detail + submit
│   │   │   │   ├── question-bank/    # Question bank
│   │   │   │   ├── books/            # Book list
│   │   │   │   ├── notifications/    # Poll for notifications
│   │   │   │   └── ...
│   │   │   └── files/                # File serving from R2
│   │   └── page.tsx                  # Public website
│   ├── components/
│   │   ├── admin/                    # Admin panel components
│   │   │   ├── AdminApp.tsx          # Main admin shell + router
│   │   │   ├── AdminTests.tsx        # Exam management
│   │   │   ├── AdminQuestions.tsx    # Question bank management
│   │   │   ├── AdminBooks.tsx        # Book management
│   │   │   ├── AdminHomeCards.tsx    # Home card management
│   │   │   ├── AdminAudioLessons.tsx # Audio lesson management
│   │   │   ├── AdminVideoLessons.tsx # Video lesson management
│   │   │   ├── AdminNotifications.tsx# Push notification sender
│   │   │   ├── AdminUsers.tsx        # User management
│   │   │   └── ...
│   │   └── ui/                       # shadcn/ui components
│   └── lib/                          # Shared libraries
│       ├── db.ts                     # Prisma client
│       ├── session.ts                # Session management
│       ├── r2.ts                     # R2 file storage
│       ├── security.ts               # Crypto utilities
│       └── ...
├── prisma/
│   └── schema.prisma                 # Database schema (25+ models)
├── student-app-rust/                 # Android app
│   └── android-wrapper/
│       ├── app/
│       │   ├── build.gradle.kts      # App-level Gradle config
│       │   └── src/main/
│       │       ├── java/app/dreamkorea/smartclass/
│       │       │   ├── api/          # Retrofit API interface + models
│       │       │   ├── data/         # AppState (cache, session, settings)
│       │       │   ├── notifications/# NotificationService (polling)
│       │       │   ├── ui/           # Compose screens
│       │       │   │   ├── Screens.kt          # Home, Tests, Books, Videos, Profile
│       │       │   │   ├── ExamScreen.kt       # Exam taking
│       │       │   │   ├── QuestionBankScreen.kt # Question bank practice
│       │       │   │   ├── LoginScreen.kt     # OTP login
│       │       │   │   ├── Theme.kt            # Theme + skeleton loaders
│       │       │   │   └── ...
│       │       │   ├── DreamKoreaApp.kt        # Application class
│       │       │   └── MainActivity.kt         # Entry point
│       │       └── res/              # Resources (icons, images, strings)
│       ├── build.gradle.kts          # Project-level Gradle config
│       ├── settings.gradle.kts
│       └── gradle.properties
├── scripts/                          # Build/utility scripts
│   ├── seed-question-bank.ts         # Seed sample data
│   ├── generate-icons.py             # Generate launcher icons
│   └── build-v120.sh                 # APK build script
├── download/                         # Built APKs
│   └── DreamKorea-SmartClass-v1.5.0.apk
├── .env.complete                     # ALL credentials (see Environment Setup)
├── prisma/schema.prisma              # Database schema
├── package.json
├── vercel.json
└── README.md                         # This file
```

---

## 🔧 Environment Setup

1. **Copy the complete env file:**
   ```bash
   cp .env.complete .env.local
   ```

2. **Fill in missing values** — open `.env.local` and fill in the keys marked with `TODO`:
   - `SUPABASE_PUBLISHABLE_KEY` — from https://supabase.com/dashboard/project/rupwlrmtcwzqaohbsswj/settings/api (anon public key)
   - `SUPABASE_SECRET_KEY` — same page (service_role secret key)
   - `R2_ACCESS_KEY_ID` — from https://dash.cloudflare.com/ → R2 → Manage R2 API Tokens
   - `R2_SECRET_ACCESS_KEY` — same page
   - `GROQ_API_KEY` — from https://console.groq.com/keys
   - `RESEND_API_KEY` — from https://resend.com/api-keys

   **OR** get all values from Vercel dashboard:
   https://vercel.com/kushal7687s-projects/my-project/settings/environment-variables

3. **Install dependencies:**
   ```bash
   bun install  # or npm install
   ```

4. **Generate Prisma client:**
   ```bash
   npx prisma generate
   ```

5. **Push schema to database:**
   ```bash
   npx prisma db push
   ```

---

## 💻 Local Development

```bash
# Start dev server
bun run dev  # or npm run dev

# Open http://localhost:3000
# Admin panel: http://localhost:3000/admin
```

**Admin login:** `admin` / `DreamKorea@2026`

---

## 🚀 Deployment

### Web (Vercel)
The app auto-deploys when you push to `main` branch on GitHub. Vercel is connected to the repo.

**Manual deploy:**
```bash
# Using Vercel CLI
npm i -g vercel
vercel --prod
```

**Or trigger via API:**
```bash
curl -X POST "https://api.vercel.com/v13/deployments" \
  -H "Authorization: Bearer $VERCEL_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"my-project","target":"production","gitSource":{"type":"github","org":"kushal7687","repo":"dreamkorea-smartclass","ref":"main"}}'
```

### Database (Supabase)
Push schema changes:
```bash
DATABASE_URL="postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres" npx prisma db push
```

---

## 📱 Android App Build

### Prerequisites
- Android SDK (platform-34, build-tools 34.0.0)
- Java 21
- Gradle 8.7 (included via wrapper)

### Build APK
```bash
cd student-app-rust/android-wrapper

# Set SDK path
echo "sdk.dir=/path/to/android-sdk" > local.properties

# Build debug APK
./gradlew assembleDebug --no-daemon

# APK output:
# app/build/outputs/apk/debug/app-debug.apk
```

### Build script (automated)
```bash
bash /home/z/my-project/scripts/build-v120.sh
```

### Current version
- **Version:** 1.5.0
- **Version Code:** 13
- **Package:** `app.dreamkorea.smartclass`
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

---

## 📡 API Reference

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/request-otp` | Request OTP code (email) |
| POST | `/api/auth/verify-otp` | Verify OTP + login (returns sessionToken) |
| POST | `/api/auth/credentials` | Login with username/password (admin/teacher) |
| GET | `/api/auth/me` | Get current user |
| POST | `/api/auth/logout` | Logout |

### Student
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/student/home-cards` | Home screen cards |
| GET | `/api/student/tests?filter=all\|practice\|exam\|ubt\|free\|batch` | Test list |
| GET | `/api/student/tests/[id]` | Test detail with questions |
| POST | `/api/student/tests/[id]/submit` | Submit test answers |
| GET | `/api/student/question-bank` | Question bank questions |
| GET | `/api/student/books` | Book list |
| GET | `/api/student/audio-lessons` | Audio lessons |
| GET | `/api/student/video-lessons` | Video lessons |
| GET | `/api/student/stats` | User stats |
| GET | `/api/student/notifications?since=<date>` | Poll for notifications |

### Admin
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET/POST | `/api/admin/tests` | List/create tests |
| GET/POST | `/api/admin/questions` | List/create questions |
| GET/POST | `/api/admin/question-bank` | Question bank CRUD |
| GET/POST | `/api/admin/books` | Book CRUD |
| GET/POST | `/api/admin/home-cards` | Home card CRUD |
| GET/POST | `/api/admin/audio-lessons` | Audio lesson CRUD |
| GET/POST | `/api/admin/video-lessons` | Video lesson CRUD |
| GET/POST | `/api/admin/notifications` | List/send notifications |
| POST | `/api/admin/file-upload` | Upload file to R2 |
| POST | `/api/admin/ai/generate` | AI question generation |
| GET | `/api/admin/users` | List users |
| GET | `/api/admin/overview` | Dashboard stats |

### Files
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/files/[...path]` | Serve file from R2 |

---

## 🔑 Credentials

All credentials are in `.env.complete`. Key ones:

| Service | Credential | Value |
|---------|-----------|-------|
| Admin Panel | URL | https://my-project-five-sepia.vercel.app/admin |
| Admin Panel | Username | `admin` |
| Admin Panel | Password | `DreamKorea@2026` |
| Supabase | Project Ref | `rupwlrmtcwzqaohbsswj` |
| Supabase | DB Password | `assissobigsexylady` |
| Supabase | URL | https://rupwlrmtcwzqaohbsswj.supabase.co |
| R2 | Account ID | `329eedd287ffd5f7ee7f2e93ab940e9a` |
| R2 | Bucket | `dreamkorea` |
| Vercel | Token | `vcp_4JXD1IOQhzuJqJnedq6cirDGv2i6FMwIefTekOkZJQgSQduJcH0BjQaj` |
| GitHub | Token | `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` |
| GitHub | User | `kushal7687` |
| GitHub | Repo | `dreamkorea-smartclass` (private) |

---

## 📝 Notes

- **Supabase connection:** Use the pooler URL (`aws-1-ap-south-1.pooler.supabase.com:5432`) with project ref in username (`postgres.rupwlrmtcwzqaohbsswj`). Direct connection (`db.rupwlrmtcwzqaohbsswj.supabase.co:5432`) may be blocked on free tier.
- **R2 file serving:** Files are served through `/api/files/[...path]` which proxies R2. This avoids needing a public R2 bucket.
- **Notification system:** Uses polling (every 60s) instead of FCM — no external service needed. Admin sends via API, students poll and show local notifications.
- **Caching:** App uses stale-while-revalidate pattern — cached data shows instantly, refreshes in background.
- **Session:** Cookie-based (`ep_sid`), stored in DB as SHA-256 hash. Mobile app persists token in SharedPreferences and sends as cookie.

---

## 🤝 Contributing

This is a private project. To make changes:
1. Clone the repo
2. Create a feature branch
3. Make changes
4. Push to `main` (auto-deploys to Vercel)
5. Rebuild APK if mobile changes (`scripts/build-v120.sh`)

---

## 📄 License

Private project. All rights reserved.
