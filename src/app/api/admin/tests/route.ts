import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";
import { audit } from "@/lib/audit";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const tests = await db.test.findMany({
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { items: true, submissions: true } }, chapter: { select: { title: true } } },
  });
  return NextResponse.json({ tests });
}

const schema = z.object({
  title: z.string().trim().min(2).max(200),
  description: z.string().max(2000).optional(),
  chapterId: z.string().optional(),
  durationMin: z.number().int().min(5).max(300),
  passScore: z.number().int().min(0).max(100).default(40),
  isExam: z.boolean().default(false),
  isPublished: z.boolean().default(true),
  questionIds: z.array(z.string()).min(1).max(100),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });

  const { title, description, chapterId, durationMin, passScore, isExam, isPublished, questionIds } = parsed.data;

  const test = await db.test.create({
    data: {
      title, description: description || null,
      chapterId: chapterId || null,
      durationMin, passScore, isExam, isPublished,
      items: {
        create: questionIds.map((qid, i) => ({ questionId: qid, points: 1, order: i })),
      },
    },
    include: { items: true },
  });

  await audit({ actorId: user.id, action: "create_test", entity: "Test", entityId: test.id, metadata: { title, qCount: questionIds.length } });
  return NextResponse.json({ test });
}
