import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function DELETE(_req: NextRequest, ctx: { params: Promise<{ bookId: string }> }) {
  const { bookId } = await ctx.params;
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  await db.book.delete({ where: { id: bookId } }).catch(() => {});
  return NextResponse.json({ ok: true });
}
