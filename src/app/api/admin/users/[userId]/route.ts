/**
 * GET /api/admin/users/[userId]
 *
 * Returns full user details including:
 *   - Account info (name, email, phone, role, banned, verified, signup)
 *   - Subscription info (type, expiry, price)
 *   - Stats (exams taken, average score, questions answered, study streak)
 *   - Recent submissions (last 20)
 *
 * Admin-only.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest, ctx: { params: Promise<{ userId: string }> }) {
  const admin = await getCurrentUser(req);
  if (!admin || admin.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { userId } = await ctx.params;

  const user = await db.user.findUnique({
    where: { id: userId },
    select: {
      id: true,
      name: true,
      email: true,
      phone: true,
      username: true,
      role: true,
      avatarUrl: true,
      isBanned: true,
      isVerified: true,
      signupMethod: true,
      createdBy: true,
      lastActiveAt: true,
      createdAt: true,
      subscriptionType: true,
      subscribedUntil: true,
      subscriptionPrice: true,
    },
  });

  if (!user) {
    return NextResponse.json({ error: "User not found" }, { status: 404 });
  }

  // Get user stats
  const stats = await db.userStat.findUnique({
    where: { userId },
  });

  // Get recent submissions (last 20)
  const submissions = await db.submission.findMany({
    where: { userId },
    orderBy: { submittedAt: "desc" },
    take: 20,
    select: {
      id: true,
      testId: true,
      score: true,
      maxScore: true,
      graded: true,
      startedAt: true,
      submittedAt: true,
      test: {
        select: {
          id: true,
          title: true,
          testCategory: true,
          examType: true,
        },
      },
    },
  });

  // Count total submissions
  const totalSubmissions = await db.submission.count({
    where: { userId, submittedAt: { not: null } },
  });

  // Check if subscription is active
  const now = new Date();
  const isSubscribed = user.subscribedUntil ? user.subscribedUntil > now : false;

  return NextResponse.json({
    user: {
      ...user,
      isSubscribed,
    },
    stats: stats || {
      totalExamsTaken: 0,
      totalCorrectAnswers: 0,
      totalQuestionsAnswered: 0,
      averageScore: 0,
      studyStreakDays: 0,
      lastStudyDate: null,
    },
    submissions,
    totalSubmissions,
  });
}

/**
 * PATCH /api/admin/users/[userId]
 * Body: { subscriptionType?, subscribedUntil?, subscriptionPrice?, isBanned?, role? }
 *
 * Admin updates user — subscribe, ban, change role, etc.
 */
export async function PATCH(req: NextRequest, ctx: { params: Promise<{ userId: string }> }) {
  const admin = await getCurrentUser(req);
  if (!admin || admin.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const { userId } = await ctx.params;
  const body = await req.json().catch(() => ({}));

  const data: Record<string, unknown> = {};
  if (body.subscriptionType !== undefined) {
    data.subscriptionType = body.subscriptionType || null;
  }
  if (body.subscribedUntil !== undefined) {
    data.subscribedUntil = body.subscribedUntil ? new Date(body.subscribedUntil) : null;
  }
  if (body.subscriptionPrice !== undefined) {
    data.subscriptionPrice = typeof body.subscriptionPrice === "number" ? body.subscriptionPrice : null;
  }
  if (body.isBanned !== undefined) {
    data.isBanned = Boolean(body.isBanned);
  }
  if (body.role !== undefined && ["STUDENT", "TEACHER", "ADMIN"].includes(body.role)) {
    data.role = body.role;
  }

  const updated = await db.user.update({
    where: { id: userId },
    data,
    select: {
      id: true,
      name: true,
      email: true,
      role: true,
      isBanned: true,
      subscriptionType: true,
      subscribedUntil: true,
      subscriptionPrice: true,
    },
  });

  return NextResponse.json({ ok: true, user: updated });
}
