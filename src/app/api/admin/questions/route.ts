import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { createQuestionSchema } from "@/lib/security";
import { audit } from "@/lib/audit";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const questions = await db.question.findMany({
    orderBy: { createdAt: "desc" },
    take: 200,
    include: { chapter: { select: { title: true } } },
  });
  // Parse JSON-encoded fields before sending to client
  const out = questions.map((q) => ({
    ...q,
    options: q.options ? JSON.parse(q.options) : null,
    correctAnswer: q.correctAnswer, // keep raw JSON string; UI parses
    tags: q.tags ? JSON.parse(q.tags) : [],
  }));
  return NextResponse.json({ questions: out });
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const body = await req.json().catch(() => null);
  const parsed = createQuestionSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });

  const { chapterId, type, difficulty, stem, options, correctAnswer, explanation, tags } = parsed.data;
  const q = await db.question.create({
    data: {
      chapterId: chapterId || null,
      type, difficulty, stem,
      options: options ? JSON.stringify(options) : null,
      correctAnswer: correctAnswer || null,
      explanation: explanation || null,
      tags: tags ? JSON.stringify(tags) : null,
    },
  });
  await audit({ actorId: user.id, action: "create_question", entity: "Question", entityId: q.id, metadata: { type } });
  return NextResponse.json({ question: { ...q, options, tags: tags || [] } });
}
