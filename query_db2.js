const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});

async function main() {
  // Check for any questions with /api/files/ URLs
  const r2Questions = await prisma.$queryRaw`
    SELECT id, "descImageUrl", "mediaImageUrl", "imageUrl", "audioUrl", "mediaAudioUrl", "optionImages", "answerType", "descType", "mediaType"
    FROM "Question"
    WHERE "descImageUrl" LIKE '%/api/files/%' 
       OR "mediaImageUrl" LIKE '%/api/files/%'
       OR "imageUrl" LIKE '%/api/files/%'
       OR "audioUrl" LIKE '%/api/files/%'
       OR "mediaAudioUrl" LIKE '%/api/files/%'
       OR "optionImages" LIKE '%/api/files/%'
    LIMIT 5
  `;
  console.log('Questions with R2 URLs:', JSON.stringify(r2Questions, null, 2));
  
  // Check for questions with actual image data (descType=image or answerType=image)
  const imageQuestions = await prisma.$queryRaw`
    SELECT id, "descImageUrl", "mediaImageUrl", "imageUrl", "optionImages", "answerType", "descType", "mediaType", "audioUrl", "mediaAudioUrl"
    FROM "Question"
    WHERE "descType" = 'image' OR "answerType" = 'image' OR "mediaType" = 'image' OR "mediaType" = 'audio'
    LIMIT 10
  `;
  console.log('Questions with image/audio types:', JSON.stringify(imageQuestions, null, 2));
  
  // Count total questions
  const total = await prisma.$queryRaw`SELECT COUNT(*) as count FROM "Question"`;
  console.log('Total questions:', JSON.stringify(total));
}

main().then(() => prisma.$disconnect()).catch(e => { console.error(e); process.exit(1); });
