/**
 * Admin Question Bank API
 *
 * GET  /api/admin/question-bank           — list all question bank questions
 * POST /api/admin/question-bank           — create a new question bank question
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import { z } from "zod";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const questions = await db.question.findMany({
    where: { inQuestionBank: true },
    orderBy: { createdAt: "desc" },
    take: 500,
  });
  return NextResponse.json({
    questions: questions.map((q) => ({
      id: q.id,
      type: q.type,
      difficulty: q.difficulty,
      stem: q.stem,
      options: q.options ? JSON.parse(q.options) : null,
      correctAnswer: q.correctAnswer ? JSON.parse(q.correctAnswer) : null,
      explanation: q.explanation,
      imageUrl: q.imageUrl,
      audioUrl: q.audioUrl,
      audioLoop: q.audioLoop,
      audioLoopDelay: q.audioLoopDelay,
      category: q.category,
      createdAt: q.createdAt,
    })),
  });
}

const questionSchema = z.object({
  type: z.enum([
    "SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE",
    "ONE_WORD", "SHORT_ANSWER", "LONG_ANSWER", "FILL_BLANK", "MATCHING",
  ]),
  difficulty: z.enum(["EASY", "MEDIUM", "HARD"]).default("MEDIUM"),
  stem: z.string().min(2).max(2000),
  options: z.array(z.string()).optional().default([]),
  correctAnswer: z.string().or(z.array(z.string())).optional(),
  explanation: z.string().optional().default(""),
  imageUrl: z.string().url().optional().or(z.literal("")).optional(),
  audioUrl: z.string().url().optional().or(z.literal("")).optional(),
  audioLoop: z.number().int().min(0).max(100).default(0),
  audioLoopDelay: z.number().int().min(0).max(60).default(0),
  category: z.string().max(100).default("General"),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = questionSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid question data" },
      { status: 400 },
    );
  }
  const d = parsed.data;
  try {
    const question = await db.question.create({
      data: {
        type: d.type,
        difficulty: d.difficulty,
        stem: d.stem,
        options: d.options.length > 0 ? JSON.stringify(d.options) : null,
        correctAnswer: d.correctAnswer ? JSON.stringify(d.correctAnswer) : null,
        explanation: d.explanation || null,
        imageUrl: d.imageUrl || null,
        audioUrl: d.audioUrl || null,
        audioLoop: d.audioLoop,
        audioLoopDelay: d.audioLoopDelay,
        inQuestionBank: true,
        category: d.category,
      },
    });
    await audit({
      actorId: user.id,
      action: "create_question_bank",
      entity: "Question",
      entityId: question.id,
    });
    return NextResponse.json({ question });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
