/**
 * DELETE /api/admin/question-bank/[questionId]
 * Removes a question from the question bank (sets inQuestionBank=false).
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";

export async function DELETE(req: NextRequest, ctx: { params: Promise<{ questionId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { questionId } = await ctx.params;
  try {
    // If the question is used in tests, just remove from bank; otherwise delete entirely
    const question = await db.question.findUnique({
      where: { id: questionId },
      include: { _count: { select: { testItems: true } } },
    });
    if (!question) {
      return NextResponse.json({ error: "Not found" }, { status: 404 });
    }
    if (question._count.testItems > 0) {
      // Used in tests — just remove from bank
      await db.question.update({
        where: { id: questionId },
        data: { inQuestionBank: false },
      });
    } else {
      // Standalone — delete entirely
      await db.question.delete({ where: { id: questionId } });
    }
    await audit({
      actorId: user.id,
      action: "delete_question_bank",
      entity: "Question",
      entityId: questionId,
    });
    return NextResponse.json({ ok: true });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
