import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

/**
 * GET /api/student/tests/[testId]
 * Returns the test with questions & options (but NEVER the correct answers).
 * Crash-safe: wraps each field access in try-catch so missing DB columns
 * don't crash the entire API.
 */
export async function GET(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  try {
    // Use include: { question: true } to get ALL columns
    const test = await db.test.findUnique({
      where: { id: testId },
      include: {
        items: {
          orderBy: { order: "asc" },
          include: { question: true },
        },
      },
    });

    if (!test) return NextResponse.json({ error: "Not found" }, { status: 404 });

    // Safe accessor — returns default if field doesn't exist or is null
    const safe = (val: any, def: any = null) => (val === null || val === undefined ? def : val);
    const safeJson = (val: any, def: any[] = []) => {
      if (!val) return def;
      try { return JSON.parse(val); } catch { return def; }
    };

    // Filter disabled blocks
    const textEnabled = test.textBlockEnabled !== false;
    const audioEnabled = test.audioBlockEnabled !== false;
    const filteredItems = test.items.filter((i) => {
      const bt = (i.question as any).blockType || "text";
      if (bt === "text" && !textEnabled) return false;
      if (bt === "audio" && !audioEnabled) return false;
      return true;
    });

    // Create or fetch a draft submission
    const draft = await db.submission.upsert({
      where: { testId_userId: { testId, userId: user.id } },
      create: { testId, userId: user.id, answers: "{}", maxScore: filteredItems.reduce((s, i) => s + i.points, 0) },
      update: {},
    });

    const res = NextResponse.json({
      test: {
        id: test.id,
        title: test.title,
        description: test.description,
        durationMin: test.durationMin,
        isExam: test.isExam,
        passScore: test.passScore,
        textBlockCount: safe(test.textBlockCount, 0),
        audioBlockCount: safe(test.audioBlockCount, 0),
        textBlockEnabled,
        audioBlockEnabled,
        showAllBlocks: test.showAllBlocks !== false,
        items: filteredItems.map((i) => {
          const q = i.question as any;
          return {
            id: i.id,
            order: i.order,
            points: i.points,
            question: {
              id: q.id,
              type: q.type,
              difficulty: q.difficulty,
              stem: safe(q.stem, ""),
              title: safe(q.title, ""),
              isFree: safe(q.isFree, false),
              options: safeJson(q.options, null),
              optionBlanks: safeJson(q.optionBlanks, []),
              imageUrl: safe(q.imageUrl),
              audioUrl: safe(q.audioUrl),
              audioLoop: safe(q.audioLoop, 0),
              audioLoopDelay: safe(q.audioLoopDelay, 0),
              blockType: safe(q.blockType, "text"),
              blockNumber: safe(q.blockNumber, 0),
              descType: safe(q.descType, "none"),
              descText: safe(q.descText),
              descImageUrl: safe(q.descImageUrl),
              descAudioUrl: safe(q.descAudioUrl),
              mediaType: safe(q.mediaType, "none"),
              mediaText: safe(q.mediaText),
              mediaImageUrl: safe(q.mediaImageUrl),
              mediaAudioUrl: safe(q.mediaAudioUrl),
              answerType: safe(q.answerType, "text"),
              optionImages: safeJson(q.optionImages, []),
              optionAudios: safeJson(q.optionAudios, []),
              correctOption: safe(q.correctOption, 0),
              explanation: safe(q.explanation),
            },
          };
        }),
      },
      submissionId: draft.id,
    });
    res.headers.set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    return res;
  } catch (e: any) {
    console.error("Student test API error:", e);
    return NextResponse.json(
      { error: `Failed to load test: ${e.message?.substring(0, 100)}` },
      { status: 500 }
    );
  }
}
