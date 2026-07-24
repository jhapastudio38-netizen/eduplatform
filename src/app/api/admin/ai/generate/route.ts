import { NextResponse, NextRequest } from "next/server";
import { getCurrentUser } from "@/lib/session";
import { generateQuestionsWithGroq } from "@/lib/groq";
import { z } from "zod";
import { rateLimited } from "@/lib/rate-limit";
import { rateLimitKey } from "@/lib/security";

const schema = z.object({
  topic: z.string().trim().min(3).max(500).optional(),
  prompt: z.string().trim().min(3).max(500).optional(),
  count: z.number().int().min(1).max(50).default(5),
  type: z.string().default("SINGLE_CHOICE"),
  difficulty: z.string().default("MEDIUM"),
  chapterId: z.string().optional(),
}).refine(d => d.topic || d.prompt, { message: "topic or prompt required" });

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  if (rateLimited(rateLimitKey("ai-gen", user.id), 50, 3600)) {
    return NextResponse.json({ error: "Rate limit. Try again later." }, { status: 429 });
  }

  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }

  const data = {
    topic: parsed.data.topic || parsed.data.prompt || "",
    count: parsed.data.count,
    type: parsed.data.type as any,
    difficulty: parsed.data.difficulty as any,
    chapterId: parsed.data.chapterId,
  };

  try {
    const questions = await generateQuestionsWithGroq(data);
    return NextResponse.json({ questions, provider: "groq" });
  } catch (e: any) {
    // Fallback: generate simple mock questions
    const mockQuestions = [];
    for (let i = 0; i < data.count; i++) {
      mockQuestions.push({
        type: data.type,
        difficulty: data.difficulty,
        stem: `[AI] Question ${i+1} about: ${data.topic}`,
        options: ["Option A", "Option B", "Option C", "Option D"],
        correctAnswer: '"Option A"',
        explanation: `Generated for: ${data.topic}`,
        aiGenerated: true,
      });
    }
    return NextResponse.json({ 
      questions: mockQuestions, 
      provider: "mock",
      warning: e.message || "Using mock questions" 
    });
  }
}
