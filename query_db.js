const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});

async function main() {
  const questions = await prisma.$queryRaw`
    SELECT id, "descImageUrl", "mediaImageUrl", "imageUrl", "audioUrl", "mediaAudioUrl", "optionImages", "answerType", "descType", "mediaType"
    FROM "Question"
    WHERE "descImageUrl" IS NOT NULL OR "mediaImageUrl" IS NOT NULL OR "imageUrl" IS NOT NULL OR "optionImages" IS NOT NULL
    LIMIT 5
  `;
  console.log(JSON.stringify(questions, null, 2));
}

main().then(() => prisma.$disconnect()).catch(e => { console.error(e); process.exit(1); });
