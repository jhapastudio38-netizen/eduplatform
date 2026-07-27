/**
 * DELETE /api/admin/tests/[testId]/questions/[itemId]
 *
 * Removes a TestItem (and its linked Question if no other Test references it)
 * from a test. Used by the simple-mode question editor
 * (batch / chapter / question_bank categories).
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function DELETE(req: NextRequest, ctx: { params: Promise<{ testId: string; itemId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId, itemId } = await ctx.params;
  try {
    const item = await db.testItem.findUnique({ where: { id: itemId } });
    if (!item || item.testId !== testId) {
      return NextResponse.json({ error: "Not found" }, { status: 404 });
    }
    const questionId = item.questionId;

    await db.testItem.delete({ where: { id: itemId } });

    // Delete the Question only if no other TestItem references it
    const otherRefs = await db.testItem.count({ where: { questionId } });
    if (otherRefs === 0) {
      await db.question.delete({ where: { id: questionId } }).catch(() => {});
    }

    return NextResponse.json({ ok: true });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
