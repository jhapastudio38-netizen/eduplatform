import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

const startSchema = z.object({
  title: z.string().trim().min(3).max(200),
  description: z.string().max(2000).optional(),
  subject: z.string().max(100).optional(),
  durationMin: z.number().int().min(15).max(240).default(60),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "TEACHER") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = startSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }

  // End any currently-live class for this teacher
  await db.liveClass.updateMany({
    where: { teacherId: user.id, isLive: true },
    data: { isLive: false, endedAt: new Date() },
  });

  const liveClass = await db.liveClass.create({
    data: {
      teacherId: user.id,
      title: parsed.data.title,
      description: parsed.data.description,
      subject: parsed.data.subject,
      scheduledAt: new Date().toISOString(),
      durationMin: parsed.data.durationMin,
      isLive: true,
      roomCode: Math.random().toString(36).slice(2, 8).toUpperCase(),
    },
  });
  return NextResponse.json({ liveClass });
}
