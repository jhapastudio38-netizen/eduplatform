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
          question: {
            select: {
              id: true, type: true, difficulty: true, stem: true,
              options: true,
              // Legacy fields (for backward compat with old app versions)
              imageUrl: true, audioUrl: true,
              audioLoop: true, audioLoopDelay: true,
              // New block-based fields
              blockType: true, blockNumber: true,
              descType: true, descText: true, descImageUrl: true, descAudioUrl: true,
              mediaType: true, mediaText: true, mediaImageUrl: true, mediaAudioUrl: true,
              answerType: true, optionImages: true, optionAudios: true, correctOption: true,
              explanation: true,
            },
          },
        },
      },
    },
  });
  if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

  // NOTE: We intentionally do NOT block on isActive / startAt / endAt here.
  // Previous strict checks caused "can't load" errors for students whenever
  // an admin-created test had an empty endAt or isActive=false by default.
  // Published tests are always openable; admins control visibility via isPublished.

  // ─── Filter out disabled blocks ────────────────────────────────────────────
  // Admin can disable the text or audio block of an exam (textBlockEnabled /
  // audioBlockEnabled). When disabled, we drop those items entirely so the
  // student never sees them — they don't count toward the question count, the
  // timer, or the score.
  const textEnabled = test.textBlockEnabled !== false; // default true if null
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

  return NextResponse.json({
    test: {
      id: test.id, title: test.title, description: test.description,
      durationMin: test.durationMin, isExam: test.isExam, passScore: test.passScore,
      // Expose block flags + counts so the app can show "X text + Y audio"
      // on the pre-exam info screen.
      textBlockCount: test.textBlockCount ?? 0,
      audioBlockCount: test.audioBlockCount ?? 0,
      textBlockEnabled: textEnabled,
      audioBlockEnabled: audioEnabled,
      items: filteredItems.map((i) => ({
        id: i.id,
        order: i.order,
        points: i.points,
        question: {
          id: i.question.id, type: i.question.type, difficulty: i.question.difficulty,
          stem: i.question.stem,
          options: i.question.options ? JSON.parse(i.question.options) : null,
          optionBlanks: i.question.optionBlanks ? JSON.parse(i.question.optionBlanks) : [],
          // Legacy fields
          imageUrl: i.question.imageUrl || null,
          audioUrl: i.question.audioUrl || null,
          audioLoop: i.question.audioLoop || 0,
          audioLoopDelay: i.question.audioLoopDelay || 0,
          // New block-based fields
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
}
