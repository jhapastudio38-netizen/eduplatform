/**
 * OTP delivery.
 *
 * Production wiring: Resend (https://resend.com) for email.
 * For SMS (phone OTP) plug in Twilio / AWS SNS in `sendPhoneOtp`.
 *
 * In development without a Resend key, OTPs are written to server log
 * and to a dev-only API surface so the test harness can read them.
 */

import { CONFIG } from "@/lib/config";

export interface OtpDeliveryResult {
  ok: boolean;
  channel: "email" | "phone" | "dev";
  message: string;
}

export async function sendOtpEmail(
  email: string,
  code: string,
): Promise<OtpDeliveryResult> {
  if (!CONFIG.resend.apiKey) {
    // Dev fallback
    console.log(`[DEV OTP] email=${email} code=${code}`);
    return {
      ok: true,
      channel: "dev",
      message: "OTP printed to server log (dev mode)",
    };
  }

  try {
    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${CONFIG.resend.apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: CONFIG.resend.fromEmail,
        to: email,
        subject: `Your ${CONFIG.app.name} verification code`,
        html: otpEmailHtml(code),
      }),
    });

    if (!res.ok) {
      const text = await res.text();
      return {
        ok: false,
        channel: "email",
        message: `Resend error: ${res.status} ${text.slice(0, 200)}`,
      };
    }
    return { ok: true, channel: "email", message: "OTP sent to email" };
  } catch (e) {
    return {
      ok: false,
      channel: "email",
      message: e instanceof Error ? e.message : "Unknown error",
    };
  }
}

export async function sendOtpPhone(
  phone: string,
  code: string,
): Promise<OtpDeliveryResult> {
  // TODO: integrate Twilio / AWS SNS for SMS.
  // For now, dev fallback prints to log so the flow works end-to-end.
  console.log(`[DEV OTP] phone=${phone} code=${code}`);
  return {
    ok: true,
    channel: "dev",
    message: "SMS not configured — OTP printed to server log",
  };
}

function otpEmailHtml(code: string): string {
  return `<!DOCTYPE html>
<html>
  <body style="font-family: -apple-system, BlinkMacSystemFont, sans-serif; background:#f6f7fb; padding:32px 0;">
    <div style="max-width:480px;margin:0 auto;background:#fff;border-radius:16px;padding:32px;box-shadow:0 4px 16px rgba(0,0,0,.04);">
      <h1 style="margin:0 0 8px;font-size:22px;color:#0f172a;">${CONFIG.app.name}</h1>
      <p style="color:#475569;margin:0 0 24px;">Use this code to verify it's you. It expires in ${CONFIG.auth.otpTtlMinutes} minutes.</p>
      <div style="font-size:36px;font-weight:700;letter-spacing:8px;text-align:center;padding:24px;background:#f1f5f9;border-radius:12px;color:#0f172a;">${code}</div>
      <p style="color:#94a3b8;font-size:12px;margin:24px 0 0;">If you didn't request this code, you can safely ignore this email.</p>
    </div>
  </body>
</html>`;
}
