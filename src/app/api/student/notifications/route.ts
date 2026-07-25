/**
 * GET /api/student/notifications
 *
 * Returns notifications for the student.
 * Optional ?since=<ISO date> to only get new notifications since last fetch.
 * The app polls this every 60 seconds and shows new notifications as local
 * Android notifications (no FCM needed — works with simple polling).
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const sinceParam = req.nextUrl.searchParams.get("since");
  const since = sinceParam ? new Date(sinceParam) : new Date(0);

  const notifications = await db.notification.findMany({
    where: { createdAt: { gt: since } },
    orderBy: { createdAt: "desc" },
    take: 50,
  });

  return NextResponse.json({
    notifications: notifications.map((n) => ({
      id: n.id,
      title: n.title,
      body: n.body,
      category: n.category,
      createdAt: n.createdAt,
    })),
  });
}
