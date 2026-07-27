/**
 * GET  /api/admin/teacher-invites — list invites (admin only)
 * POST /api/admin/teacher-invites — create a new invite (admin only)
 *   Body: { presetName?, presetEmail?, expiresInDays?: 7|14|30 }
 *   Returns: { invite: { id, code, expiresAt, presetName, presetEmail } }
 *
 * Teachers can self-sign up at /teacher?invite=<code>. The invite is single-use
 * and tracked through to the user who consumed it.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import { randomBytes } from "crypto";
import { z } from "zod";

// Generate a human-readable, all-caps invite code: DK-XXXX-XXXX
function generateInviteCode(): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no I/O/0/1 to avoid confusion
  const bytes = randomBytes(8);
  let p1 = "";
  let p2 = "";
  for (let i = 0; i < 4; i++) p1 += alphabet[bytes[i] % alphabet.length];
  for (let i = 4; i < 8; i++) p2 += alphabet[bytes[i] % alphabet.length];
  return `DK-${p1}-${p2}`;
}

const createSchema = z.object({
  presetName: z.string().trim().max(100).optional().or(z.literal("")),
  presetEmail: z.string().trim().toLowerCase().email().max(254).optional().or(z.literal("")),
  expiresInDays: z.number().int().min(1).max(90).default(14),
});

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const invites = await db.teacherInvite.findMany({
    orderBy: { createdAt: "desc" },
    take: 100,
  });
  return NextResponse.json({ invites });
}

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  // Note: only admins create invites. Teachers — even those with
  // canCreateTeachers — must go through the admin endpoint at /api/admin/users
  // because they cannot mint invite codes themselves.
  if (user.role !== "ADMIN") {
    return NextResponse.json({ error: "Only admins can create invite codes" }, { status: 403 });
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
  const expiresAt = new Date(Date.now() + d.expiresInDays * 24 * 3600 * 1000);

  // Generate a unique code (retry on rare collision)
  let code = "";
  for (let i = 0; i < 5; i++) {
    const candidate = generateInviteCode();
    const exists = await db.teacherInvite.findUnique({ where: { code: candidate } });
    if (!exists) { code = candidate; break; }
  }
  if (!code) {
    return NextResponse.json({ error: "Failed to generate unique code" }, { status: 500 });
  }

  const invite = await db.teacherInvite.create({
    data: {
      code,
      presetName: d.presetName || null,
      presetEmail: d.presetEmail || null,
      createdBy: user.id,
      expiresAt,
    },
  });

  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  await audit({
    actorId: user.id,
    action: "create_teacher_invite",
    entity: "TeacherInvite",
    entityId: invite.id,
    ip,
    metadata: { code, expiresAt: expiresAt.toISOString() },
  });

  return NextResponse.json({ invite });
}

export async function DELETE(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const url = new URL(req.url);
  const id = url.searchParams.get("id");
  if (!id) return NextResponse.json({ error: "Missing id" }, { status: 400 });

  await db.teacherInvite.deleteMany({ where: { id, consumedBy: null } });
  return NextResponse.json({ ok: true });
}
