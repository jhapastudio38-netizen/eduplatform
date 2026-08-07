const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});
async function main() {
  // Get ALL questions that have ANY image URL
  const rows = await prisma.$queryRaw`
    SELECT id, "descImageUrl", "mediaImageUrl", "imageUrl", "optionImages", "answerType", "descType", "mediaType"
    FROM "Question"
    WHERE 
      ("descImageUrl" IS NOT NULL AND "descImageUrl" != '')
      OR ("mediaImageUrl" IS NOT NULL AND "mediaImageUrl" != '')
      OR ("imageUrl" IS NOT NULL AND "imageUrl" != '')
      OR ("optionImages" IS NOT NULL AND "optionImages" LIKE '%http%')
    LIMIT 20
  `;
  console.log('Total questions with image URLs:', rows.length);
  rows.forEach((r, i) => {
    console.log(`\n--- Q${i+1} (id: ${r.id}) ---`);
    console.log('  descImageUrl:', r.descImageUrl);
    console.log('  mediaImageUrl:', r.mediaImageUrl);
    console.log('  imageUrl:', r.imageUrl);
    console.log('  optionImages:', r.optionImages);
    console.log('  answerType:', r.answerType);
    console.log('  descType:', r.descType);
    console.log('  mediaType:', r.mediaType);
  });
}
main().then(() => prisma.$disconnect()).catch(e => console.error(e));
