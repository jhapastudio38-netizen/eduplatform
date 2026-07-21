import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET() {
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const [lessonsCompleted, testsTaken, qaAsked] = await Promise.all([
    db.lessonProgress.count({ where: { userId: user.id, completed: true } }),
    db.submission.count({ where: { userId: user.id, submittedAt: { not: null } } }),
    db.qAQuestion.count({ where: { userId: user.id } }),
  ]);

  return NextResponse.json({
    lessonsCompleted,
    testsTaken,
    qaAsked,
    streak: 7, // placeholder — track with a daily login table in production
    recentActivity: [],
  });
}
