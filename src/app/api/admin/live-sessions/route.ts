import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const sessions = await db.liveSession.findMany({
    orderBy: { createdAt: "desc" },
    take: 100,
  });
  return NextResponse.json({ sessions });
}

const schema = z.object({
  title: z.string().min(2).max(200),
  description: z.string().max(2000).optional().or(z.literal("")),
  joinCode: z.string().min(3).max(50).toUpperCase(),
  meetingUrl: z.string().min(1).max(500),
  credentials: z.string().max(1000).optional().or(z.literal("")),
  hostName: z.string().max(100).optional().or(z.literal("")),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }
  const d = parsed.data;
  try {
    const session = await db.liveSession.create({
      data: {
        title: d.title,
        description: d.description || null,
        joinCode: d.joinCode,
        meetingUrl: d.meetingUrl,
        credentials: d.credentials || null,
        hostName: d.hostName || null,
        isActive: true,
        createdBy: user.id,
        startedAt: new Date(),
      },
    });
    return NextResponse.json({ session });
  } catch (e: any) {
    if (e?.code === "P2002") {
      return NextResponse.json({ error: "Join code already exists. Use a different code." }, { status: 409 });
    }
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
