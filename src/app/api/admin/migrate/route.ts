import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function POST(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const results: string[] = [];

  // Add missing columns to Test table
  const testColumns = [
    { name: "showAllBlocks", type: "BOOLEAN DEFAULT true" },
    { name: "textBlockEnabled", type: "BOOLEAN DEFAULT true" },
    { name: "audioBlockEnabled", type: "BOOLEAN DEFAULT true" },
  ];

  for (const col of testColumns) {
    try {
      await db.$executeRawUnsafe(`ALTER TABLE "Test" ADD COLUMN IF NOT EXISTS "${col.name}" ${col.type}`);
      results.push(`Test.${col.name} — OK`);
    } catch (e: any) {
      results.push(`Test.${col.name} — ${e.message?.substring(0, 80)}`);
    }
  }

  return NextResponse.json({ ok: true, results });
}
