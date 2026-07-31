import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

/**
 * GET /api/student/subscription
 * Returns the student's subscription status.
 */
export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  // Admins and teachers are always "subscribed"
  if (user.role === "ADMIN" || user.role === "TEACHER") {
    return NextResponse.json({ isSubscribed: true, expiresAt: null });
  }

  const sub = await db.subscription.findFirst({
    where: {
      userId: user.id,
      expiresAt: { gt: new Date() },
    },
    orderBy: { expiresAt: "desc" },
  });

  return NextResponse.json({
    isSubscribed: !!sub,
    expiresAt: sub?.expiresAt?.toISOString() ?? null,
  });
}
