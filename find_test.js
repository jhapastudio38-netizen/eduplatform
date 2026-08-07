const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});
async function main() {
  const rows = await prisma.$queryRaw`
    SELECT q.id, q.stem, q."mediaImageUrl", q."imageUrl", q."descImageUrl", q."mediaType",
           t.id as "testId", t.title
    FROM "Question" q
    JOIN "TestItem" ti ON q.id = ti."questionId"
    JOIN "Test" t ON ti."testId" = t.id
    WHERE q.options::text LIKE '%라디오%'
    LIMIT 5
  `;
  rows.forEach(r => console.log('Test:', r.testId, '|', r.title, '| Q:', r.stem?.substring(0,40), '| mediaImg:', r.mediaImageUrl, '| img:', r.imageUrl));
}
main().then(() => prisma.$disconnect()).catch(e => console.error(e));
