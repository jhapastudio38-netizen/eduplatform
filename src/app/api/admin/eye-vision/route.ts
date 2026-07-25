/**
 * GET  /api/admin/eye-vision — list all eye vision tests
 * POST /api/admin/eye-vision — create a new eye vision test
 *
 * Body: { title, description?, imageUrl, correctAnswer, category? }
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const tests = await db.eyeVisionTest.findMany({
    orderBy: [{ sortOrder: "asc" }, { createdAt: "desc" }],
  });
  return NextResponse.json({ tests });
}

const schema = z.object({
  title: z.string().min(2).max(200),
  description: z.string().max(2000).optional().or(z.literal("")),
  imageUrl: z.string().min(1, "Image is required"),
  correctAnswer: z.string().min(1, "Correct answer is required").max(200),
  category: z.string().max(100).optional().or(z.literal("")),
  sortOrder: z.number().int().default(0),
  isPublished: z.boolean().default(true),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid data" },
      { status: 400 },
    );
  }
  const d = parsed.data;
  try {
    const test = await db.eyeVisionTest.create({
      data: {
        title: d.title,
        description: d.description || null,
        imageUrl: d.imageUrl,
        correctAnswer: d.correctAnswer,
        category: d.category || null,
        sortOrder: d.sortOrder,
        isPublished: d.isPublished,
        createdBy: user.id,
      },
    });
    return NextResponse.json({ test });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
