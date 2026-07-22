import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const lessons = await db.audioLesson.findMany({ orderBy: { createdAt: "desc" } });
  return NextResponse.json({ lessons });
}
