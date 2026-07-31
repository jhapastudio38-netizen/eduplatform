import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const results: string[] = [];

  const testColumns = [
    { name: "showAllBlocks", type: "BOOLEAN DEFAULT true" },
    { name: "textBlockEnabled", type: "BOOLEAN DEFAULT true" },
    { name: "audioBlockEnabled", type: "BOOLEAN DEFAULT true" },
  ];
  for (const col of testColumns) {
    try {
      await db.$executeRawUnsafe(`ALTER TABLE "Test" ADD COLUMN IF NOT EXISTS "${col.name}" ${col.type}`);
      results.push(`Test.${col.name} — OK`);
    } catch (e: any) {
      results.push(`Test.${col.name} — ${e.message?.substring(0, 80)}`);
    }
  }

  const questionColumns = [
    { name: "title", type: "TEXT" },
    { name: "isFree", type: "BOOLEAN DEFAULT false" },
    { name: "blockType", type: "TEXT" },
    { name: "blockNumber", type: "INTEGER" },
    { name: "descType", type: "TEXT DEFAULT 'none'" },
    { name: "descText", type: "TEXT" },
    { name: "descImageUrl", type: "TEXT" },
    { name: "descAudioUrl", type: "TEXT" },
    { name: "mediaType", type: "TEXT DEFAULT 'none'" },
    { name: "mediaText", type: "TEXT" },
    { name: "mediaImageUrl", type: "TEXT" },
    { name: "mediaAudioUrl", type: "TEXT" },
    { name: "answerType", type: "TEXT DEFAULT 'text'" },
    { name: "optionImages", type: "TEXT" },
    { name: "optionAudios", type: "TEXT" },
    { name: "optionBlanks", type: "TEXT" },
    { name: "correctOption", type: "INTEGER DEFAULT 0" },
  ];
  for (const col of questionColumns) {
    try {
      await db.$executeRawUnsafe(`ALTER TABLE "Question" ADD COLUMN IF NOT EXISTS "${col.name}" ${col.type}`);
      results.push(`Question.${col.name} — OK`);
    } catch (e: any) {
      results.push(`Question.${col.name} — ${e.message?.substring(0, 80)}`);
    }
  }

  return NextResponse.json({ ok: true, results });
}
