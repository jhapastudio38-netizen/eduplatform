# Dockerfile — use Node.js instead of Bun for runtime (Prisma compatibility)
FROM oven/bun:1 AS build
WORKDIR /app
COPY package.json bun.lock ./
RUN bun install --frozen-lockfile
COPY . .
RUN bun run db:generate
RUN bun run build

# Use Node.js for runtime (Prisma works better with Node)
FROM node:20-slim AS runtime
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

# Copy Prisma client
RUN mkdir -p /app/node_modules
COPY --from=build /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=build /app/node_modules/@prisma ./node_modules/@prisma
COPY --from=build /app/prisma ./prisma

EXPOSE 3000
CMD ["node", "server.js"]
