import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest, ctx: { params: Promise<{ bundleId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const { bundleId } = await ctx.params;

  // Get the bundle test
  const test = await db.test.findUnique({
    where: { id: bundleId },
    include: {
      items: {
        include: { question: true },
        orderBy: { order: "asc" },
      },
    },
  });

  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

  return NextResponse.json({
    test: {
      id: `bundle-${bundleId}`,
      title: test.title,
      description: test.description,
      durationMin: test.durationMin || 60,
      isExam: test.isExam,
      passScore: test.passScore,
      textBlockEnabled: test.textBlockEnabled !== false,
      audioBlockEnabled: test.audioBlockEnabled !== false,
      showAllBlocks: test.showAllBlocks !== false,
      items: test.items.map((item, idx) => ({
        id: `bundle-${item.id}`,
        order: idx + 1,
        points: item.points,
        question: item.question,
      })),
    },
  });
}
