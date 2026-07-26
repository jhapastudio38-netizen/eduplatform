/**
 * GET    /api/admin/bundles/[bundleId]   — fetch bundle + items (+ tests)
 * PATCH  /api/admin/bundles/[bundleId]   — update fields
 * DELETE /api/admin/bundles/[bundleId]   — delete bundle (cascades items)
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET(req: NextRequest, ctx: { params: Promise<{ bundleId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { bundleId } = await ctx.params;
  const bundle = await db.questionBundle.findUnique({
    where: { id: bundleId },
    include: {
      items: {
        orderBy: { sortOrder: "asc" },
        include: {
          test: {
            select: {
              id: true,
              title: true,
              testCategory: true,
              examType: true,
              durationMin: true,
              _count: { select: { items: true } },
            },
          },
        },
      },
    },
  });
  if (!bundle) return NextResponse.json({ error: "Not found" }, { status: 404 });
  return NextResponse.json({ bundle });
}

const patchSchema = z.object({
  title: z.string().trim().min(2).max(200).optional(),
  description: z.string().trim().max(2000).optional().or(z.literal("")),
  kind: z.enum(["qbank", "batch", "exam", "chapter"]).optional(),
  // Accept relative paths (/api/files/...) OR absolute URLs OR null (to clear)
  coverUrl: z
    .string()
    .max(2000)
    .optional()
    .or(z.literal(""))
    .or(z.null())
    .refine(
      (v) => v === null || v === "" || v === undefined || v.startsWith("/") || /^https?:\/\//.test(v),
      "Must be a relative path (/...) or absolute URL (https://...)",
    ),
  price: z.number().int().min(0).max(10_000_000).optional(),
  isPublished: z.boolean().optional(),
  batchId: z.string().optional().or(z.literal("")).or(z.null()),
});

export async function PATCH(req: NextRequest, ctx: { params: Promise<{ bundleId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { bundleId } = await ctx.params;
  const body = await req.json().catch(() => null);
  const parsed = patchSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid data" },
      { status: 400 },
    );
  }
  const d = parsed.data;
  const data: Record<string, unknown> = {};
  if (d.title !== undefined) data.title = d.title;
  if (d.description !== undefined) data.description = d.description || null;
  if (d.kind !== undefined) data.kind = d.kind;
  if (d.coverUrl !== undefined) data.coverUrl = d.coverUrl || null;
  if (d.price !== undefined) data.price = d.price;
  if (d.isPublished !== undefined) data.isPublished = d.isPublished;
  if (d.batchId !== undefined) data.batchId = d.batchId || null;

  const updated = await db.questionBundle.update({
    where: { id: bundleId },
    data,
  });
  return NextResponse.json({ bundle: updated });
}

export async function DELETE(req: NextRequest, ctx: { params: Promise<{ bundleId: string }> }) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const { bundleId } = await ctx.params;
  await db.questionBundle.delete({ where: { id: bundleId } });
  return NextResponse.json({ ok: true });
}
