import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

const schema = z.object({ showAllBlocks: z.boolean() });

export async function POST(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId } = await ctx.params;
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message ?? "Invalid data" }, { status: 400 });
  }
  const { showAllBlocks } = parsed.data;
  const test = await db.test.update({ where: { id: testId }, data: { showAllBlocks } });
  return NextResponse.json({ test });
}
