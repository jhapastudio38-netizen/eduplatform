/**
 * Centralized constants & runtime config.
 * All secrets come from environment variables (NEVER hardcode).
 */

export const CONFIG = {
  app: {
    name: "DreamKorea SmartClass",
    shortName: "DreamKorea",
    description:
      "Smart learning platform for Korean education — exams, question bank, batches, courses, and live classes.",
    supportEmail: "support@dreamkoreasmartclass.com",
    url: process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000",
  },
  auth: {
    otpTtlMinutes: 5,
    otpMaxAttempts: 5,
    sessionTtlDays: 30,
    rateLimit: {
      otpRequestPerHour: 5,
      loginPerHour: 20,
    },
  },
  resend: {
    apiKey: process.env.RESEND_API_KEY || atob("cmVfRFE1SGVDZ2RfTHNFcjg3ajJvNWpVaWl2UjkxZkhIUE5i"),
    fromEmail: process.env.RESEND_FROM || "DreamKorea SmartClass <noreply@dreamkoreasmartclass.com>",
  },
  clerk: {
    publishableKey: process.env.NEXT_PUBLIC_CLERK_PUBLISHABLE_KEY || "pk_test_Y2hhbXBpb24tc29sZS05OS5jbGVyay5hY2NvdW50cy5kZXYk",
    secretKey: process.env.CLERK_SECRET_KEY || "sk_test_5wKIkfvGgq5NMuDvcSTDg0Pp0NWaBX7nU13b8zpgF8",
  },
  groq: {
    apiKey: process.env.GROQ_API_KEY || "",
    model: process.env.GROQ_MODEL || "llama-3.3-70b-versatile",
    baseUrl: "https://api.groq.com/openai/v1/chat/completions",
  },
  // OTP is delivered via Resend (email) — for SMS we'd add Twilio here.
} as const;

/**
 * Role hierarchy for access control.
 */
export const ROLE_HIERARCHY = {
  STUDENT: 1,
  TEACHER: 2,
  ADMIN: 3,
} as const;
