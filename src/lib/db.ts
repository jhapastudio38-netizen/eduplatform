/**
 * Database client — lazy initialization with dynamic import.
 * The server can start without Prisma. Prisma is only loaded on first DB query.
 */

let _prisma: any = null;

async function getPrisma() {
  if (_prisma) return _prisma;
  try {
    const { PrismaClient } = await import('@prisma/client');
    _prisma = new PrismaClient({
      log: process.env.NODE_ENV === 'development' ? ['query'] : [],
    });
    return _prisma;
  } catch (e) {
    console.error('Prisma init failed:', e);
    throw e;
  }
}

// Synchronous proxy — queues calls until Prisma is ready
const _pending: Array<{ prop: string; args: any[]; resolve: Function; reject: Function }> = [];
let _ready = false;

async function init() {
  if (_ready) return;
  try {
    _prisma = await getPrisma();
    _ready = true;
    // Process pending calls
    for (const p of _pending) {
      try {
        const result = await _prisma[p.prop](...p.args);
        p.resolve(result);
      } catch (e) {
        p.reject(e);
      }
    }
    _pending.length = 0;
  } catch (e) {
    // Reject all pending
    for (const p of _pending) p.reject(e);
    _pending.length = 0;
    throw e;
  }
}

// Create a proxy that returns async functions
export const db = new Proxy({} as any, {
  get(_target, prop) {
    return async function (...args: any[]) {
      if (!_prisma) {
        await init();
      }
      const fn = _prisma[prop];
      if (typeof fn === 'function') {
        return fn.apply(_prisma, args);
      }
      return fn;
    };
  },
});
