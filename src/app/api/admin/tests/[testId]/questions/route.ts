/**
 * POST /api/admin/tests/[testId]/questions
 * Creates a new question with block-based fields and links it to the test.
 *
 * Body:
 *   blockType: "text" | "audio"
 *   blockNumber: 1-40
 *   stem: string (question text)
 *   descType: "none" | "text" | "image" | "audio"
 *   descText?, descImageUrl?, descAudioUrl?
 *   mediaType: "none" | "text" | "image" | "audio"
 *   mediaText?, mediaImageUrl?, mediaAudioUrl?
 *   answerType: "text" | "image" | "audio" | "choose"
 *   options: string[] (for text/choose — 4 options)
 *   optionImages: string[] (for image — 4 image URLs)
 *   optionAudios: string[] (for audio — 4 audio URLs)
 *   correctOption: 0-3
 *   explanation?: string
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

const questionSchema = z.object({
  blockType: z.enum(["text", "audio"]),
  blockNumber: z.number().int().min(1).max(100),
  stem: z.string().min(1).max(2000),
  descType: z.enum(["none", "text", "image", "audio"]).default("none"),
  descText: z.string().optional().or(z.literal("")),
  descImageUrl: z.string().optional().or(z.literal("")),
  descAudioUrl: z.string().optional().or(z.literal("")),
  mediaType: z.enum(["none", "text", "image", "audio"]).default("none"),
  mediaText: z.string().optional().or(z.literal("")),
  mediaImageUrl: z.string().optional().or(z.literal("")),
  mediaAudioUrl: z.string().optional().or(z.literal("")),
  answerType: z.enum(["text", "image", "audio", "choose"]).default("text"),
  options: z.array(z.string()).optional().default([]),
  optionImages: z.array(z.string()).optional().default([]),
  optionAudios: z.array(z.string()).optional().default([]),
  correctOption: z.number().int().min(0).max(3).default(0),
  explanation: z.string().optional().or(z.literal("")),
});

export async function GET(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId } = await ctx.params;
  const items = await db.testItem.findMany({
    where: { testId },
    orderBy: { order: "asc" },
    include: { question: true },
  });
  return NextResponse.json({
    questions: items.map((i) => ({
      id: i.question.id,
      testItemId: i.id,
      blockType: i.question.blockType,
      blockNumber: i.question.blockNumber,
      stem: i.question.stem,
      descType: i.question.descType,
      descText: i.question.descText,
      descImageUrl: i.question.descImageUrl,
      descAudioUrl: i.question.descAudioUrl,
      mediaType: i.question.mediaType,
      mediaText: i.question.mediaText,
      mediaImageUrl: i.question.mediaImageUrl,
      mediaAudioUrl: i.question.mediaAudioUrl,
      answerType: i.question.answerType,
      options: i.question.options ? JSON.parse(i.question.options) : [],
      optionImages: i.question.optionImages ? JSON.parse(i.question.optionImages) : [],
      optionAudios: i.question.optionAudios ? JSON.parse(i.question.optionAudios) : [],
      correctOption: i.question.correctOption,
      explanation: i.question.explanation,
      points: i.points,
      order: i.order,
    })),
  });
}

export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId } = await ctx.params;
  const body = await req.json().catch(() => null);
  const parsed = questionSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid question" },
      { status: 400 },
    );
  }
  const d = parsed.data;

  try {
    // Map answerType to QuestionType enum
    const questionType =
      d.answerType === "choose" ? "SINGLE_CHOICE" :
      d.answerType === "text" ? "SINGLE_CHOICE" :
      d.answerType === "image" ? "SINGLE_CHOICE" :
      d.answerType === "audio" ? "SINGLE_CHOICE" : "SINGLE_CHOICE";

    // Build options array for storage
    let optionsJson: string | null = null;
    if (d.answerType === "text" || d.answerType === "choose") {
      optionsJson = JSON.stringify(d.options);
    }

    // Build correct answer
    let correctAnswer: string | null = null;
    if (d.answerType === "text" || d.answerType === "choose") {
      correctAnswer = JSON.stringify(d.options[d.correctOption] || "");
    } else if (d.answerType === "image") {
      correctAnswer = JSON.stringify(d.optionImages[d.correctOption] || "");
    } else if (d.answerType === "audio") {
      correctAnswer = JSON.stringify(d.optionAudios[d.correctOption] || "");
    }

    // Create the question
    const question = await db.question.create({
      data: {
        type: questionType as any,
        difficulty: "MEDIUM",
        stem: d.stem,
        options: optionsJson,
        correctAnswer,
        explanation: d.explanation || null,
        // Legacy fields for app compat
        imageUrl: d.mediaType === "image" ? d.mediaImageUrl : null,
        audioUrl: d.mediaType === "audio" ? d.mediaAudioUrl : null,
        audioLoop: d.blockType === "audio" ? 1 : 0,
        audioLoopDelay: 0,
        // New block-based fields
        blockType: d.blockType,
        blockNumber: d.blockNumber,
        descType: d.descType,
        descText: d.descText || null,
        descImageUrl: d.descImageUrl || null,
        descAudioUrl: d.descAudioUrl || null,
        mediaType: d.mediaType,
        mediaText: d.mediaText || null,
        mediaImageUrl: d.mediaImageUrl || null,
        mediaAudioUrl: d.mediaAudioUrl || null,
        answerType: d.answerType,
        optionImages: d.optionImages.length > 0 ? JSON.stringify(d.optionImages) : null,
        optionAudios: d.optionAudios.length > 0 ? JSON.stringify(d.optionAudios) : null,
        correctOption: d.correctOption,
      },
    });

    // Link to test
    const count = await db.testItem.count({ where: { testId } });
    const item = await db.testItem.create({
      data: {
        testId,
        questionId: question.id,
        points: 1,
        order: d.blockNumber, // use block number as order
      },
    });

    return NextResponse.json({ ok: true, question, item });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
