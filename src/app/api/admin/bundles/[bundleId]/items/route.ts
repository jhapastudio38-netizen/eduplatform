/**
 * POST /api/admin/bundles/[bundleId]/items
 *   Body: { testId, sortOrder? }
 *
 * Adds a Test to a QuestionBundle. Rules enforced by bundle kind:
 *   • "batch"  — only tests with testCategory="batch" can be added
 *   • "qbank"  — any test category can be added (admin chooses from anywhere)
 *   • "exam"   — any test category can be added
 *   • "chapter"— any test category can be added
 *
 * The relation is unique (bundleId+testId) so adding the same test twice is
 * idempotent.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest, ctx: { params: Promise<{ bundleId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { bundleId } = await ctx.params;
  const body = await req.json().catch(() => ({}));
  const testId = typeof body?.testId === "string" ? body.testId : "";
  if (!testId) return NextResponse.json({ error: "testId is required" }, { status: 400 });

  // Verify the bundle and the test both exist
  const [bundle, test] = await Promise.all([
    db.questionBundle.findUnique({ where: { id: bundleId }, select: { id: true, kind: true } }),
    db.test.findUnique({ where: { id: testId }, select: { id: true, title: true, testCategory: true } }),
  ]);
  if (!bundle) return NextResponse.json({ error: "Bundle not found" }, { status: 404 });
  if (!test) return NextResponse.json({ error: "Test not found" }, { status: 404 });

  // Enforce category restriction for batch bundles
  if (bundle.kind === "batch" && test.testCategory !== "batch") {
    return NextResponse.json(
      {
        error: `Batch packages can only contain batch exams. This test is a "${test.testCategory || "exam"}" test.`,
      },
      { status: 400 },
    );
  }

  // Figure out the next sort order
  const maxOrder = await db.questionBundleItem.aggregate({
    where: { bundleId },
    _max: { sortOrder: true },
  });
  const sortOrder = typeof body?.sortOrder === "number"
    ? body.sortOrder
    : (maxOrder._max.sortOrder ?? 0) + 1;

  // Upsert — idempotent if added twice
  const item = await db.questionBundleItem.upsert({
    where: { bundleId_testId: { bundleId, testId } },
    update: { sortOrder },
    create: { bundleId, testId, sortOrder },
  });
  return NextResponse.json({ item });
}

/**
 * GET /api/admin/bundles/[bundleId]/items
 * Returns all tests in the bundle, sorted by sortOrder.
 */
export async function GET(req: NextRequest, ctx: { params: Promise<{ bundleId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { bundleId } = await ctx.params;
  const items = await db.questionBundleItem.findMany({
    where: { bundleId },
    orderBy: { sortOrder: "asc" },
    include: {
      test: {
        select: {
          id: true,
          title: true,
          testCategory: true,
          examType: true,
          durationMin: true,
          isPublished: true,
          _count: { select: { items: true } },
        },
      },
    },
  });
  return NextResponse.json({ items });
}
