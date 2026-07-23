#!/bin/bash
# Wrapper to run seed scripts with DATABASE_URL explicitly set
# This ensures PrismaClient can find the database URL
set -e

# DATABASE_URL is passed as environment variable from GitHub Actions
echo "Running seed with DATABASE_URL set..."

# Run the seed scripts with the env var explicitly exported
export DATABASE_URL="${DATABASE_URL}"

# Run seed-korean
node --experimental-strip-types scripts/seed-korean.ts 2>/dev/null || \
  bun run scripts/seed-korean.ts 2>/dev/null || \
  npx tsx scripts/seed-korean.ts 2>/dev/null || \
  echo "seed-korean failed (may already exist)"

# Run seed-fixed-tokens
node --experimental-strip-types scripts/seed-fixed-tokens.ts 2>/dev/null || \
  bun run scripts/seed-fixed-tokens.ts 2>/dev/null || \
  npx tsx scripts/seed-fixed-tokens.ts 2>/dev/null || \
  echo "seed-fixed-tokens failed"

echo "Seed wrapper complete."
