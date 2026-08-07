const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});
async function main() {
  // Get the exact question from "FINAL UBT SET1 2026" test (cms8gqqkd0000jx040j05w6lf)
  // Q7 has mediaImageUrl
  const rows = await prisma.$queryRaw`
    SELECT q.id, q.stem, q."mediaType", q."descType", q."answerType",
           q."mediaImageUrl", q."imageUrl", q."descImageUrl",
           q."optionImages", q."audioUrl", q."mediaAudioUrl"
    FROM "TestItem" ti
    JOIN "Question" q ON ti."questionId" = q.id
    WHERE ti."testId" = 'cms8gqqkd0000jx040j05w6lf'
    AND (q."mediaImageUrl" IS NOT NULL AND q."mediaImageUrl" != '')
    LIMIT 3
  `;
  rows.forEach((r, i) => {
    console.log(`=== QUESTION ${i+1} ===`);
    console.log('ID:', r.id);
    console.log('Stem:', r.stem?.substring(0, 60));
    console.log('mediaType:', r.mediaType);
    console.log('descType:', r.descType);
    console.log('answerType:', r.answerType);
    console.log('mediaImageUrl:', r.mediaImageUrl);
    console.log('imageUrl:', r.imageUrl);
    console.log('descImageUrl:', r.descImageUrl);
    console.log('optionImages:', r.optionImages);
    console.log('audioUrl:', r.audioUrl);
    console.log('mediaAudioUrl:', r.mediaAudioUrl);
    console.log('');
  });
}
main().then(() => prisma.$disconnect()).catch(e => console.error(e));
