/**
 * Load test for 100k concurrency simulation.
 *
 * Strategy: We can't actually run 100k concurrent requests from this sandbox,
 * but we can:
 *   1. Fire N parallel requests with controlled concurrency
 *   2. Measure latency distribution + error rate
 *   3. Verify the rate limiter doesn't break under load
 *   4. Identify hot paths that would need to scale (DB queries, API routes)
 *
 * For a TRUE 100k test, deploy to AWS and use k6 / Artillery / Locust
 * from multiple regions. This script gives you a quick smoke test.
 *
 * Run: bun run scripts/load-test.ts
 */

const BASE = process.env.BASE_URL || "http://localhost:3000";
const CONCURRENCY = Number(process.env.LOAD_CONCURRENCY) || 50;
const TOTAL = Number(process.env.LOAD_TOTAL) || 500;

interface Sample { status: number; ms: number; ok: boolean; }

async function fire(url: string, init: RequestInit = {}): Promise<Sample> {
  const t0 = performance.now();
  try {
    const res = await fetch(`${BASE}${url}`, init);
    await res.text();
    return { status: res.status, ms: performance.now() - t0, ok: res.status < 500 };
  } catch {
    return { status: 0, ms: performance.now() - t0, ok: false };
  }
}

async function runPool(tasks: (() => Promise<Sample>)[], concurrency: number): Promise<Sample[]> {
  const results: Sample[] = [];
  let idx = 0;
  const workers = Array.from({ length: concurrency }, async () => {
    while (idx < tasks.length) {
      const i = idx++;
      results[i] = await tasks[i]();
    }
  });
  await Promise.all(workers);
  return results;
}

function pct(arr: number[], p: number): number {
  if (arr.length === 0) return 0;
  const sorted = [...arr].sort((a, b) => a - b);
  return sorted[Math.floor(sorted.length * p)];
}

async function main() {
  console.log(`Load test against ${BASE}`);
  console.log(`Concurrency=${CONCURRENCY}  Total=${TOTAL}\n`);

  // Test 1: Public read endpoint (no auth, no rate limit on the response itself)
  console.log("Test 1: GET /api/student/subjects (read-only, public)");
  const t1 = await runPool(
    Array.from({ length: TOTAL }, () => () => fire("/api/student/subjects")),
    CONCURRENCY,
  );
  report(t1);

  // Test 2: Auth-protected endpoint (rejected without session, fast path)
  console.log("\nTest 2: GET /api/auth/me (auth check, fast 401 path)");
  const t2 = await runPool(
    Array.from({ length: TOTAL }, () => () => fire("/api/auth/me")),
    CONCURRENCY,
  );
  report(t2);

  // Test 3: Home page render
  console.log("\nTest 3: GET / (full page render)");
  const t3 = await runPool(
    Array.from({ length: Math.min(TOTAL, 100) }, () => () => fire("/")),
    Math.min(CONCURRENCY, 20),
  );
  report(t3);

  console.log("\n--- Scaling notes for 100k concurrent users ---");
  console.log("• SQLite is single-writer. For 100k users, migrate to Aurora Postgres / DynamoDB.");
  console.log("• In-memory rate limiter is per-process. Use Redis (`@upstash/redis`) for multi-instance.");
  console.log("• Run Next.js behind ALB + multiple Fargate/EKS instances (target ~500 req/s per instance).");
  console.log("• Use CloudFront for static assets + edge caching of public API responses.");
  console.log("• WebSocket live class: add `@socket.io/redis-adapter` for horizontal scaling.");
  console.log("• Database: enable RDS Proxy, read replicas, connection pooling (PgBouncer).");
  console.log("• Cache hot reads (chapter listings, test listings) with `Cache-Control: s-maxage=60`.");
}

function report(samples: Sample[]) {
  const ms = samples.map((s) => s.ms);
  const ok = samples.filter((s) => s.ok).length;
  const errors = samples.length - ok;
  const avg = ms.reduce((a, b) => a + b, 0) / ms.length;
  console.log(`  Total: ${samples.length}`);
  console.log(`  Success: ${ok}  Errors: ${errors}  Error rate: ${(errors / samples.length * 100).toFixed(2)}%`);
  console.log(`  Latency (ms):  avg=${avg.toFixed(1)}  p50=${pct(ms, 0.5).toFixed(1)}  p95=${pct(ms, 0.95).toFixed(1)}  p99=${pct(ms, 0.99).toFixed(1)}`);
}

main().catch(console.error);
