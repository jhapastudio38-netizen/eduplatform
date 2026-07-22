import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import { z } from "zod";

const schema = z.object({
  chapterId: z.string().optional(),
  questions: z.array(z.object({
    type: z.string(),
    difficulty: z.string(),
    stem: z.string().min(3),
    options: z.array(z.string()).optional(),
    correctAnswer: z.string().optional(),
    explanation: z.string().optional(),
  })).min(1).max(50),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });

  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });

  const { chapterId, questions } = parsed.data;

  const created = await db.$transaction(
    questions.map((q) =>
      db.question.create({
        data: {
          chapterId: chapterId || null,
          type: q.type as never,
          difficulty: q.difficulty as never,
          stem: q.stem,
          options: q.options ? JSON.stringify(q.options) : null,
          correctAnswer: q.correctAnswer || null,
          explanation: q.explanation || null,
          aiGenerated: true,
        },
      }),
    ),
  );

  await audit({ actorId: user.id, action: "ai_save_questions", entity: "Question", metadata: { count: created.length, chapterId } });
  return NextResponse.json({ saved: created.length });
}
