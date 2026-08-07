const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});

async function main() {
  const otps = await prisma.$queryRaw`
    SELECT contact, code, "createdAt", consumed
    FROM "OtpCode"
    WHERE contact = 'crossagencyhelp@gmail.com'
    ORDER BY "createdAt" DESC
    LIMIT 3
  `;
  otps.forEach(o => console.log('Code:', o.code, 'Consumed:', o.consumed, 'Created:', o.createdAt));
}

main().then(() => prisma.$disconnect()).catch(e => { console.error(e); process.exit(1); });
