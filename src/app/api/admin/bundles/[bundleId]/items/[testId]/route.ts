/**
 * DELETE /api/admin/bundles/[bundleId]/items/[testId]
 * Removes a Test from a QuestionBundle.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function DELETE(req: NextRequest, ctx: { params: Promise<{ bundleId: string; testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { bundleId, testId } = await ctx.params;
  await db.questionBundleItem.deleteMany({
    where: { bundleId, testId },
  });
  return NextResponse.json({ ok: true });
}
