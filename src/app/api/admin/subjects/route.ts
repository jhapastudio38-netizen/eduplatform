import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";
import { audit } from "@/lib/audit";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const subjects = await db.subject.findMany({ orderBy: { name: "asc" } });
  return NextResponse.json({ subjects });
}

const schema = z.object({
  name: z.string().trim().min(2).max(100),
  slug: z.string().trim().min(2).max(120).regex(/^[a-z0-9-]+$/),
  description: z.string().trim().max(2000).optional(),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  try {
    const s = await db.subject.create({ data: parsed.data });
    await audit({ actorId: user.id, action: "create_subject", entity: "Subject", entityId: s.id, metadata: { name: s.name } });
    return NextResponse.json({ subject: s });
  } catch (e) {
    return NextResponse.json({ error: "Slug already exists" }, { status: 409 });
  }
}
