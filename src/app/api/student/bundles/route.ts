import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const kind = req.nextUrl.searchParams.get("kind");
  const where: any = { testCategory: { in: ["question_bank", "batch"] } };
  if (kind) where.testCategory = kind;

  const tests = await db.test.findMany({
    where: { ...where, isPublished: true, isActive: true },
    orderBy: { createdAt: "desc" },
    select: {
      id: true, title: true, description: true, testCategory: true,
      durationMin: true, isPublished: true,
    },
  });

  return NextResponse.json({ bundles: tests });
}
