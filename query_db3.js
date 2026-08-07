const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});

async function main() {
  const tests = await prisma.$queryRaw`
    SELECT t.id, t.title
    FROM "Test" t
    WHERE EXISTS (
      SELECT 1 FROM "TestItem" ti
      JOIN "Question" q ON ti."questionId" = q.id
      WHERE ti."testId" = t.id
      AND (q."descImageUrl" LIKE '%/api/files/%' 
           OR q."optionImages" LIKE '%/api/files/%'
           OR q."mediaImageUrl" LIKE '%/api/files/%')
    )
    LIMIT 5
  `;
  tests.forEach(t => console.log(t.id, '|', t.title));
}

main().then(() => prisma.$disconnect()).catch(e => { console.error(e); process.exit(1); });
