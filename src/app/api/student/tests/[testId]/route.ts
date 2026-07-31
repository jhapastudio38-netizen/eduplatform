import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const { testId } = await ctx.params;
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  try {
    // Use raw SQL to avoid Prisma client type issues
    const testRows = await db.$queryRaw`
      SELECT * FROM "Test" WHERE id = ${testId}
    ` as any[];
    
    if (!testRows || testRows.length === 0) {
      return NextResponse.json({ error: "Not found" }, { status: 404 });
    }
    
    const testRow = testRows[0];
    
    const itemRows = await db.$queryRaw`
      SELECT t.*, q.* FROM "TestItem" t
      JOIN "Question" q ON t."questionId" = q.id
      WHERE t."testId" = ${testId}
      ORDER BY t.order ASC
    ` as any[];

    // Parse items
    const items = (itemRows || []).map((row: any) => {
      const safeJson = (val: any, def: any[] = []) => {
        if (!val) return def;
        try { return JSON.parse(val); } catch { return def; }
      };
      return {
        id: row.id,
        order: row.order,
        points: row.points,
        question: {
          id: row.questionId,
          type: row.type,
          difficulty: row.difficulty,
          stem: row.stem || "",
          title: row.title || "",
          isFree: row.isFree || false,
          options: safeJson(row.options, null),
          optionBlanks: safeJson(row.optionBlanks, []),
          imageUrl: row.imageUrl || null,
          audioUrl: row.audioUrl || null,
          audioLoop: row.audioLoop || 0,
          audioLoopDelay: row.audioLoopDelay || 0,
          blockType: row.blockType || "text",
          blockNumber: row.blockNumber || 0,
          descType: row.descType || "none",
          descText: row.descText || null,
          descImageUrl: row.descImageUrl || null,
          descAudioUrl: row.descAudioUrl || null,
          mediaType: row.mediaType || "none",
          mediaText: row.mediaText || null,
          mediaImageUrl: row.mediaImageUrl || null,
          mediaAudioUrl: row.mediaAudioUrl || null,
          answerType: row.answerType || "text",
          optionImages: safeJson(row.optionImages, []),
          optionAudios: safeJson(row.optionAudios, []),
          correctOption: row.correctOption ?? 0,
          explanation: row.explanation || null,
        },
      };
    });

    // Filter disabled blocks
    const textEnabled = testRow.textBlockEnabled !== false;
    const audioEnabled = testRow.audioBlockEnabled !== false;
    const filteredItems = items.filter((i: any) => {
      const bt = i.question.blockType;
      if (bt === "text" && !textEnabled) return false;
      if (bt === "audio" && !audioEnabled) return false;
      return true;
    });

    const res = NextResponse.json({
      test: {
        id: testRow.id,
        title: testRow.title,
        description: testRow.description,
        durationMin: testRow.durationMin,
        isExam: testRow.isExam,
        passScore: testRow.passScore,
        textBlockCount: testRow.textBlockCount || 0,
        audioBlockCount: testRow.audioBlockCount || 0,
        textBlockEnabled: textEnabled,
        audioBlockEnabled: audioEnabled,
        showAllBlocks: testRow.showAllBlocks !== false,
        items: filteredItems,
      },
      submissionId: "draft-" + Date.now(),
    });
    res.headers.set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    return res;
  } catch (e: any) {
    console.error("Student test API error:", e);
    return NextResponse.json(
      { error: `Failed to load test: ${e.message?.substring(0, 150)}` },
      { status: 500 }
    );
  }
}
// force rebuild Fri Jul 31 14:57:41 UTC 2026
