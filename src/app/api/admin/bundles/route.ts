/**
 * GET  /api/admin/bundles            — list bundles (filter by ?kind=qbank|batch|exam|chapter)
 * POST /api/admin/bundles            — create a bundle
 *   Body: { title, slug?, description?, kind?, coverUrl?, price?, batchId? }
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { z } from "zod";
import { randomBytes } from "crypto";

function slugify(input: string): string {
  return input
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .slice(0, 80);
}

async function uniqueSlug(base: string): Promise<string> {
  let slug = base || "bundle";
  let n = 1;
  while (await db.questionBundle.findUnique({ where: { slug } })) {
    slug = `${base}-${++n}`;
    if (slug.length > 90) {
      // Fallback to a random suffix if we've appended too many numbers
      slug = `${base.slice(0, 60)}-${randomBytes(3).toString("hex")}`;
      break;
    }
  }
  return slug;
}

const createSchema = z.object({
  title: z.string().trim().min(2, "Title is required").max(200),
  slug: z.string().trim().max(120).optional().or(z.literal("")),
  description: z.string().trim().max(2000).optional().or(z.literal("")),
  kind: z.enum(["qbank", "batch", "exam", "chapter"]).default("qbank"),
  coverUrl: z.string().url().optional().or(z.literal("")),
  price: z.number().int().min(0).max(10_000_000).default(0),
  batchId: z.string().optional().or(z.literal("")),
});

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const url = new URL(req.url);
  const kind = url.searchParams.get("kind"); // qbank | batch | exam | chapter
  const where: Record<string, unknown> = {};
  if (kind) where.kind = kind;
  const bundles = await db.questionBundle.findMany({
    where,
    orderBy: { createdAt: "desc" },
    include: { _count: { select: { items: true } } },
    take: 200,
  });
  return NextResponse.json({ bundles });
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const body = await req.json().catch(() => null);
  const parsed = createSchema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json(
      { error: parsed.error.issues[0]?.message ?? "Invalid data" },
      { status: 400 },
    );
  }
  const d = parsed.data;
  const slug = await uniqueSlug(d.slug ? slugify(d.slug) : slugify(d.title));

  const bundle = await db.questionBundle.create({
    data: {
      title: d.title,
      slug,
      description: d.description || null,
      kind: d.kind,
      coverUrl: d.coverUrl || null,
      price: d.price,
      batchId: d.batchId || null,
      createdBy: user.id,
    },
    include: { _count: { select: { items: true } } },
  });
  return NextResponse.json({ bundle });
}
