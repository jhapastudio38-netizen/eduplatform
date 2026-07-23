# Dockerfile for EduPlatform Next.js backend.
FROM oven/bun:1 AS build
WORKDIR /app
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile
COPY . .
RUN bun run db:generate
RUN bun run build

FROM oven/bun:1-slim AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# Copy standalone server
COPY --from=build /app/.next/standalone ./
# Copy static assets
COPY --from=build /app/.next/static ./.next/static
# Copy public assets
COPY --from=build /app/public ./public

# CRITICAL: Copy Prisma client (in case build script didn't)
RUN mkdir -p /app/node_modules/.prisma /app/node_modules/@prisma /app/prisma
COPY --from=build /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=build /app/node_modules/@prisma ./node_modules/@prisma
COPY --from=build /app/prisma ./prisma

EXPOSE 3000
CMD ["bun", "server.js"]
