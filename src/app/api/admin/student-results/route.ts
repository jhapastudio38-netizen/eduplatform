import { NextResponse } from "next/server";
import { db } from "@/lib/db";
import { getCurrentUser } from "@/lib/session";

export async function GET() {
  const user = await getCurrentUser();
  if (!user || (user.role !== "ADMIN" && user.role !== "TEACHER")) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const students = await db.user.findMany({
    where: { role: "STUDENT" },
    select: {
      id: true, name: true, email: true, role: true,
      _count: { select: { submissions: { where: { submittedAt: { not: null } } } } },
    },
    take: 200,
  });

  // Fetch stats for each student
  const out = await Promise.all(
    students.map(async (s) => {
      const stats = await db.userStat.findUnique({ where: { userId: s.id } });
      return { ...s, stats };
    }),
  );

  return NextResponse.json({ students: out });
}
