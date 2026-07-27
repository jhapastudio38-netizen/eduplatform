/**
 * POST /api/student/eye-vision/[testId]/check
 *   Body: { answer: string }
 *
 * 1. Looks up the test to get the correct answer + level
 * 2. Compares case-insensitively
 * 3. Persists an EyeVisionAttempt for adaptive difficulty tracking
 * 4. Returns:
 *      { correct, correctAnswer, level, nextLevel, consecutiveCorrect, stats }
 *
 * The "nextLevel" field lets the app surface a "Level up!" / "Try again"
 * banner between attempts.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const { testId } = await ctx.params;
  const body = await req.json().catch(() => ({}));
  const studentAnswer = (body.answer || "").toString().trim().toLowerCase();

  const test = await db.eyeVisionTest.findUnique({
    where: { id: testId },
    select: { id: true, correctAnswer: true, level: true, category: true },
  });
  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

  const correct = test.correctAnswer.trim().toLowerCase();
  const isCorrect = studentAnswer === correct;
  const level = test.level || 1;

  // Persist the attempt
  await db.eyeVisionAttempt.create({
    data: {
      userId: user.id,
      testId,
      isCorrect,
      userAnswer: studentAnswer.slice(0, 500) || null,
      category: test.category,
      level,
    },
  });

  // Pull recent attempts (including the one we just inserted) to compute
  // the next recommended level + stats.
  const recent = await db.eyeVisionAttempt.findMany({
    where: { userId: user.id },
    orderBy: { createdAt: "desc" },
    take: 10,
    select: { isCorrect: true, level: true, createdAt: true },
  });

  let consecutiveCorrect = 0;
  for (const a of recent) {
    if (a.isCorrect) consecutiveCorrect++;
    else break;
  }
  let consecutiveWrong = 0;
  for (const a of recent) {
    if (!a.isCorrect) consecutiveWrong++;
    else break;
  }

  let nextLevel = level;
  if (consecutiveCorrect >= 2) {
    nextLevel = Math.min(5, level + 1);
  } else if (consecutiveWrong >= 2) {
    nextLevel = Math.max(1, level - 1);
  }

  const totalAttempts = recent.length;
  const correctCount = recent.filter((a) => a.isCorrect).length;
  const accuracy = totalAttempts > 0 ? Math.round((correctCount / totalAttempts) * 100) : 0;

  return NextResponse.json({
    correct: isCorrect,
    correctAnswer: test.correctAnswer,
    level,
    nextLevel,
    leveledUp: nextLevel > level,
    leveledDown: nextLevel < level,
    consecutiveCorrect,
    consecutiveWrong,
    stats: { totalAttempts, correctAttempts: correctCount, accuracy, consecutiveCorrect },
  });
}
