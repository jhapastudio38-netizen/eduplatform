/**
 * GET /api/student/eye-vision
 *   ?adaptive=true   — return only tests at the student's recommended level
 *
 * Adaptive difficulty:
 *   • Look at the student's last 10 attempts
 *   • If the most recent 2+ are correct, advance to the next level (max 5)
 *   • If the most recent attempt is wrong, stay at the same level (or drop
 *     one level if the last 2+ are wrong)
 *   • Default starting level = 1
 *
 * Response includes:
 *   - tests: published eye-vision tests (filtered by adaptive level when ?adaptive=true)
 *   - level: current recommended level (1-5)
 *   - stats: { totalAttempts, correctAttempts, accuracy, consecutiveCorrect }
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  // Fetch the student's recent attempts (most recent first)
  const recent = await db.eyeVisionAttempt.findMany({
    where: { userId: user.id },
    orderBy: { createdAt: "desc" },
    take: 10,
    select: { isCorrect: true, level: true, createdAt: true },
  });

  // Compute the recommended level
  let recommendedLevel = 1;
  let consecutiveCorrect = 0;
  // Walk from the most recent backward to count the consecutive-correct streak
  for (const a of recent) {
    if (a.isCorrect) consecutiveCorrect++;
    else break;
  }

  if (recent.length > 0) {
    // The most recent attempt's level is the baseline
    const lastLevel = recent[0].level || 1;
    if (consecutiveCorrect >= 2) {
      // Promote — but cap at 5
      recommendedLevel = Math.min(5, lastLevel + 1);
    } else if (recent[0].isCorrect) {
      // Single correct — stay at the same level
      recommendedLevel = lastLevel;
    } else {
      // Wrong answer — drop a level (but never below 1)
      // Count the consecutive-wrong streak
      let consecutiveWrong = 0;
      for (const a of recent) {
        if (!a.isCorrect) consecutiveWrong++;
        else break;
      }
      if (consecutiveWrong >= 2) {
        recommendedLevel = Math.max(1, lastLevel - 1);
      } else {
        // Single wrong — give them another shot at the same level
        recommendedLevel = lastLevel;
      }
    }
  }

  // Fetch tests
  const url = new URL(req.url);
  const wantAdaptive = url.searchParams.get("adaptive") === "true";
  const levelParam = url.searchParams.get("level");
  const explicitLevel = levelParam ? parseInt(levelParam, 10) : null;

  const targetLevel = explicitLevel && !isNaN(explicitLevel)
    ? Math.max(1, Math.min(5, explicitLevel))
    : recommendedLevel;

  const where = wantAdaptive
    ? { isPublished: true, level: targetLevel }
    : { isPublished: true };

  const tests = await db.eyeVisionTest.findMany({
    where,
    orderBy: [{ sortOrder: "asc" }, { createdAt: "desc" }],
    select: {
      id: true,
      title: true,
      description: true,
      imageUrl: true,
      category: true,
      level: true,
      // correctAnswer intentionally omitted
    },
  });

  const totalAttempts = recent.length;
  const correctAttempts = recent.filter((a) => a.isCorrect).length;
  const accuracy = totalAttempts > 0 ? Math.round((correctAttempts / totalAttempts) * 100) : 0;

  return NextResponse.json({
    tests,
    level: targetLevel,
    recommendedLevel,
    stats: { totalAttempts, correctAttempts, accuracy, consecutiveCorrect },
  });
}
