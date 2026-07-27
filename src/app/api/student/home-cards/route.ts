/**
 * GET /api/student/home-cards
 * Returns all active home cards for the student app.
 */
import { NextResponse, NextRequest } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const cards = await db.homeCard.findMany({
    where: { isActive: true },
    orderBy: [{ section: "asc" }, { sortOrder: "asc" }],
  });

  return NextResponse.json({
    cards: cards.map(c => ({
      id: c.id,
      key: c.key,
      title: c.title,
      section: c.section,
      imageUrl: c.imageUrl || null,
      sortOrder: c.sortOrder,
      isActive: c.isActive,
      route: c.route || null,
    })),
  });
}
