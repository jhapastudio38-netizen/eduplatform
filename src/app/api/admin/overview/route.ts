import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || user.role !== "ADMIN") {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }
  const [subjects, chapters, lessons, questions, tests, students] = await Promise.all([
    db.subject.count(),
    db.chapter.count(),
    db.lesson.count(),
    db.question.count(),
    db.test.count(),
    db.user.count({ where: { role: "STUDENT" } }),
  ]);
  return NextResponse.json({ subjects, chapters, lessons, questions, tests, students });
}
