/**
 * Centralized constants & runtime config.
 * All secrets come from environment variables (NEVER hardcode).
 */

export const CONFIG = {
  app: {
    name: "EduPlatform",
    shortName: "EduPlatform",
    description:
      "Learn, teach, and manage — a unified education platform for students, teachers, and admins.",
    supportEmail: "support@eduplatform.app",
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
    apiKey: process.env.RESEND_API_KEY || "",
    // Resend's onboarding@resend.dev can ONLY send to the account owner's
    // verified email. For all other recipients, you must verify your own
    // domain at https://resend.com/domains and update RESEND_FROM.
    fromEmail: process.env.RESEND_FROM || "EduPlatform <onboarding@resend.dev>",
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
