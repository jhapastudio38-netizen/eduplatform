import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { createQuestionSchema } from "@/lib/security";
import { audit } from "@/lib/audit";
import { z } from "zod";

// Extended schema with audio loop fields (not in the base createQuestionSchema)
const extendedQuestionSchema = createQuestionSchema.extend({
  imageUrl: z.string().url().optional().or(z.literal("")),
  audioUrl: z.string().url().optional().or(z.literal("")),
  audioLoop: z.number().int().min(-1).max(20).default(0),
  audioLoopDelay: z.number().int().min(0).max(60).default(0),
});

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const questions = await db.question.findMany({
    orderBy: { createdAt: "desc" },
    take: 200,
    include: { chapter: { select: { title: true } } },
  });
  const out = questions.map((q) => ({
    ...q,
    options: q.options ? JSON.parse(q.options) : null,
    correctAnswer: q.correctAnswer,
    tags: q.tags ? JSON.parse(q.tags) : [],
  }));
  return NextResponse.json({ questions: out });
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = extendedQuestionSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });

  const { chapterId, type, difficulty, stem, options, correctAnswer, explanation, tags, imageUrl, audioUrl, audioLoop, audioLoopDelay } = parsed.data;
  const q = await db.question.create({
    data: {
      chapterId: chapterId || null,
      type, difficulty, stem,
      options: options ? JSON.stringify(options) : null,
      correctAnswer: correctAnswer || null,
      explanation: explanation || null,
      tags: tags ? JSON.stringify(tags) : null,
      imageUrl: imageUrl || null,
      audioUrl: audioUrl || null,
      audioLoop: audioLoop || 0,
      audioLoopDelay: audioLoopDelay || 0,
    },
  });
  await audit({ actorId: user.id, action: "create_question", entity: "Question", entityId: q.id, metadata: { type } });
  return NextResponse.json({ question: { ...q, options, tags: tags || [] } });
}
