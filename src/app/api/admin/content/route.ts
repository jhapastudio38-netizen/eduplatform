/**
 * GET  /api/admin/content?type=grammar|vocabulary
 * POST /api/admin/content  — create new content entry
 *
 * Admin/teacher can write grammar lessons and vocabulary entries.
 * These are stored in the database and shown in the app's Grammar tool.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const url = new URL(req.url);
  const type = url.searchParams.get("type") || "grammar";
  const entries = await db.question.findMany({
    where: { inQuestionBank: true, category: type },
    orderBy: { createdAt: "desc" },
    take: 200,
  });
  return NextResponse.json({
    entries: entries.map((e) => ({
      id: e.id,
      title: e.title || "",
      stem: e.stem,
      explanation: e.explanation,
      category: e.category,
    })),
  });
}

const schema = z.object({
  type: z.enum(["grammar", "vocabulary"]),
  title: z.string().max(200).optional().or(z.literal("")),
  content: z.string().min(1).max(5000),
  explanation: z.string().max(2000).optional().or(z.literal("")),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }
  const d = parsed.data;
  const entry = await db.question.create({
    data: {
      type: "SHORT_ANSWER",
      difficulty: "EASY",
      stem: d.content,
      title: d.title || null,
      explanation: d.explanation || null,
      inQuestionBank: true,
      category: d.type,
    },
  });
  return NextResponse.json({ ok: true, id: entry.id });
}
