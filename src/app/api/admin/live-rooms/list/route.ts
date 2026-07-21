import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const rooms = await db.liveRoom.findMany({
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { attendees: true } } },
  });
  return NextResponse.json({ rooms });
}
