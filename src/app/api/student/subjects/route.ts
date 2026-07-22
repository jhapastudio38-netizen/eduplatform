import { NextResponse } from "next/server";
import { db } from "@/lib/db";

export async function GET() {
  const subjects = await db.subject.findMany({
    orderBy: { name: "asc" },
    select: { id: true, name: true, slug: true, description: true, iconUrl: true },
  });
  return NextResponse.json({ subjects });
}
