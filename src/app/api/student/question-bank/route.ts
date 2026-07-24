/**
 * GET /api/student/question-bank
 *
 * Returns all question bank questions for student practice.
 * Groups by category for the browse UI.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const questions = await db.question.findMany({
    where: { inQuestionBank: true },
    orderBy: [{ category: "asc" }, { createdAt: "desc" }],
    take: 500,
  });

  // Group by category
  const byCategory: Record<string, any[]> = {};
  for (const q of questions) {
    const cat = q.category || "General";
    if (!byCategory[cat]) byCategory[cat] = [];
    byCategory[cat].push({
      id: q.id,
      type: q.type,
      difficulty: q.difficulty,
      stem: q.stem,
      options: q.options ? JSON.parse(q.options) : null,
      correctAnswer: q.correctAnswer ? JSON.parse(q.correctAnswer) : null,
      explanation: q.explanation,
      imageUrl: q.imageUrl || null,
      audioUrl: q.audioUrl || null,
      audioLoop: q.audioLoop || 0,
      audioLoopDelay: q.audioLoopDelay || 0,
      category: cat,
    });
  }

  return NextResponse.json({
    questions: questions.map((q) => ({
      id: q.id,
      type: q.type,
      difficulty: q.difficulty,
      stem: q.stem,
      options: q.options ? JSON.parse(q.options) : null,
      correctAnswer: q.correctAnswer ? JSON.parse(q.correctAnswer) : null,
      explanation: q.explanation,
      imageUrl: q.imageUrl || null,
      audioUrl: q.audioUrl || null,
      audioLoop: q.audioLoop || 0,
      audioLoopDelay: q.audioLoopDelay || 0,
      category: q.category || "General",
    })),
    categories: Object.keys(byCategory),
    byCategory,
    total: questions.length,
  });
}
