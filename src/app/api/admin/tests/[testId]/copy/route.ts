/**
 * POST /api/admin/tests/[testId]/copy
 *
 * Two modes:
 *
 * 1. Duplicate whole test:
 *    Body: { mode: "duplicate", targetCategory?: "exam"|"demo"|"batch"|"chapter"|"question_bank", newTitle?: string, newDescription?: string }
 *    Creates a NEW test in the target category (defaults to same as source)
 *    with a copy of ALL questions + test items. Returns the new test.
 *
 * 2. Copy a single set into an existing test:
 *    Body: { mode: "copySet", setNumber: number, targetTestId: string }
 *    Copies all questions from (source test, setNumber) into targetTestId,
 *    preserving block type/number. Useful for "Copy Set 1 from QBank into
 *    Batch Exam X".
 *
 * Both modes require ADMIN or TEACHER role. The original test is never
 * modified — we always create new question rows (duplicated) so the source
 * and destination are fully independent going forward.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import { z } from "zod";

const duplicateSchema = z.object({
  mode: z.literal("duplicate"),
  targetCategory: z.enum(["exam", "demo", "batch", "chapter", "question_bank"]).optional(),
  newTitle: z.string().trim().min(2).max(200).optional(),
  newDescription: z.string().trim().max(2000).optional(),
});

const copySetSchema = z.object({
  mode: z.literal("copySet"),
  setNumber: z.number().int().min(1).max(100),
  targetTestId: z.string().min(1),
});

export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId } = await ctx.params;

  const body = await req.json().catch(() => null);
  const parsed = z.union([duplicateSchema, copySetSchema]).safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message ?? "Invalid data" }, { status: 400 });
  }
  const d = parsed.data;
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  // Load source test + items + questions
  const sourceTest = await db.test.findUnique({
    where: { id: testId },
    include: {
      items: {
        orderBy: { order: "asc" },
        include: { question: true },
      },
    },
  });
  if (!sourceTest) return NextResponse.json({ error: "Source test not found" }, { status: 404 });

  if (d.mode === "duplicate") {
    // ─── Duplicate entire test + all questions ──────────────────────────────
    const targetCategory = d.targetCategory || sourceTest.testCategory;
    const newTitle = d.newTitle || `${sourceTest.title} (Copy)`;
    const newDescription = d.newDescription ?? sourceTest.description ?? "";

    // Create the new test
    const newTest = await db.test.create({
      data: {
        title: newTitle,
        description: newDescription,
        durationMin: sourceTest.durationMin,
        isExam: sourceTest.isExam,
        examType: sourceTest.examType,
        testCategory: targetCategory,
        passScore: sourceTest.passScore,
        isActive: true,
        isPublished: false, // Always start as draft — admin publishes when ready
        createdBy: user.id,
        price: sourceTest.price,
        featuredImage: sourceTest.featuredImage,
        category: sourceTest.category,
        audioPlayMode: sourceTest.audioPlayMode,
        audioGapSec: sourceTest.audioGapSec,
        textBlockCount: sourceTest.textBlockCount,
        audioBlockCount: sourceTest.audioBlockCount,
        textBlockEnabled: sourceTest.textBlockEnabled,
        audioBlockEnabled: sourceTest.audioBlockEnabled,
        showAllBlocks: sourceTest.showAllBlocks,
      },
    });

    // Copy all questions + test items
    let order = 1;
    for (const item of sourceTest.items) {
      const q = item.question;
      const newQ = await db.question.create({
        data: {
          type: q.type,
          difficulty: q.difficulty,
          stem: q.stem,
          options: q.options,
          correctAnswer: q.correctAnswer,
          explanation: q.explanation,
          imageUrl: q.imageUrl,
          audioUrl: q.audioUrl,
          audioLoop: q.audioLoop,
          audioLoopDelay: q.audioLoopDelay,
          blockType: q.blockType,
          blockNumber: q.blockNumber,
          setNumber: q.setNumber ?? 1,
          descType: q.descType,
          descText: q.descText,
          descImageUrl: q.descImageUrl,
          descAudioUrl: q.descAudioUrl,
          mediaType: q.mediaType,
          mediaText: q.mediaText,
          mediaImageUrl: q.mediaImageUrl,
          mediaAudioUrl: q.mediaAudioUrl,
          answerType: q.answerType,
          optionImages: q.optionImages,
          optionAudios: q.optionAudios,
          correctOption: q.correctOption,
          inQuestionBank: targetCategory === "question_bank",
          category: q.category,
        },
      });
      await db.testItem.create({
        data: {
          testId: newTest.id,
          questionId: newQ.id,
          points: item.points,
          order: order++,
        },
      });
    }

    await audit({
      actorId: user.id,
      action: "duplicate_test",
      entity: "Test",
      entityId: newTest.id,
      ip,
      metadata: { sourceTestId: testId, targetCategory, newTitle },
    });

    return NextResponse.json({ ok: true, test: newTest });
  }

  // ─── copySet mode ──────────────────────────────────────────────────────
  // Copy all questions from (source test, setNumber) into targetTestId
  const targetTest = await db.test.findUnique({
    where: { id: d.targetTestId },
    select: { id: true, testCategory: true },
  });
  if (!targetTest) return NextResponse.json({ error: "Target test not found" }, { status: 404 });

  // Filter source items by setNumber
  const setItems = sourceTest.items.filter(
    (i) => (i.question.setNumber ?? 1) === d.setNumber,
  );
  if (setItems.length === 0) {
    return NextResponse.json({ error: `No questions found in Set ${d.setNumber}` }, { status: 400 });
  }

  // Get the highest existing order in the target test so we append
  const maxOrder = await db.testItem.aggregate({
    where: { testId: targetTest.id },
    _max: { order: true },
  });
  let order = (maxOrder._max.order ?? 0) + 1;

  // Copy each question + link to target test
  for (const item of setItems) {
    const q = item.question;
    const newQ = await db.question.create({
      data: {
        type: q.type,
        difficulty: q.difficulty,
        stem: q.stem,
        options: q.options,
        correctAnswer: q.correctAnswer,
        explanation: q.explanation,
        imageUrl: q.imageUrl,
        audioUrl: q.audioUrl,
        audioLoop: q.audioLoop,
        audioLoopDelay: q.audioLoopDelay,
        blockType: q.blockType,
        blockNumber: q.blockNumber,
        setNumber: q.setNumber ?? 1,
        descType: q.descType,
        descText: q.descText,
        descImageUrl: q.descImageUrl,
        descAudioUrl: q.descAudioUrl,
        mediaType: q.mediaType,
        mediaText: q.mediaText,
        mediaImageUrl: q.mediaImageUrl,
        mediaAudioUrl: q.mediaAudioUrl,
        answerType: q.answerType,
        optionImages: q.optionImages,
        optionAudios: q.optionAudios,
        correctOption: q.correctOption,
        inQuestionBank: targetTest.testCategory === "question_bank",
        category: q.category,
      },
    });
    await db.testItem.create({
      data: {
        testId: targetTest.id,
        questionId: newQ.id,
        points: item.points,
        order: order++,
      },
    });
  }

  await audit({
    actorId: user.id,
    action: "copy_set",
    entity: "Test",
    entityId: targetTest.id,
    ip,
    metadata: { sourceTestId: testId, setNumber: d.setNumber, copiedCount: setItems.length },
  });

  return NextResponse.json({ ok: true, copiedCount: setItems.length });
}
