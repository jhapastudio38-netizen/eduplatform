import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET(_req: NextRequest, ctx: { params: Promise<{ questionId: string }> }) {
  const { questionId } = await ctx.params;
  const question = await db.qAQuestion.findUnique({
    where: { id: questionId },
    include: { answers: { orderBy: { createdAt: "asc" } } },
  });
  if (!question) return NextResponse.json({ error: "Not found" }, { status: 404 });
  return NextResponse.json({
    question: {
      ...question,
      tags: question.tags ? JSON.parse(question.tags) : [],
    },
  });
}

const ansSchema = z.object({ body: z.string().trim().min(2).max(5000) });

export async function POST(req: NextRequest, ctx: { params: Promise<{ questionId: string }> }) {
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  const { questionId } = await ctx.params;

  const body = await req.json().catch(() => null);
  const parsed = ansSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }

  const answer = await db.qAAnswer.create({
    data: { questionId, authorId: user.id, body: parsed.data.body },
  });
  return NextResponse.json({ answer });
}
