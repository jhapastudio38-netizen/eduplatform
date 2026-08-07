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
import { QuestionType as QType } from "@prisma/client";

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
  audioLoop: z.number().int().min(0).max(100).default(2),
  audioLoopDelay: z.number().int().min(0).max(60).default(0),
  title: z.string().optional().or(z.literal("")),
  isFree: z.boolean().optional().default(false),
  optionBlanks: z.array(z.string()).optional().default([]),
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
      title: i.question.title || "",
      isFree: i.question.isFree ?? false,
      audioLoop: i.question.audioLoop ?? 2,
      audioLoopDelay: i.question.audioLoopDelay ?? 0,
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
      optionBlanks: i.question.optionBlanks ? JSON.parse(i.question.optionBlanks) : [],
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
    const questionType = QType.SINGLE_CHOICE;

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

    // Check if a question already exists for this block (same testId + blockType + blockNumber)
    const existingItem = await db.testItem.findFirst({
      where: {
        testId,
        question: {
          blockType: d.blockType,
          blockNumber: d.blockNumber,
        },
      },
      include: { question: true },
    });

    let question;
    if (existingItem) {
      // UPDATE existing question
      question = await db.question.update({
        where: { id: existingItem.questionId },
        data: {
          type: questionType,
          stem: d.stem,
          title: d.title || null,
          isFree: d.isFree ?? false,
          options: optionsJson,
          correctAnswer,
          explanation: d.explanation || null,
          imageUrl: d.mediaType === "image" ? d.mediaImageUrl : null,
          audioUrl: d.mediaType === "audio" ? d.mediaAudioUrl : null,
          audioLoop: d.audioLoop ?? 2,
          audioLoopDelay: d.audioLoopDelay ?? 0,
          descType: d.descType,
          descText: d.descText || null,
          descImageUrl: d.descImageUrl || null,
          descAudioUrl: d.descAudioUrl || null,
          mediaType: d.mediaType,
          mediaText: d.mediaText || null,
          mediaImageUrl: d.mediaImageUrl || null,
          mediaAudioUrl: d.mediaAudioUrl || null,
          answerType: d.answerType,
          optionImages: d.optionImages.filter((u: string) => u && u.trim()).length > 0 ? JSON.stringify(d.optionImages) : null,
          optionAudios: d.optionAudios.filter((u: string) => u && u.trim()).length > 0 ? JSON.stringify(d.optionAudios) : null,
          optionBlanks: d.optionBlanks && d.optionBlanks.length > 0 ? JSON.stringify(d.optionBlanks) : null,
          correctOption: d.correctOption,
        },
      });
    } else {
      // CREATE new question + link to test
      question = await db.question.create({
        data: {
          type: questionType,
          difficulty: "MEDIUM",
          stem: d.stem,
          title: d.title || null,
          isFree: d.isFree ?? false,
          options: optionsJson,
          correctAnswer,
          explanation: d.explanation || null,
          imageUrl: d.mediaType === "image" ? d.mediaImageUrl : null,
          audioUrl: d.mediaType === "audio" ? d.mediaAudioUrl : null,
          audioLoop: d.audioLoop ?? 2,
          audioLoopDelay: d.audioLoopDelay ?? 0,
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
          optionImages: d.optionImages.filter((u: string) => u && u.trim()).length > 0 ? JSON.stringify(d.optionImages) : null,
          optionAudios: d.optionAudios.filter((u: string) => u && u.trim()).length > 0 ? JSON.stringify(d.optionAudios) : null,
          optionBlanks: d.optionBlanks && d.optionBlanks.length > 0 ? JSON.stringify(d.optionBlanks) : null,
          correctOption: d.correctOption,
        },
      });

      // Link to test
      await db.testItem.create({
        data: {
          testId,
          questionId: question.id,
          points: 1,
          order: d.blockNumber,
        },
      });
    }

    return NextResponse.json({ ok: true, question });
  } catch (e: any) {
    console.error("Question save error:", e);
    return NextResponse.json({ error: e.message?.substring(0, 300) }, { status: 500 });
  }
}
