import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";

export async function GET() {
  const tests = await db.test.findMany({
    where: { isPublished: true },
    orderBy: { createdAt: "desc" },
    include: {
      _count: { select: { items: true } },
    },
  });
  // serialize
  const out = tests.map((t) => ({
    id: t.id, title: t.title, description: t.description,
    durationMin: t.durationMin, isExam: t.isExam, passScore: t.passScore,
    startAt: t.startAt, endAt: t.endAt, isPublished: t.isPublished,
    items: [], // do not leak questions on the listing
    questionCount: t._count.items,
  }));
  return NextResponse.json({ tests: out });
}
