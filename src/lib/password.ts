/**
 * Password hashing utilities using Node's built-in scrypt (no external deps).
 * Used by teachers + admins (students use OTP).
 */
import { scrypt as scryptCallback, randomBytes, timingSafeEqual } from "crypto";
import { promisify } from "util";

const scrypt = promisify(scryptCallback) as (
  password: string | Buffer,
  salt: Buffer,
  keylen: number,
  options?: { N?: number; r?: number; p?: number }
) => Promise<Buffer>;

const KEYLEN = 64;
const N = 16384; // CPU/memory cost
const r = 8;
const p = 1;

/**
 * Hash a password. Returns a string of the form:
 *   "scrypt:N:r:p:<saltHex>:<hashHex>"
 */
export async function hashPassword(password: string): Promise<string> {
  if (!password || password.length < 6) {
    throw new Error("Password must be at least 6 characters");
  }
  const salt = randomBytes(16);
  const hash = await scrypt(password, salt, KEYLEN, { N, r, p });
  return `scrypt:${N}:${r}:${p}:${salt.toString("hex")}:${hash.toString("hex")}`;
}

/**
 * Verify a password against a stored hash.
 * Constant-time comparison via timingSafeEqual.
 */
export async function verifyPassword(
  password: string,
  stored: string
): Promise<boolean> {
  try {
    const parts = stored.split(":");
    if (parts.length !== 6 || parts[0] !== "scrypt") return false;
    const N_ = parseInt(parts[1], 10);
    const r_ = parseInt(parts[2], 10);
    const p_ = parseInt(parts[3], 10);
    const salt = Buffer.from(parts[4], "hex");
    const expectedHash = Buffer.from(parts[5], "hex");
    const hash = await scrypt(password, salt, expectedHash.length, {
      N: N_,
      r: r_,
      p: p_,
    });
    return timingSafeEqual(hash, expectedHash);
  } catch {
    return false;
  }
}
