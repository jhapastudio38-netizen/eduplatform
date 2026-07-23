# Dockerfile for EduPlatform Next.js backend.
# Multi-stage build: build → runtime.
# Output image: standalone Next.js server on port 3000.

# ─── Build stage ────────────────────────────────────────────────────────────
FROM oven/bun:1 AS build
WORKDIR /app

# Install deps (cached layer)
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile

# Copy source and build
COPY . .
# Generate Prisma client BEFORE building Next.js
RUN bun run db:generate
RUN bun run build

# ─── Runtime stage ──────────────────────────────────────────────────────────
FROM oven/bun:1-slim AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# Copy standalone server + static + public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

EXPOSE 3000
CMD ["bun", "server.js"]
