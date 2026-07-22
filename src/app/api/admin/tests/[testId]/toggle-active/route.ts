import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const test = await db.test.findUnique({ where: { id: testId } });
  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });
  const updated = await db.test.update({
    where: { id: testId },
    data: { isActive: !test.isActive },
  });
  return NextResponse.json({ test: updated });
}
