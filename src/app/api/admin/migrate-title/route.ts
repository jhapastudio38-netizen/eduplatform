/**
 * POST /api/admin/migrate-title
 *
 * One-time migration: adds the `title` column to the Question table if it
 * doesn't exist. The Deploy Web workflow's `prisma db push` failed because
 * GitHub Actions can't reach Supabase's direct Postgres connection — this
 * endpoint runs inside Vercel (which CAN reach Supabase) to apply the
 * schema change.
 *
 * Admin-only. Safe to call multiple times (uses IF NOT EXISTS).
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  try {
    // Add the title column if it doesn't exist
    await db.$executeRaw`ALTER TABLE "Question" ADD COLUMN IF NOT EXISTS "title" TEXT`;
    // Add the isFree column if it doesn't exist (free/paid question feature)
    await db.$executeRaw`ALTER TABLE "Question" ADD COLUMN IF NOT EXISTS "isFree" BOOLEAN NOT NULL DEFAULT false`;
    return NextResponse.json({ ok: true, message: "title + isFree columns added (or already existed)" });
  } catch (e: any) {
    return NextResponse.json(
      { error: e.message?.substring(0, 300) || "Migration failed" },
      { status: 500 },
    );
  }
}
