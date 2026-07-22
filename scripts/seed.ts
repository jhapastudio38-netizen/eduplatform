/**
 * Seed script — creates an admin user and sample content so the app
 * is immediately usable after fresh setup.
 *
 * Run: bun run scripts/seed.ts
 */
import { db } from "../src/lib/db";

async function main() {
  // Admin user (auto-verified, can log in via OTP flow with the email below)
  const admin = await db.user.upsert({
    where: { email: "admin@eduplatform.app" },
    create: {
      email: "admin@eduplatform.app",
      phone: "+9779800000001",
      name: "Admin",
      role: "ADMIN",
      isVerified: true,
    },
    update: { role: "ADMIN", isVerified: true },
  });

  const teacher = await db.user.upsert({
    where: { email: "teacher@eduplatform.app" },
    create: {
      email: "teacher@eduplatform.app",
      phone: "+9779800000002",
      name: "Demo Teacher",
      role: "TEACHER",
      isVerified: true,
    },
    update: { role: "TEACHER", isVerified: true },
  });

  const student = await db.user.upsert({
    where: { email: "student@eduplatform.app" },
    create: {
      email: "student@eduplatform.app",
      phone: "+9779800000003",
      name: "Demo Student",
      role: "STUDENT",
      isVerified: true,
    },
    update: { role: "STUDENT", isVerified: true },
  });

  // Sample subject + chapter + lesson + question
  const subject = await db.subject.upsert({
    where: { slug: "mathematics" },
    create: { name: "Mathematics", slug: "mathematics", description: "Numbers, algebra, geometry and more." },
    update: {},
  });

  const chapter = await db.chapter.upsert({
    where: { subjectId_slug: { subjectId: subject.id, slug: "linear-equations" } },
    create: {
      subjectId: subject.id,
      title: "Linear Equations",
      slug: "linear-equations",
      description: "Solving one-variable linear equations.",
      order: 1,
      isPublished: true,
      authorId: teacher.id,
    },
    update: {},
  });

  const lesson = await db.lesson.upsert({
    where: { chapterId_slug: { chapterId: chapter.id, slug: "intro" } },
    create: {
      chapterId: chapter.id,
      title: "Introduction to Linear Equations",
      slug: "intro",
      type: "TEXT",
      content: "# Linear Equations\n\nA **linear equation** in one variable has the form:\n\n`ax + b = 0` where `a != 0`.\n\n## Solving\n1. Isolate the variable term.\n2. Divide by its coefficient.\n\n### Example\n`2x + 6 = 14` -> `2x = 8` -> `x = 4`",
      durationMin: 10,
      order: 1,
      isPublished: true,
    },
    update: {},
  });

  // Sample questions (only create if chapter has none)
  const existingQs = await db.question.count({ where: { chapterId: chapter.id } });
  if (existingQs === 0) {
    const q1 = await db.question.create({
      data: {
        chapterId: chapter.id,
        type: "SINGLE_CHOICE",
        difficulty: "EASY",
        stem: "Solve for x:  2x + 6 = 14",
        options: JSON.stringify(["x = 2", "x = 4", "x = 6", "x = 8"]),
        correctAnswer: JSON.stringify("x = 4"),
        explanation: "Subtract 6 from both sides, then divide by 2.",
      },
    });

    const q2 = await db.question.create({
      data: {
        chapterId: chapter.id,
        type: "ONE_WORD",
        difficulty: "MEDIUM",
        stem: "What value of x satisfies  3x - 5 = 16 ?",
        correctAnswer: JSON.stringify("7"),
        explanation: "3x = 21, so x = 7.",
      },
    });

    // Sample test
    const existingTest = await db.test.findFirst({ where: { title: "Linear Equations Quiz" } });
    if (!existingTest) {
      await db.test.create({
        data: {
          chapterId: chapter.id,
          title: "Linear Equations Quiz",
          description: "Practice quiz - 2 questions, 5 minutes.",
          durationMin: 5,
          passScore: 50,
          isPublished: true,
          items: {
            create: [
              { questionId: q1.id, points: 1, order: 0 },
              { questionId: q2.id, points: 1, order: 1 },
            ],
          },
        },
      });
    }
  }

  console.log("Seed complete.");
  console.log("Admin:    admin@eduplatform.app");
  console.log("Teacher:  teacher@eduplatform.app");
  console.log("Student:  student@eduplatform.app");
  console.log("(In dev mode without RESEND_API_KEY, OTP codes are printed to server log.)");
}

main()
  .catch((e) => { console.error(e); process.exit(1); })
  .finally(() => db.$disconnect());
