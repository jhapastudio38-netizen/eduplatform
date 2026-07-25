/**
 * GET /api/student/eye-vision
 * Returns all published eye vision tests for students.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET(req: NextRequest) {
  const user = await getCurrentUser(req);
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
  const tests = await db.eyeVisionTest.findMany({
    where: { isPublished: true },
    orderBy: [{ sortOrder: "asc" }, { createdAt: "desc" }],
    select: {
      id: true,
      title: true,
      description: true,
      imageUrl: true,
      category: true,
      // NOTE: correctAnswer is NOT sent — student submits their answer,
      // server checks it via POST /api/student/eye-vision/[id]/check
    },
  });
  return NextResponse.json({ tests });
}
