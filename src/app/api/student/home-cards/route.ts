/**
 * GET /api/student/home-cards
 * Returns all active home cards for the student app, grouped by section.
 * Public (requires auth but any logged-in user can fetch).
 */
import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET() {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const cards = await db.homeCard.findMany({
    where: { isActive: true },
    orderBy: [{ section: "asc" }, { sortOrder: "asc" }],
  });

  // Group by section
  const sections: Record<string, typeof cards> = {};
  for (const c of cards) {
    if (!sections[c.section]) sections[c.section] = [];
    sections[c.section].push(c);
  }

  return NextResponse.json({ sections, cards });
}
