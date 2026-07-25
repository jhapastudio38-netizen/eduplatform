/**
 * DELETE /api/admin/eye-vision/[testId]
 * PUT  /api/admin/eye-vision/[testId]
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function DELETE(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId } = await ctx.params;
  try {
    await db.eyeVisionTest.delete({ where: { id: testId } });
    return NextResponse.json({ ok: true });
  } catch {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }
}

export async function PUT(req: NextRequest, ctx: { params: Promise<{ testId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { testId } = await ctx.params;
  const body = await req.json().catch(() => ({}));
  try {
    const updated = await db.eyeVisionTest.update({
      where: { id: testId },
      data: {
        title: body.title,
        description: body.description || null,
        imageUrl: body.imageUrl,
        correctAnswer: body.correctAnswer,
        category: body.category || null,
        isPublished: body.isPublished ?? true,
      },
    });
    return NextResponse.json({ test: updated });
  } catch (e: any) {
    return NextResponse.json({ error: e.message?.substring(0, 200) }, { status: 500 });
  }
}
