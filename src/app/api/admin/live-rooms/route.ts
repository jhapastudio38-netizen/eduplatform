/**
 * GET /api/student/live-rooms — list active live rooms
 * POST /api/admin/live-rooms — admin/teacher creates a live room
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { generateToken } from "@/lib/security";
import { z } from "zod";

export async function GET() {
  const rooms = await db.liveRoom.findMany({
    where: { isLive: true },
    orderBy: { startedAt: "desc" },
    include: {
      _count: { select: { attendees: true } },
    },
  });
  return NextResponse.json({ rooms });
}

const schema = z.object({
  title: z.string().trim().min(3).max(200),
  description: z.string().max(2000).optional(),
  subject: z.string().max(100).optional(),
  audioOnly: z.boolean().default(true),
  maxStudents: z.number().int().min(1).max(500).default(50),
  startNow: z.boolean().default(true),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }

  const { title, description, subject, audioOnly, maxStudents, startNow } = parsed.data;

  // Generate a unique 6-char room code
  let roomCode = "";
  let attempts = 0;
  while (attempts < 10) {
    roomCode = generateToken(3).toUpperCase().slice(0, 6);
    const existing = await db.liveRoom.findUnique({ where: { roomCode } });
    if (!existing) break;
    attempts++;
  }

  const room = await db.liveRoom.create({
    data: {
      hostId: user.id,
      title,
      description,
      subject,
      audioOnly,
      maxStudents,
      roomCode,
      isLive: startNow,
      startedAt: startNow ? new Date() : null,
    },
  });

  return NextResponse.json({ room });
}
