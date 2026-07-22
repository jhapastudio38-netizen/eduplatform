import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const batches = await db.batch.findMany({
    orderBy: { createdAt: "desc" },
    include: {
      _count: { select: { students: true, exams: true } },
    },
  });
  return NextResponse.json({ batches });
}

const schema = z.object({
  name: z.string().trim().min(2).max(200),
  slug: z.string().trim().min(2).max(120).regex(/^[a-z0-9-]+$/),
  description: z.string().max(2000).optional(),
});

export async function POST(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: parsed.error.issues[0]?.message }, { status: 400 });
  try {
    const batch = await db.batch.create({ data: { ...parsed.data, teacherId: user.id } });
    return NextResponse.json({ batch });
  } catch {
    return NextResponse.json({ error: "Slug already exists" }, { status: 409 });
  }
}
