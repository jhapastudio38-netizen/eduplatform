/**
 * POST /api/admin/tests/[testId]/toggle-block
 * Body: { block: "text" | "audio", enabled: boolean }
 *
 * Toggles the visibility of the text or audio block on a test. When disabled,
 * the student app filters those questions out entirely — they don't count
 * toward the question count, timer, or score.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

const schema = z.object({
  block: z.enum(["text", "audio"]),
  enabled: z.boolean(),
});

export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId } = await ctx.params;
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid data" },
      { status: 400 },
    );
  }
  const { block, enabled } = parsed.data;
  const data = block === "text"
    ? { textBlockEnabled: enabled }
    : { audioBlockEnabled: enabled };
  const test = await db.test.update({ where: { id: testId }, data });
  return NextResponse.json({ test });
}
