import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

/**
 * GET /api/student/tests/[testId]
 * Returns the test with questions & options (but NEVER the correct answers).
 */
export async function GET(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const test = await db.test.findUnique({
    where: { id: testId },
    include: {
      items: {
        orderBy: { order: "asc" },
        include: {
          question: true, // include ALL fields
        },
      },
    },
  });
  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

  // ─── Filter out disabled blocks ────────────────────────────────────────────
  const textEnabled = test.textBlockEnabled !== false;
  const audioEnabled = test.audioBlockEnabled !== false;
  const filteredItems = test.items.filter((i) => {
    const bt = i.question.blockType || "text";
    if (bt === "text" && !textEnabled) return false;
    if (bt === "audio" && !audioEnabled) return false;
    return true;
  });

  // Create or fetch a draft submission so the timer starts now
  const draft = await db.submission.upsert({
    where: { testId_userId: { testId, userId: user.id } },
    create: { testId, userId: user.id, answers: "{}", maxScore: filteredItems.reduce((s, i) => s + i.points, 0) },
    update: {},
  });

  // No caching — students always see real-time data
  const res = NextResponse.json({
    test: {
      id: test.id, title: test.title, description: test.description,
      durationMin: test.durationMin, isExam: test.isExam, passScore: test.passScore,
      textBlockCount: test.textBlockCount ?? 0,
      audioBlockCount: test.audioBlockCount ?? 0,
      textBlockEnabled: textEnabled,
      audioBlockEnabled: audioEnabled,
      showAllBlocks: test.showAllBlocks !== false,
      items: filteredItems.map((i) => ({
        id: i.id,
        order: i.order,
        points: i.points,
        question: {
          id: i.question.id, type: i.question.type, difficulty: i.question.difficulty,
          stem: i.question.stem || "",
          title: i.question.title || "",
          isFree: i.question.isFree || false,
          options: i.question.options ? JSON.parse(i.question.options) : null,
          optionBlanks: i.question.optionBlanks ? JSON.parse(i.question.optionBlanks) : [],
          // Legacy fields
          imageUrl: i.question.imageUrl || null,
          audioUrl: i.question.audioUrl || null,
          audioLoop: i.question.audioLoop || 0,
          audioLoopDelay: i.question.audioLoopDelay || 0,
          // Block-based fields
          blockType: i.question.blockType || "text",
          blockNumber: i.question.blockNumber || 0,
          descType: i.question.descType || "none",
          descText: i.question.descText || null,
          descImageUrl: i.question.descImageUrl || null,
          descAudioUrl: i.question.descAudioUrl || null,
          mediaType: i.question.mediaType || "none",
          mediaText: i.question.mediaText || null,
          mediaImageUrl: i.question.mediaImageUrl || null,
          mediaAudioUrl: i.question.mediaAudioUrl || null,
          answerType: i.question.answerType || "text",
          optionImages: i.question.optionImages ? JSON.parse(i.question.optionImages) : [],
          optionAudios: i.question.optionAudios ? JSON.parse(i.question.optionAudios) : [],
          correctOption: i.question.correctOption ?? 0,
          explanation: i.question.explanation || null,
        },
      })),
    },
    submissionId: draft.id,
  });
  res.headers.set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
  return res;
}
