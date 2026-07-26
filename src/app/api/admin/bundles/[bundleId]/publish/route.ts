/**
 * POST /api/admin/bundles/[bundleId]/publish
 *   Body: { publish: boolean }
 *
 * Flips the bundle's isPublished flag. Published bundles are visible to
 * students on their app; unpublished are admin-only drafts.
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
  const publish = body?.publish !== false;

  // Refuse to publish an empty bundle
  if (publish) {
    const count = await db.questionBundleItem.count({ where: { bundleId } });
    if (count === 0) {
      return NextResponse.json({ error: "Cannot publish an empty bundle — add tests first" }, { status: 400 });
    }
  }

  const bundle = await db.questionBundle.update({
    where: { id: bundleId },
    data: { isPublished: publish },
  });
  return NextResponse.json({ bundle });
}
