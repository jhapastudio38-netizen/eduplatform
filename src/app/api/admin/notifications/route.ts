/**
 * GET  /api/admin/notifications          — list all notifications
 * POST /api/admin/notifications          — create + broadcast a notification
 *
 * Body: { title, body, category? }
 * The notification is stored in DB and will be fetched by all student devices
 * via /api/student/notifications (polled every 60s).
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import { z } from "zod";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const notifications = await db.notification.findMany({
    orderBy: { createdAt: "desc" },
    take: 100,
  });
  return NextResponse.json({ notifications });
}

const schema = z.object({
  title: z.string().min(2).max(200),
  body: z.string().min(2).max(1000),
  category: z.string().max(50).optional().default("general"),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid notification" },
      { status: 400 },
    );
  }
  try {
    const notification = await db.notification.create({
      data: {
        title: parsed.data.title,
        body: parsed.data.body,
        category: parsed.data.category,
        createdBy: user.id,
      },
    });
    await audit({
      actorId: user.id,
      action: "send_notification",
      entity: "Notification",
      entityId: notification.id,
    });
    return NextResponse.json({ notification });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
