const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});
async function main() {
  const otps = await prisma.$queryRaw`SELECT code FROM "OtpCode" WHERE contact = 'crossagencyhelp@gmail.com' AND consumed = false ORDER BY "createdAt" DESC LIMIT 1`;
  otps.forEach(o => console.log(o.code));
}
main().then(() => prisma.$disconnect()).catch(e => console.error(e));
