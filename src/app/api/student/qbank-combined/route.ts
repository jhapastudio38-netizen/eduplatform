import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  // Get all question_bank tests
  const tests = await db.test.findMany({
    where: { testCategory: "question_bank", isPublished: true, isActive: true },
    include: {
      items: {
        include: { question: true },
        orderBy: { order: "asc" },
      },
    },
  });

  // Combine all questions from all question_bank tests
  const allItems: any[] = [];
  let totalDuration = 0;
  tests.forEach(t => {
    totalDuration += t.durationMin || 0;
    t.items.forEach(item => {
      allItems.push({
        id: `combined-${item.id}`,
        order: allItems.length + 1,
        points: item.points,
        question: item.question,
      });
    });
  });

  return NextResponse.json({
    test: {
      id: "qbank-combined",
      title: "Question Bank (All Questions)",
      description: "All questions from the question bank combined",
      durationMin: totalDuration || 60,
      isExam: false,
      passScore: 40,
      textBlockEnabled: true,
      audioBlockEnabled: true,
      showAllBlocks: false,
      items: allItems,
    },
  });
}
