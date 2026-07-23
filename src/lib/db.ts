/**
 * Database client — lazily initialized.
 * Prisma is only loaded when a DB query is actually needed.
 * This allows the server to start even if Prisma has issues.
 */
import type { PrismaClient } from '@prisma/client'

let _prisma: PrismaClient | null = null
let _initFailed = false

function getPrisma(): PrismaClient {
  if (_prisma) return _prisma
  if (_initFailed) {
    throw new Error('Database connection failed. Please try again later.')
  }
  try {
    const { PrismaClient } = require('@prisma/client')
    _prisma = new PrismaClient({
      log: process.env.NODE_ENV === 'development' ? ['query'] : [],
    })
    return _prisma
  } catch (e) {
    _initFailed = true
    console.error('Prisma initialization failed:', e)
    throw e
  }
}

// Proxy that lazily creates the Prisma client on first use
export const db = new Proxy({} as PrismaClient, {
  get(_target, prop) {
    const prisma = getPrisma()
    const value = (prisma as any)[prop]
    if (typeof value === 'function') {
      return value.bind(prisma)
    }
    return value
  },
})
