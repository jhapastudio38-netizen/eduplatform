import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import { z } from "zod";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const url = new URL(req.url);
  const category = url.searchParams.get("category"); // exam | demo | batch | chapter | question_bank
  const q = url.searchParams.get("q")?.trim() || ""; // free-text title search
  const where: Record<string, unknown> = {};
  if (
    category &&
    ["exam", "demo", "batch", "chapter", "question_bank"].includes(category)
  ) {
    where.testCategory = category;
  }
  if (q) {
    where.title = { contains: q, mode: "insensitive" };
  }
  const tests = await db.test.findMany({
    where,
    orderBy: { createdAt: "desc" },
    take: 200,
    include: { _count: { select: { items: true } } },
  });
  return NextResponse.json({ tests });
}

const testSchema = z.object({
  title: z.string().min(2).max(200),
  description: z.string().max(2000).optional().default(""),
  durationMin: z.number().int().min(1).max(600).default(30),
  isExam: z.boolean().default(true),
  examType: z.string().default("UBT"),
  testCategory: z.enum(["exam", "demo", "batch", "chapter", "question_bank"]).default("exam"),
  passScore: z.number().int().min(0).max(100).default(40),
  isPublished: z.boolean().default(false),
  // New fields
  price: z.number().min(0).optional(),
  featuredImage: z.string().optional().or(z.literal("")),
  category: z.string().max(100).optional().or(z.literal("")),
  audioPlayMode: z.enum(["single", "double"]).default("single"),
  audioGapSec: z.number().int().min(0).max(60).default(2),
  textBlockCount: z.number().int().min(0).max(100).default(20),
  audioBlockCount: z.number().int().min(0).max(100).default(20),
  // Per-block enable/disable — admin can hide the audio or text section.
  textBlockEnabled: z.boolean().optional().default(true),
  audioBlockEnabled: z.boolean().optional().default(true),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = testSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }
  const d = parsed.data;
  try {
    const test = await db.test.create({
      data: {
        title: d.title,
        description: d.description,
        durationMin: d.durationMin,
        isExam: d.isExam,
        examType: d.examType,
        testCategory: d.testCategory,
        passScore: d.passScore,
        isPublished: d.isPublished,
        isActive: true,
        createdBy: user.id,
        price: d.price ?? null,
        featuredImage: d.featuredImage || null,
        category: d.category || null,
        audioPlayMode: d.audioPlayMode,
        audioGapSec: d.audioGapSec,
        textBlockCount: d.textBlockCount,
        audioBlockCount: d.audioBlockCount,
        textBlockEnabled: d.textBlockEnabled ?? true,
        audioBlockEnabled: d.audioBlockEnabled ?? true,
      },
    });
    await audit({ actorId: user.id, action: "create_test", entity: "Test", entityId: test.id });
    return NextResponse.json({ test });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
