const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient({
  datasources: { db: { url: 'postgresql://postgres.rupwlrmtcwzqaohbsswj:assissobigsexylady@aws-1-ap-south-1.pooler.supabase.com:5432/postgres' } }
});
async function main() {
  // Find the user
  const user = await prisma.user.findFirst({ where: { email: 'crossagencyhelp@gmail.com' } });
  if (!user) { console.log('No user found'); return; }
  console.log('User ID:', user.id, 'Role:', user.role);
  
  // Create a session
  const session = await prisma.session.create({
    data: {
      userId: user.id,
      token: 'test_token_' + Date.now(),
      expiresAt: new Date(Date.now() + 86400000),
    }
  });
  console.log('Session token:', session.token);
}
main().then(() => prisma.$disconnect()).catch(e => console.error(e));
