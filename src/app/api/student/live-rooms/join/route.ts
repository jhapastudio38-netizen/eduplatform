/**
 * POST /api/student/live-rooms/join
 * Body: { roomCode: string }
 * Student joins a live room by entering the 6-char code.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

const schema = z.object({
  roomCode: z.string().trim().length(6).toUpperCase(),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: "Invalid room code" }, { status: 400 });
  }

  const room = await db.liveRoom.findUnique({
    where: { roomCode: parsed.data.roomCode },
  });
  if (!room) {
    return NextResponse.json({ error: "Room not found" }, { status: 404 });
  }
  if (!room.isLive) {
    return NextResponse.json({ error: "Room is not live" }, { status: 400 });
  }

  // Check capacity
  const attendeeCount = await db.liveRoomAttendee.count({
    where: { roomId: room.id, leftAt: null },
  });
  if (attendeeCount >= room.maxStudents) {
    return NextResponse.json({ error: "Room is full" }, { status: 400 });
  }

  // Join (upsert in case they re-join)
  await db.liveRoomAttendee.upsert({
    where: { roomId_userId: { roomId: room.id, userId: user.id } },
    create: {
      roomId: room.id,
      userId: user.id,
      role: "student",
      joinedAt: new Date(),
      leftAt: null,
    },
    update: { leftAt: null, joinedAt: new Date() },
  });

  return NextResponse.json({ room });
}
