const { PrismaClient } = require('@prisma/client');
const crypto = require('crypto');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});
async function main() {
  const user = await prisma.user.findFirst({ where: { email: 'crossagencyhelp@gmail.com' } });
  if (!user) { console.log('No user'); return; }
  const rawToken = 'mytesttoken1234567890';
  const hashedToken = crypto.createHash('sha256').update(rawToken).digest('hex');
  await prisma.session.create({
    data: { userId: user.id, token: hashedToken, expiresAt: new Date(Date.now() + 86400000) }
  });
  console.log('Raw token (use as cookie):', rawToken);
  console.log('Hashed token (stored in DB):', hashedToken);
}
main().then(() => prisma.$disconnect()).catch(e => console.error(e));
