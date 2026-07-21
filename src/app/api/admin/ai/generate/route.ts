import { NextResponse, NextRequest } from "next/server";
import { getCurrentUser } from "@/lib/session";
import { generateQuestionsWithGroq } from "@/lib/groq";
import { z } from "zod";
import { rateLimited } from "@/lib/rate-limit";
import { rateLimitKey } from "@/lib/security";

const schema = z.object({
  topic: z.string().trim().min(3).max(500),
  count: z.number().int().min(1).max(20),
  type: z.enum([
    "SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE",
    "ONE_WORD", "SHORT_ANSWER", "LONG_ANSWER", "FILL_BLANK",
  ]),
  difficulty: z.enum(["EASY", "MEDIUM", "HARD"]),
  chapterId: z.string().optional(),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  // Rate-limit AI generation per admin (10/hour)
  if (rateLimited(rateLimitKey("ai-gen", user.id), 10, 3600)) {
    return NextResponse.json({ error: "AI generation rate limit hit. Try again later." }, { status: 429 });
  }

  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }

  try {
    const questions = await generateQuestionsWithGroq(parsed.data);
    return NextResponse.json({ questions });
  } catch (e) {
    return NextResponse.json(
      { error: e instanceof Error ? e.message : "Generation failed" },
      { status: 502 },
    );
  }
}
