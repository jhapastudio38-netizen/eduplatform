/**
 * GET /api/student/qbank-combined
 *
 * Combines ALL questions from ALL published question_bank tests into ONE
 * virtual test. The student sees all QBank questions in a single exam —
 * they solve everything at once.
 *
 * Returns the same shape as /api/student/tests/[testId] so the app can
 * use the same ExamScreen to render it.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  // Fetch all published question_bank tests with their items
  const tests = await db.test.findMany({
    where: { testCategory: "question_bank", isPublished: true },
    include: {
      items: {
        orderBy: { order: "asc" },
        include: { question: true },
      },
    },
  });

  // Combine all items from all tests into one list
  const allItems: any[] = [];
  let totalDuration = 0;
  for (const t of tests) {
    totalDuration += t.durationMin;
    for (const item of t.items) {
      allItems.push(item);
    }
  }

  // Build a virtual test response (same shape as /api/student/tests/[testId])
  const combinedTest = {
    id: "qbank-combined",
    title: "Question Bank — All Questions",
    description: `${allItems.length} questions from ${tests.length} sets`,
    durationMin: Math.max(totalDuration, 60),
    isExam: false,
    passScore: 0,
    textBlockCount: allItems.filter(i => i.question.blockType === "text").length,
    audioBlockCount: allItems.filter(i => i.question.blockType === "audio").length,
    textBlockEnabled: true,
    audioBlockEnabled: true,
    items: allItems.map((item, idx) => ({
      id: `qb-${idx}`,
      order: idx + 1,
      points: item.points,
      question: {
        id: item.question.id,
        type: item.question.type,
        difficulty: item.question.difficulty,
        stem: item.question.stem,
        options: item.question.options ? JSON.parse(item.question.options) : null,
        optionBlanks: item.question.optionBlanks ? JSON.parse(item.question.optionBlanks) : [],
        imageUrl: item.question.imageUrl || null,
        audioUrl: item.question.audioUrl || null,
        audioLoop: item.question.audioLoop || 0,
        audioLoopDelay: item.question.audioLoopDelay || 0,
        blockType: item.question.blockType || "text",
        blockNumber: idx + 1,
        setNumber: item.question.setNumber ?? 1,
        descType: item.question.descType || "none",
        descText: item.question.descText || null,
        descImageUrl: item.question.descImageUrl || null,
        descAudioUrl: item.question.descAudioUrl || null,
        mediaType: item.question.mediaType || "none",
        mediaText: item.question.mediaText || null,
        mediaImageUrl: item.question.mediaImageUrl || null,
        mediaAudioUrl: item.question.mediaAudioUrl || null,
        answerType: item.question.answerType || "text",
        optionImages: item.question.optionImages ? JSON.parse(item.question.optionImages) : [],
        optionAudios: item.question.optionAudios ? JSON.parse(item.question.optionAudios) : [],
        correctOption: item.question.correctOption ?? 0,
        explanation: item.question.explanation || null,
      },
    })),
  };

  return NextResponse.json({
    test: combinedTest,
    submissionId: "qbank-combined-session",
  });
}
