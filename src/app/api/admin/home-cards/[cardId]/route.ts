/**
 * PUT /api/admin/home-cards/[cardId] — update a home card
 * DELETE /api/admin/home-cards/[cardId] — delete a home card
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import { z } from "zod";

const updateSchema = z.object({
  title: z.string().trim().min(2).max(100).optional(),
  section: z.enum(["test", "resources", "premium"]).optional(),
  imageUrl: z.string().url().optional().or(z.literal("")).or(z.null()),
  sortOrder: z.number().int().min(0).max(100).optional(),
  isActive: z.boolean().optional(),
  route: z.string().trim().max(50).optional().or(z.literal("")),
});

export async function PUT(req: NextRequest, ctx: { params: Promise<{ cardId: string }> }) {
  const { cardId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = updateSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  }
  const data: Record<string, unknown> = { ...parsed.data };
  if (data.imageUrl === "" || data.imageUrl === null) data.imageUrl = null;
  const card = await db.homeCard.update({ where: { id: cardId }, data });
  await audit({ actorId: user.id, action: "update_home_card", entity: "HomeCard", entityId: cardId });
  return NextResponse.json({ card });
}

export async function DELETE(_req: NextRequest, ctx: { params: Promise<{ cardId: string }> }) {
  const { cardId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  await db.homeCard.delete({ where: { id: cardId } });
  await audit({ actorId: user.id, action: "delete_home_card", entity: "HomeCard", entityId: cardId });
  return NextResponse.json({ ok: true });
}
