import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";
import { audit } from "@/lib/audit";
import { hashPassword } from "@/lib/password";
import {
  emailSchema,
  phoneSchema,
  rateLimitKey,
} from "@/lib/security";
import { rateLimited } from "@/lib/rate-limit";

/**
 * GET /api/admin/users
 *   ?role=STUDENT|TEACHER|ADMIN  (optional filter)
 *   ?q=search                     (optional search by name/email/phone)
 *   ?page=1&limit=50              (pagination)
 *
 * Returns user list with name, email, phone, role, banned status, signup date, signup method.
 * Admin-only.
 */
export async function GET(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const url = new URL(req.url);
  const role = url.searchParams.get("role"); // STUDENT | TEACHER | ADMIN
  const q = url.searchParams.get("q")?.trim() || "";
  const page = Math.max(1, parseInt(url.searchParams.get("page") || "1", 10));
  const limit = Math.min(200, Math.max(10, parseInt(url.searchParams.get("limit") || "50", 10)));

  const where: Record<string, unknown> = {};
  if (role && ["STUDENT", "TEACHER", "ADMIN"].includes(role)) {
    where.role = role;
  }
  if (q) {
    where.OR = [
      { name: { contains: q, mode: "insensitive" } },
      { email: { contains: q, mode: "insensitive" } },
      { phone: { contains: q, mode: "insensitive" } },
      { username: { contains: q, mode: "insensitive" } },
    ];
  }

  const [users, total] = await Promise.all([
    db.user.findMany({
      where,
      orderBy: { createdAt: "desc" },
      skip: (page - 1) * limit,
      take: limit,
      select: {
        id: true,
        name: true,
        email: true,
        phone: true,
        username: true,
        role: true,
        isBanned: true,
        isVerified: true,
        signupMethod: true,
        createdBy: true,
        createdAt: true,
        lastActiveAt: true,
      },
    }),
    db.user.count({ where }),
  ]);

  // Stats overview
  const stats = {
    total,
    totalStudents: await db.user.count({ where: { role: "STUDENT" } }),
    totalTeachers: await db.user.count({ where: { role: "TEACHER" } }),
    totalAdmins: await db.user.count({ where: { role: "ADMIN" } }),
    bannedUsers: await db.user.count({ where: { isBanned: true } }),
    verifiedUsers: await db.user.count({ where: { isVerified: true } }),
  };

  return NextResponse.json({ users, total, page, limit, stats });
}

/**
 * POST /api/admin/users
 * Body: { action: "create_teacher", username, password, name, email, phone }
 *
 * Admin creates a new teacher account with credentials (no OTP for teacher).
 */
export async function POST(req: NextRequest) {
  const admin = await getCurrentUser();
  if (!admin || admin.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }
  const b = body as {
    action?: unknown;
    username?: unknown;
    password?: unknown;
    name?: unknown;
    email?: unknown;
    phone?: unknown;
  };

  if (b.action !== "create_teacher") {
    return NextResponse.json({ error: "Unknown action" }, { status: 400 });
  }

  const username =
    typeof b.username === "string" ? b.username.trim().toLowerCase().slice(0, 50) : "";
  const password = typeof b.password === "string" ? b.password : "";
  const name = typeof b.name === "string" ? b.name.trim().slice(0, 100) : "";
  const emailRaw = typeof b.email === "string" ? b.email.trim().toLowerCase() : "";
  const phoneRaw = typeof b.phone === "string" ? b.phone.trim() : "";

  // Validate
  if (!username || username.length < 3) {
    return NextResponse.json({ error: "Username must be at least 3 characters" }, { status: 400 });
  }
  if (!/^[a-z0-9._-]+$/.test(username)) {
    return NextResponse.json({ error: "Username can only contain letters, numbers, dot, underscore, hyphen" }, { status: 400 });
  }
  if (password.length < 6) {
    return NextResponse.json({ error: "Password must be at least 6 characters" }, { status: 400 });
  }
  if (!name) {
    return NextResponse.json({ error: "Name is required" }, { status: 400 });
  }
  const email = emailSchema.safeParse(emailRaw).success ? emailRaw : "";
  const phone = phoneSchema.safeParse(phoneRaw).success ? phoneRaw : null;
  if (!email) {
    return NextResponse.json({ error: "Valid email is required" }, { status: 400 });
  }

  // Rate limit
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";
  if (rateLimited(rateLimitKey("create-teacher", ip), 20, 3600)) {
    return NextResponse.json({ error: "Too many requests. Slow down." }, { status: 429 });
  }

  // Check for existing username/email
  const existing = await db.user.findFirst({
    where: { OR: [{ username }, { email }] },
  });
  if (existing) {
    return NextResponse.json(
      { error: existing.username === username ? "Username already taken" : "Email already registered" },
      { status: 409 },
    );
  }

  // Hash password
  const passwordHash = await hashPassword(password);

  // Create teacher
  const teacher = await db.user.create({
    data: {
      username,
      email,
      phone,
      name,
      role: "TEACHER",
      isVerified: true,
      passwordHash,
      signupMethod: "credentials",
      createdBy: admin.id,
    },
    select: {
      id: true,
      username: true,
      name: true,
      email: true,
      phone: true,
      role: true,
      createdAt: true,
    },
  });

  await audit({
    actorId: admin.id,
    action: "create_teacher",
    entity: "User",
    entityId: teacher.id,
    ip,
    metadata: JSON.stringify({ username, email }),
  });

  return NextResponse.json({ ok: true, teacher });
}
