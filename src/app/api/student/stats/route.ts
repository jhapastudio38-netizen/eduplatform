/**
 * GET /api/student/stats — fetch the current user's stats (real-time)
 * Returns aggregated UserStat + recent activity.
 */
import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET() {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  // Get or create the user's stats record
  let stat = await db.userStat.findUnique({ where: { userId: user.id } });
  if (!stat) {
    stat = await db.userStat.create({ data: { userId: user.id } });
  }

  // Recent submissions (last 10)
  const recentSubmissions = await db.submission.findMany({
    where: { userId: user.id, submittedAt: { not: null } },
    orderBy: { submittedAt: "desc" },
    take: 10,
    select: {
      id: true,
      score: true,
      maxScore: true,
      graded: true,
      submittedAt: true,
      test: { select: { id: true, title: true, isExam: true } },
    },
  });

  // Books in progress
  const booksInProgress = await db.bookProgress.findMany({
    where: { userId: user.id, completed: false },
    take: 5,
    select: {
      id: true,
      currentPage: true,
      percent: true,
      book: { select: { id: true, title: true, coverUrl: true, pageCount: true } },
    },
  });

  // Audio lessons in progress
  const audioInProgress = await db.audioLessonProgress.findMany({
    where: { userId: user.id, completed: false },
    take: 5,
    select: {
      id: true,
      listenedSec: true,
      audioLesson: {
        select: { id: true, title: true, durationSec: true, audioUrl: true },
      },
    },
  });

  return NextResponse.json({
    stats: {
      totalExamsTaken: stat.totalExamsTaken,
      totalCorrectAnswers: stat.totalCorrectAnswers,
      totalQuestionsAnswered: stat.totalQuestionsAnswered,
      averageScore: stat.averageScore,
      studyStreakDays: stat.studyStreakDays,
      lastStudyDate: stat.lastStudyDate,
      totalTimeSpentMin: stat.totalTimeSpentMin,
      booksRead: stat.booksRead,
      audioLessonsCompleted: stat.audioLessonsCompleted,
      badgesEarned: stat.badgesEarned,
    },
    recentSubmissions,
    booksInProgress,
    audioInProgress,
  });
}
