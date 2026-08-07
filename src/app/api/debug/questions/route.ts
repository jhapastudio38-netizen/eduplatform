import { NextResponse } from "next/server";
import { db } from "@/lib/db";
export async function GET() {
  try {
    const questions = await db.$queryRaw`
      SELECT id, SUBSTRING(stem, 1, 40) as stem, "descType", "descImageUrl",
             "mediaType", "mediaImageUrl", "imageUrl", "audioUrl", "mediaAudioUrl",
             "answerType", "optionImages", "optionAudios",
             "audioLoop", "audioLoopDelay", "blockType", "blockNumber"
      FROM "Question"
      WHERE "descImageUrl" IS NOT NULL OR "mediaImageUrl" IS NOT NULL
         OR "imageUrl" IS NOT NULL OR "optionImages" IS NOT NULL
         OR "audioUrl" IS NOT NULL OR "mediaAudioUrl" IS NOT NULL
      LIMIT 10
    ` as any[];
    return NextResponse.json({ count: questions.length, questions });
  } catch (e: any) {
    return NextResponse.json({ error: e.message }, { status: 500 });
  }
}
