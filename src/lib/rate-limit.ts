/**
 * In-memory rate limiter (per-process).
 *
 * For 100k concurrency on AWS, replace with Redis-backed limiter:
 *   - Use `@upstash/redis` or `ioredis` with sliding window algorithm.
 *   - The API surface stays the same: rateLimit(key, max, windowSec).
 *
 * This implementation is correct for single-instance deployments and
 * acceptable for development / staging. The interface is Redis-compatible
 * so swapping is a one-line change.
 */

type Bucket = { count: number; resetAt: number };

const store = new Map<string, Bucket>();

// Periodically evict expired buckets to prevent memory leak
if (typeof setInterval !== "undefined") {
  setInterval(() => {
    const now = Date.now();
    for (const [k, b] of store) {
      if (b.resetAt < now) store.delete(k);
    }
  }, 60_000).unref?.();
}

export interface RateLimitResult {
  ok: boolean;
  remaining: number;
  resetAt: number;
}

export function rateLimit(
  key: string,
  max: number,
  windowSec: number,
): RateLimitResult {
  const now = Date.now();
  const existing = store.get(key);

  if (!existing || existing.resetAt < now) {
    const resetAt = now + windowSec * 1000;
    store.set(key, { count: 1, resetAt });
    return { ok: true, remaining: max - 1, resetAt };
  }

  if (existing.count >= max) {
    return { ok: false, remaining: 0, resetAt: existing.resetAt };
  }

  existing.count += 1;
  return {
    ok: true,
    remaining: max - existing.count,
    resetAt: existing.resetAt,
  };
}

// Convenience: returns a 429-style flag for API routes
export function rateLimited(
  key: string,
  max: number,
  windowSec: number,
): boolean {
  return !rateLimit(key, max, windowSec).ok;
}
