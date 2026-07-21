import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || user.role !== "TEACHER") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  // Aggregate student progress across all subjects.
  // In production this would be a materialized view refreshed by a cron.
  const students = await db.user.findMany({
    where: { role: "STUDENT" },
    select: {
      id: true, name: true, email: true,
      _count: {
        select: {
          submissions: { where: { submittedAt: { not: null } } },
        },
      },
    },
    take: 100,
  });

  const out = await Promise.all(
    students.map(async (s) => {
      const lessonsCompleted = await db.lessonProgress.count({
        where: { userId: s.id, completed: true },
      });
      const submissions = await db.submission.findMany({
        where: { userId: s.id, graded: true, score: { not: null } },
        select: { score: true, maxScore: true },
      });
      const avgScore = submissions.length
        ? Math.round(
            (submissions.reduce((acc, x) => acc + (x.score || 0), 0) /
              submissions.reduce((acc, x) => acc + (x.maxScore || 1), 0)) *
              100,
          )
        : 0;
      return {
        id: s.id, name: s.name, email: s.email,
        lessonsCompleted,
        testsTaken: s._count.submissions,
        avgScore,
      };
    }),
  );

  return NextResponse.json({ students: out });
}
