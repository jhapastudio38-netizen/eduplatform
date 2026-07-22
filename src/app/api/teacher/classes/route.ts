import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { generateToken } from "@/lib/security";
import { z } from "zod";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || user.role !== "TEACHER") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const now = new Date();
  const [live, upcoming, past] = await Promise.all([
    db.liveClass.findFirst({
      where: { teacherId: user.id, isLive: true },
      orderBy: { scheduledAt: "desc" },
    }),
    db.liveClass.findMany({
      where: { teacherId: user.id, isLive: false, scheduledAt: { gt: now } },
      orderBy: { scheduledAt: "asc" },
    }),
    db.liveClass.findMany({
      where: { teacherId: user.id, endedAt: { not: null } },
      orderBy: { scheduledAt: "desc" },
      take: 10,
    }),
  ]);
  return NextResponse.json({ live, upcoming, past });
}

const createSchema = z.object({
  title: z.string().trim().min(3).max(200),
  description: z.string().max(2000).optional(),
  subject: z.string().max(100).optional(),
  scheduledAt: z.string().datetime(),
  durationMin: z.number().int().min(15).max(240).default(60),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "TEACHER") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = createSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }

  const liveClass = await db.liveClass.create({
    data: {
      teacherId: user.id,
      title: parsed.data.title,
      description: parsed.data.description,
      subject: parsed.data.subject,
      scheduledAt: parsed.data.scheduledAt,
      durationMin: parsed.data.durationMin,
      roomCode: generateToken(3).toUpperCase().slice(0, 6),
    },
  });
  return NextResponse.json({ liveClass });
}
