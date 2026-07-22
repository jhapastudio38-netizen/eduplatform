/**
 * Seed secure admin + teacher login links + sample content.
 * Run: bun run scripts/seed-secure-logins.ts
 */
import { db } from "../src/lib/db";
import { generateToken } from "../src/lib/security";

async function main() {
  // Generate secure tokens (32 chars each)
  const adminToken = generateToken(16);
  const teacherToken = generateToken(16);

  // Expires in 90 days
  const expiresAt = new Date(Date.now() + 90 * 24 * 60 * 60 * 1000);

  // Clean up old tokens
  await db.secureLoginToken.deleteMany({});

  await db.secureLoginToken.create({
    data: { role: "ADMIN", token: adminToken, expiresAt },
  });
  await db.secureLoginToken.create({
    data: { role: "TEACHER", token: teacherToken, expiresAt },
  });

  console.log("Secure login tokens created (valid for 90 days):\n");
  console.log(`Admin URL:   https://dreamkoreasmartclass.com/admin-login/${adminToken}`);
  console.log(`Teacher URL: https://dreamkoreasmartclass.com/teacher-login/${teacherToken}`);
  console.log("\nSave these URLs securely. Share with admin/teacher out-of-band.");

  // Create sample UserStat records for existing users
  const users = await db.user.findMany({ select: { id: true } });
  for (const u of users) {
    await db.userStat.upsert({
      where: { userId: u.id },
      create: { userId: u.id },
      update: {},
    });
  }
  console.log(`\nInitialized UserStat for ${users.length} users.`);

  // Create sample books
  const bookCount = await db.book.count();
  if (bookCount === 0) {
    await db.book.createMany({
      data: [
        {
          title: "TOPIK I Complete Guide",
          slug: "topik-1-complete-guide",
          description: "Comprehensive guide for TOPIK Level 1 & 2 exam preparation.",
          author: "DreamKorea Team",
          category: "TOPIK",
          level: "Beginner",
          isPublished: true,
          pageCount: 248,
        },
        {
          title: "Korean Conversation Essentials",
          slug: "korean-conversation-essentials",
          description: "Everyday Korean phrases and dialogues for beginners.",
          author: "Park Min-jun",
          category: "Conversation",
          level: "Beginner",
          isPublished: true,
          pageCount: 120,
        },
        {
          title: "Intermediate Korean Grammar",
          slug: "intermediate-korean-grammar",
          description: "Deep dive into intermediate Korean grammar patterns.",
          author: "Lee Soo-jin",
          category: "Grammar",
          level: "Intermediate",
          isPublished: true,
          pageCount: 320,
        },
      ],
    });
    console.log("Created 3 sample books.");
  }

  // Create sample audio lessons
  const audioCount = await db.audioLesson.count();
  if (audioCount === 0) {
    await db.audioLesson.createMany({
      data: [
        {
          title: "Daily Korean Conversation — Episode 1",
          slug: "daily-conversation-1",
          description: "Learn how to greet people in Korean.",
          audioUrl: "https://example.com/audio/lesson-1.mp3",
          durationSec: 180,
          transcript: "안녕하세요. 만나서 반갑습니다.",
          translation: "Hello. Nice to meet you.",
          level: "Beginner",
          category: "Conversation",
          isPublished: true,
        },
        {
          title: "Korean News Reading — Intermediate",
          slug: "korean-news-reading-1",
          description: "Practice listening to natural Korean news.",
          audioUrl: "https://example.com/audio/news-1.mp3",
          durationSec: 360,
          level: "Intermediate",
          category: "News",
          isPublished: true,
        },
        {
          title: "Pronunciation Masterclass — Vowels",
          slug: "pronunciation-vowels",
          description: "Master the 10 basic Korean vowels.",
          audioUrl: "https://example.com/audio/vowels.mp3",
          durationSec: 240,
          level: "Beginner",
          category: "Pronunciation",
          isPublished: true,
        },
      ],
    });
    console.log("Created 3 sample audio lessons.");
  }

  console.log("\nSeed complete.");
}

main()
  .catch((e) => { console.error(e); process.exit(1); })
  .finally(() => db.$disconnect());
