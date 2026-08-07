const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});
async function main() {
  // Find a test that has questions with images
  const tests = await prisma.$queryRaw`
    SELECT t.id, t.title, t."durationMin",
      (SELECT COUNT(*) FROM "TestItem" ti 
       JOIN "Question" q ON ti."questionId" = q.id 
       WHERE ti."testId" = t.id AND q."mediaImageUrl" IS NOT NULL AND q."mediaImageUrl" != '') as img_count
    FROM "Test" t
    WHERE t."isPublished" = true
    AND EXISTS (
      SELECT 1 FROM "TestItem" ti 
      JOIN "Question" q ON ti."questionId" = q.id 
      WHERE ti."testId" = t.id AND q."mediaImageUrl" IS NOT NULL AND q."mediaImageUrl" != ''
    )
    ORDER BY img_count DESC
    LIMIT 3
  `;
  tests.forEach(t => console.log(t.id, '|', t.title, '|', t.durationMin, 'min |', t.img_count, 'images'));
}
main().then(() => prisma.$disconnect()).catch(e => console.error(e));
