/**
 * GET /api/student/subscription
 * Returns the current user's subscription status.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const fullUser = await db.user.findUnique({
    where: { id: user.id },
    select: { subscriptionType: true, subscribedUntil: true },
  });

  const now = new Date();
  const isSubscribed = fullUser?.subscribedUntil
    ? fullUser.subscribedUntil > now
    : false;

  return NextResponse.json({
    isSubscribed,
    subscriptionType: fullUser?.subscriptionType || null,
    subscribedUntil: fullUser?.subscribedUntil?.toISOString() || null,
  });
}
