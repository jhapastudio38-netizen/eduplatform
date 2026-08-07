const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});
async function main() {
  const rows = await prisma.$queryRaw`
    SELECT id, "descImageUrl", "mediaImageUrl", "imageUrl", "optionImages", "answerType"
    FROM "Question"
    WHERE "descImageUrl" IS NOT NULL AND "descImageUrl" != ''
       OR "mediaImageUrl" IS NOT NULL AND "mediaImageUrl" != ''
       OR "imageUrl" IS NOT NULL AND "imageUrl" != ''
       OR "optionImages" IS NOT NULL AND "optionImages" LIKE '%/api/files/%'
    LIMIT 5
  `;
  rows.forEach(r => {
    console.log('ID:', r.id);
    console.log('  descImageUrl:', r.descImageUrl);
    console.log('  mediaImageUrl:', r.mediaImageUrl);
    console.log('  imageUrl:', r.imageUrl);
    console.log('  optionImages:', r.optionImages);
    console.log('  answerType:', r.answerType);
    console.log('---');
  });
}
main().then(() => prisma.$disconnect()).catch(e => console.error(e));
