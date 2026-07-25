/**
 * POST /api/student/eye-vision/[testId]/check
 * Body: { answer: string }
 *
 * Checks if the student's answer matches the correct answer (case-insensitive).
 * Returns: { correct: boolean, correctAnswer: string }
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
  const correctAnswer = (body.correctAnswer || "").toString().trim().toLowerCase();

  // Fetch the correct answer from DB if not provided
  let correct = correctAnswer;
  if (!correct) {
    const test = await db.eyeVisionTest.findUnique({ where: { id: testId } });
    if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });
    correct = test.correctAnswer.trim().toLowerCase();
  }

  const isCorrect = studentAnswer === correct;
  return NextResponse.json({
    correct: isCorrect,
    correctAnswer: correct, // return so app can show it
  });
}
