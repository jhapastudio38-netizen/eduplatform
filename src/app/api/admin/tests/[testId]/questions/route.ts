/**
 * POST /api/admin/tests/[testId]/questions
 * Links a question to a test (adds to TestItem)
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => ({}));
  const { questionId, points } = body;
  if (!questionId) return NextResponse.json({ error: "questionId required" }, { status: 400 });

  // Get the next order number
  const count = await db.testItem.count({ where: { testId } });
  
  const item = await db.testItem.create({
    data: {
      testId,
      questionId,
      points: points || 1,
      order: count,
    },
  });
  return NextResponse.json({ ok: true, item });
}
