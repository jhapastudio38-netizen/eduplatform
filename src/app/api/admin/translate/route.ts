/**
 * POST /api/admin/translate
 * Body: { text: string, target?: "ko" | "en", source?: "en" | "ko" }
 *
 * Free English↔Korean translation using Groq LLM (no external API key needed
 * beyond what's already configured). Returns the translated text.
 *
 * Used by admin to quickly type questions in English and translate to Korean
 * with one click.
 */
import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/session";
import { rateLimited } from "@/lib/rate-limit";
import { rateLimitKey } from "@/lib/security";

const GROQ_API_KEY = process.env.GROQ_API_KEY;
const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
const GROQ_MODEL = process.env.GROQ_MODEL || "llama-3.3-70b-versatile";

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  // Rate limit: 30 translations per minute per user
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  if (rateLimited(rateLimitKey("translate", user.id), 30, 60)) {
    return NextResponse.json({ error: "Too many translation requests. Slow down." }, { status: 429 });
  }

  const body = await req.json().catch(() => null) as { text?: string; target?: string; source?: string };
  const text = (body.text || "").trim().slice(0, 2000);
  const target = body.target === "en" ? "en" : "ko";
  const source = body.source === "ko" ? "ko" : "en";

  if (!text) {
    return NextResponse.json({ error: "Text is required" }, { status: 400 });
  }

  if (!GROQ_API_KEY) {
    return NextResponse.json({ error: "Translation service not configured" }, { status: 503 });
  }

  const targetName = target === "ko" ? "Korean (Hangul)" : "English";
  const sourceName = source === "ko" ? "Korean" : "English";

  const prompt = `You are a professional translator. Translate the following ${sourceName} text to ${targetName}. Return ONLY the translation, no explanations, no quotes, no preamble. Preserve any formatting, numbers, or proper nouns.

Text to translate:
${text}`;

  try {
    const groqRes = await fetch(GROQ_URL, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${GROQ_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: GROQ_MODEL,
        messages: [
          { role: "system", content: "You are a professional Korean-English translator. Be precise and natural. Return only the translation." },
          { role: "user", content: prompt },
        ],
        temperature: 0.3,
        max_tokens: 1000,
      }),
    });

    if (!groqRes.ok) {
      const errText = await groqRes.text().catch(() => "Unknown error");
      console.error("Groq translate error:", errText);
      return NextResponse.json({ error: "Translation failed" }, { status: 502 });
    }

    const data = await groqRes.json();
    const translation = data.choices?.[0]?.message?.content?.trim() || "";

    return NextResponse.json({
      ok: true,
      translation,
      source,
      target,
    });
  } catch (e) {
    console.error("Translate error:", e);
    return NextResponse.json({ error: "Translation service unavailable" }, { status: 503 });
  }
}
