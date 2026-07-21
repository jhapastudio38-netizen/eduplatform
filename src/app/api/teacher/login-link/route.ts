/**
 * GET /api/teacher/login-link?token=xxx
 * Verifies a secure teacher login token is valid and not expired.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";

export async function GET(req: NextRequest) {
  const token = req.nextUrl.searchParams.get("token");
  if (!token || token.length < 16) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  const record = await db.secureLoginToken.findUnique({
    where: { token, role: "TEACHER" },
  });

  if (!record || record.expiresAt < new Date()) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  await db.secureLoginToken.update({
    where: { id: record.id },
    data: { lastUsedAt: new Date() },
  });

  return NextResponse.json({ ok: true });
}
