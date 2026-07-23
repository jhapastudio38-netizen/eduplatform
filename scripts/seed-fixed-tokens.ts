/**
 * Seed a FIXED admin login token so the admin URL is always the same.
 * This is for convenience — the admin URL won't change between deploys.
 *
 * Admin URL: https://dreamkoreasmartclass.com/admin-login/dreamkorea-admin-2026
 * Admin ID:  admin
 * Admin Password: DreamKorea@2026
 *
 * Teacher URL: https://dreamkoreasmartclass.com/teacher
 * (Teachers login with username + password, created by admin)
 */
import { db } from "../src/lib/db";

async function main() {
  const adminToken = "dreamkorea-admin-2026";
  const teacherToken = "dreamkorea-teacher-2026";

  // Expires in 365 days
  const expiresAt = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000);

  // Upsert admin token (don't delete existing — just update if exists)
  const existingAdmin = await db.secureLoginToken.findUnique({ where: { token: adminToken } });
  if (existingAdmin) {
    await db.secureLoginToken.update({
      where: { token: adminToken },
      data: { expiresAt, role: "ADMIN" },
    });
  } else {
    await db.secureLoginToken.create({
      data: { role: "ADMIN", token: adminToken, expiresAt },
    });
  }

  // Upsert teacher token
  const existingTeacher = await db.secureLoginToken.findUnique({ where: { token: teacherToken } });
  if (existingTeacher) {
    await db.secureLoginToken.update({
      where: { token: teacherToken },
      data: { expiresAt, role: "TEACHER" },
    });
  } else {
    await db.secureLoginToken.create({
      data: { role: "TEACHER", token: teacherToken, expiresAt },
    });
  }

  console.log("✅ Fixed admin/teacher tokens seeded:");
  console.log(`   Admin URL:   https://dreamkoreasmartclass.com/admin-login/${adminToken}`);
  console.log(`   Admin ID:    admin`);
  console.log(`   Admin Pass:  DreamKorea@2026`);
  console.log(`   Teacher URL: https://dreamkoreasmartclass.com/teacher`);
}

main()
  .catch((e) => { console.error(e); process.exit(1); })
  .finally(() => db.$disconnect());
