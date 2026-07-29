/**
 * POST /api/admin/migrate
 *
 * Runs database migrations via raw SQL (since prisma db push can't reach
 * the database from GitHub Actions or local dev).
 *
 * This endpoint adds any missing columns to the Test table.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const results: string[] = [];

  // Add showAllBlocks column if it doesn't exist
  try {
    await db.$executeRaw`ALTER TABLE "Test" ADD COLUMN IF NOT EXISTS "showAllBlocks" BOOLEAN DEFAULT true`;
    results.push("✓ Test.showAllBlocks column added (or already exists)");
  } catch (e: any) {
    results.push(`✗ Test.showAllBlocks: ${e.message?.substring(0, 100)}`);
  }

  return NextResponse.json({ ok: true, results });
}
