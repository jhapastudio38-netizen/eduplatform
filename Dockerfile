# Dockerfile — Node.js with OpenSSL for Prisma
FROM node:20-slim AS build
WORKDIR /app

# Install OpenSSL for Prisma
RUN apt-get update && apt-get install -y openssl && rm -rf /var/lib/apt/lists/*

# Install dependencies
COPY package.json bun.lock ./
RUN npm install --legacy-peer-deps

# Copy source
COPY . .

# Generate Prisma client
RUN npx prisma generate

# Build Next.js (copy Prisma into standalone)
RUN npx next build && cp -r .next/static .next/standalone/.next/ && cp -r public .next/standalone/ && mkdir -p .next/standalone/node_modules && cp -r node_modules/.prisma .next/standalone/node_modules/.prisma/ && cp -r node_modules/@prisma .next/standalone/node_modules/@prisma/ && cp -r prisma .next/standalone/prisma/

# Runtime — with OpenSSL
FROM node:20-slim AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# Install OpenSSL for Prisma runtime
RUN apt-get update && apt-get install -y openssl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

# Prisma client
RUN mkdir -p /app/node_modules
COPY --from=build /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=build /app/node_modules/@prisma ./node_modules/@prisma
COPY --from=build /app/prisma ./prisma

EXPOSE 3000
CMD ["node", "server.js"]
