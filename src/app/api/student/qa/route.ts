import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET() {
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const questions = await db.qAQuestion.findMany({
    where: { userId: user.id },
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { answers: true } } },
  });
  return NextResponse.json({
    questions: questions.map((q) => ({
      ...q,
      tags: q.tags ? JSON.parse(q.tags) : [],
      answers: Array(q._count.answers).fill(null), // count only on listing
    })),
  });
}

const qaSchema = z.object({
  title: z.string().trim().min(5).max(200),
  body: z.string().trim().min(5).max(5000),
  tags: z.array(z.string().min(1).max(40)).max(10).default([]),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const body = await req.json().catch(() => null);
  const parsed = qaSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }

  const q = await db.qAQuestion.create({
    data: {
      userId: user.id,
      title: parsed.data.title,
      body: parsed.data.body,
      tags: JSON.stringify(parsed.data.tags),
    },
  });
  return NextResponse.json({ question: { ...q, tags: parsed.data.tags } });
}
