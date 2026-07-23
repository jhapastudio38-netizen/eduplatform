/**
 * POST /api/admin/seed
 * Runs the database seed scripts from inside the running server.
 * This endpoint has the correct DATABASE_URL (from ECS task definition).
 *
 * Body: { secret: string } — must match ADMIN_PASSWORD env var
 *
 * This fixes the issue where seed scripts fail in GitHub Actions because
 * PrismaClient can't find DATABASE_URL (bun loads .env which has SQLite).
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";

export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => ({})) as { secret?: string };
  const secret = body.secret;
  const adminPassword = process.env.ADMIN_PASSWORD || "DreamKorea@2026";

  if (secret !== adminPassword) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const results: string[] = [];

  try {
    // ─── Seed fixed admin/teacher tokens ────────────────────────────────────
    const adminToken = "dreamkorea-admin-2026";
    const teacherToken = "dreamkorea-teacher-2026";
    const expiresAt = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000);

    const existingAdmin = await db.secureLoginToken.findUnique({ where: { token: adminToken } });
    if (existingAdmin) {
      await db.secureLoginToken.update({
        where: { token: adminToken },
        data: { expiresAt, role: "ADMIN" },
      });
      results.push("Admin token updated");
    } else {
      await db.secureLoginToken.create({
        data: { role: "ADMIN", token: adminToken, expiresAt },
      });
      results.push("Admin token created");
    }

    const existingTeacher = await db.secureLoginToken.findUnique({ where: { token: teacherToken } });
    if (existingTeacher) {
      await db.secureLoginToken.update({
        where: { token: teacherToken },
        data: { expiresAt, role: "TEACHER" },
      });
      results.push("Teacher token updated");
    } else {
      await db.secureLoginToken.create({
        data: { role: "TEACHER", token: teacherToken, expiresAt },
      });
      results.push("Teacher token created");
    }

    // ─── Seed home cards (if none exist) ────────────────────────────────────
    const cardCount = await db.homeCard.count();
    if (cardCount === 0) {
      const defaultCards = [
        { key: "ubt_test", title: "UBT TEST", section: "test", sortOrder: 0, route: "tests" },
        { key: "free_exam", title: "Free Exam", section: "test", sortOrder: 1, route: "tests" },
        { key: "batch", title: "Batch", section: "test", sortOrder: 2, route: "tests" },
        { key: "results", title: "Results", section: "test", sortOrder: 3, route: "profile" },
        { key: "all_books", title: "ALL BOOKS", section: "resources", sortOrder: 0, route: "books" },
        { key: "question_bank", title: "QUESTION BANK", section: "resources", sortOrder: 1, route: "learn" },
        { key: "course_video", title: "COURSE VIDEO", section: "resources", sortOrder: 2, route: "videos" },
        { key: "audio_lessons", title: "AUDIO LESSONS", section: "resources", sortOrder: 3, route: "learn" },
        { key: "classroom", title: "CLASSROOM", section: "premium", sortOrder: 0, route: "live" },
        { key: "live_class", title: "LIVE CLASS", section: "premium", sortOrder: 1, route: "live" },
        { key: "recorded_video", title: "RECORDED VIDEO", section: "premium", sortOrder: 2, route: "videos" },
        { key: "class_result", title: "CLASS RESULT", section: "premium", sortOrder: 3, route: "profile" },
      ];
      for (const c of defaultCards) {
        await db.homeCard.create({ data: c });
      }
      results.push(`Created ${defaultCards.length} home cards`);
    } else {
      results.push(`Home cards already exist (${cardCount})`);
    }

    // ─── Seed sample test (if none exist) ───────────────────────────────────
    const testCount = await db.test.count();
    if (testCount === 0) {
      // Create a simple question + test so the app isn't empty
      const q = await db.question.create({
        data: {
          type: "SINGLE_CHOICE",
          difficulty: "EASY",
          stem: "What does '안녕하세요' mean?",
          options: JSON.stringify(["Goodbye", "Hello", "Thank you", "Sorry"]),
          correctAnswer: JSON.stringify("Hello"),
          explanation: "안녕하세요 is the standard Korean greeting.",
        },
      });
      await db.test.create({
        data: {
          title: "Korean Greetings Quiz",
          description: "Test your knowledge of Korean greetings.",
          durationMin: 5,
          isExam: true,
          examType: "REGULAR",
          passScore: 50,
          isPublished: true,
          isActive: true,
          items: {
            create: [{ questionId: q.id, points: 1, order: 0 }],
          },
        },
      });
      results.push("Created sample test");
    } else {
      results.push(`Tests already exist (${testCount})`);
    }

    return NextResponse.json({
      ok: true,
      results,
      adminUrl: `https://dreamkoreasmartclass.com/admin-login/${adminToken}`,
      adminId: "admin",
      adminPassword: "DreamKorea@2026",
    });
  } catch (e: any) {
    return NextResponse.json({
      error: e.message,
      results,
    }, { status: 500 });
  }
}
