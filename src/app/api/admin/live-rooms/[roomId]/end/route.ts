import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(_req: NextRequest, ctx: { params: Promise<{ roomId: string }> }) {
  const { roomId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  await db.liveRoom.update({
    where: { id: roomId },
    data: { isLive: false, endedAt: new Date() },
  });
  return NextResponse.json({ ok: true });
}
