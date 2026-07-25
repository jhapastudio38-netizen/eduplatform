/**
 * POST /api/admin/tests/[testId]/publish
 * Pushes the exam to the student app (sets isPublished = true).
 * Also validates that the exam has at least 1 question before publishing.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";

export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId } = await ctx.params;

  const test = await db.test.findUnique({
    where: { id: testId },
    include: { _count: { select: { items: true } } },
  });
  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

  // Validate: must have at least 1 question
  if (test._count.items === 0) {
    return NextResponse.json(
      { error: "Cannot push: exam has no questions. Add at least one question first." },
      { status: 400 },
    );
  }

  const updated = await db.test.update({
    where: { id: testId },
    data: { isPublished: true, isActive: true },
  });

  await audit({
    actorId: user.id,
    action: "publish_test",
    entity: "Test",
    entityId: testId,
  });

  return NextResponse.json({
    ok: true,
    test: updated,
    message: `Pushed to app — ${test._count.items} questions live for students`,
  });
}
