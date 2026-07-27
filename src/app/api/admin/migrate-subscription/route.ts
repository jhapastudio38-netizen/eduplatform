/**
 * POST /api/admin/migrate-subscription
 *
 * One-time migration: adds subscription fields to the User table and
 * priceNpr to the Test table. Safe to call multiple times (IF NOT EXISTS).
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
    await db.$executeRaw`ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "subscriptionType" TEXT`;
    await db.$executeRaw`ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "subscribedUntil" TIMESTAMP`;
    await db.$executeRaw`ALTER TABLE "User" ADD COLUMN IF NOT EXISTS "subscriptionPrice" INTEGER`;
    await db.$executeRaw`ALTER TABLE "Test" ADD COLUMN IF NOT EXISTS "priceNpr" INTEGER`;
    return NextResponse.json({ ok: true, message: "Subscription + priceNpr columns added" });
  } catch (e: any) {
    return NextResponse.json(
      { error: e.message?.substring(0, 300) || "Migration failed" },
      { status: 500 },
    );
  }
}
