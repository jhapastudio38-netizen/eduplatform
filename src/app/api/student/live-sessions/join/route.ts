/**
 * POST /api/student/live-sessions/join
 * Body: { joinCode: string }
 *
 * Student enters the code given by teacher. If valid, returns the meeting
 * link + credentials. If invalid, returns error.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const body = await req.json().catch(() => ({}));
  const joinCode = (body.joinCode || "").toString().trim().toUpperCase();

  if (!joinCode || joinCode.length < 3) {
    return NextResponse.json({ error: "Enter a valid code" }, { status: 400 });
  }

  const session = await db.liveSession.findFirst({
    where: { joinCode, isActive: true },
  });

  if (!session) {
    return NextResponse.json({ error: "Invalid code or session has ended" }, { status: 404 });
  }

  return NextResponse.json({
    ok: true,
    session: {
      id: session.id,
      title: session.title,
      description: session.description,
      meetingUrl: session.meetingUrl,
      credentials: session.credentials,
      hostName: session.hostName,
    },
  });
}
