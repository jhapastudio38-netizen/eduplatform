/**
 * GET /api/student/bundles
 *   ?kind=qbank|batch|exam|chapter  (optional)
 *
 * Returns published bundles visible to the student, with their test items.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const url = new URL(req.url);
  const kind = url.searchParams.get("kind");
  const where: Record<string, unknown> = { isPublished: true };
  if (kind) where.kind = kind;

  const bundles = await db.questionBundle.findMany({
    where,
    orderBy: { createdAt: "desc" },
    select: {
      id: true,
      title: true,
      slug: true,
      description: true,
      kind: true,
      coverUrl: true,
      price: true,
      createdAt: true,
      items: {
        orderBy: { sortOrder: "asc" },
        select: {
          sortOrder: true,
          test: {
            select: {
              id: true,
              title: true,
              testCategory: true,
              examType: true,
              durationMin: true,
              passScore: true,
              featuredImage: true,
              _count: { select: { items: true } },
            },
          },
        },
      },
    },
  });

  return NextResponse.json({ bundles });
}
