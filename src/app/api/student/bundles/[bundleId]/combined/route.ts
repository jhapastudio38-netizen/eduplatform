/**
 * GET /api/student/bundles/[bundleId]/combined
 *
 * Combines ALL questions from ALL tests in a specific bundle into ONE virtual
 * exam. Used by:
 *   • Question Bank packages — admin selects tests from ANY category (batch,
 *     exam, demo, chapter, etc.) and the student gets one combined exam.
 *   • Batch packages — admin selects batch tests only, and the student gets
 *     one combined exam containing only batch exam questions.
 *
 * The student solves everything at once, like a normal exam. The test ID for
 * submission is `bundle-{bundleId}` — see /api/student/tests/[testId]/submit
 * for how it's handled.
 *
 * Response shape is identical to /api/student/tests/[testId] so the app can
 * reuse ExamEntryScreen + ExamScreen.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest, ctx: { params: Promise<{ bundleId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const { bundleId } = await ctx.params;

  const bundle = await db.questionBundle.findUnique({
    where: { id: bundleId },
    include: {
      items: {
        orderBy: { sortOrder: "asc" },
        include: {
          test: {
            include: {
              items: {
                orderBy: { order: "asc" },
                include: { question: true },
              },
            },
          },
        },
      },
    },
  });

  if (!bundle) return NextResponse.json({ error: "Bundle not found" }, { status: 404 });
  if (!bundle.isPublished) return NextResponse.json({ error: "Bundle not published" }, { status: 403 });

  // Combine all questions from all tests in the bundle into one list.
  // Each question is given a stable synthetic ID `bundle-{bundleId}-q{idx}` so
  // the Android submit endpoint can route it correctly.
  const allItems: any[] = [];
  let totalDuration = 0;
  let totalQuestions = 0;

  for (const bundleItem of bundle.items) {
    const t = bundleItem.test;
    if (!t) continue;
    totalDuration += t.durationMin || 30;
    for (const item of t.items) {
      allItems.push({
        bundleItem,
        test: t,
        item,
      });
      totalQuestions++;
    }
  }

  // Build a virtual test response (same shape as /api/student/tests/[testId])
  const combinedTestId = `bundle-${bundle.id}`;
  const combinedTest = {
    id: combinedTestId,
    title: bundle.title,
    description: bundle.description || `${totalQuestions} questions from ${bundle.items.length} sets`,
    durationMin: Math.max(totalDuration, 60),
    isExam: false,
    passScore: 0,
    examType: bundle.kind === "batch" ? "BATCH" : "QBANK",
    testCategory: bundle.kind === "batch" ? "batch" : "question_bank",
    textBlockCount: allItems.filter((x) => x.item.question.blockType === "text").length,
    audioBlockCount: allItems.filter((x) => x.item.question.blockType === "audio").length,
    textBlockEnabled: true,
    audioBlockEnabled: true,
    items: allItems.map((x, idx) => ({
      id: `${combinedTestId}-item-${idx}`,
      order: idx + 1,
      points: x.item.points,
      // The actual question ID — used by submit endpoint to look up the real question
      questionId: x.item.question.id,
      question: {
        id: x.item.question.id,
        type: x.item.question.type,
        difficulty: x.item.question.difficulty,
        stem: x.item.question.stem,
        options: x.item.question.options ? JSON.parse(x.item.question.options) : null,
        optionBlanks: x.item.question.optionBlanks ? JSON.parse(x.item.question.optionBlanks) : [],
        imageUrl: x.item.question.imageUrl || null,
        audioUrl: x.item.question.audioUrl || null,
        audioLoop: x.item.question.audioLoop || 0,
        audioLoopDelay: x.item.question.audioLoopDelay || 0,
        blockType: x.item.question.blockType || "text",
        blockNumber: idx + 1,
        setNumber: x.item.question.setNumber ?? 1,
        descType: x.item.question.descType || "none",
        descText: x.item.question.descText || null,
        descImageUrl: x.item.question.descImageUrl || null,
        descAudioUrl: x.item.question.descAudioUrl || null,
        mediaType: x.item.question.mediaType || "none",
        mediaText: x.item.question.mediaText || null,
        mediaImageUrl: x.item.question.mediaImageUrl || null,
        mediaAudioUrl: x.item.question.mediaAudioUrl || null,
        answerType: x.item.question.answerType || "text",
        optionImages: x.item.question.optionImages ? JSON.parse(x.item.question.optionImages) : [],
        optionAudios: x.item.question.optionAudios ? JSON.parse(x.item.question.optionAudios) : [],
        correctOption: x.item.question.correctOption ?? 0,
        explanation: x.item.question.explanation || null,
      },
    })),
  };

  return NextResponse.json({
    test: combinedTest,
    submissionId: `${combinedTestId}-session`,
    bundle: {
      id: bundle.id,
      title: bundle.title,
      kind: bundle.kind,
      totalSets: bundle.items.length,
      totalQuestions,
    },
  });
}
