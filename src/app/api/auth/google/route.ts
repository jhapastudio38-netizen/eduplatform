/**
 * POST /api/auth/google
 * Body: { idToken?: string, clerkToken?: string }
 *
 * Two modes:
 * 1. Google ID token (original) — verifies with Google's tokeninfo endpoint
 * 2. Clerk session token — verifies with Clerk's backend API
 *
 * Creates/finds user in DB, creates session, returns user credentials.
 */
import { NextRequest, NextResponse } from "next/server";
import { db } from "@/lib/db";
import { createSession } from "@/lib/session";
import { audit } from "@/lib/audit";
import { hashPassword } from "@/lib/password";

const CLERK_SECRET_KEY = "sk_test_5wKIkfvGgq5NMuDvcSTDg0Pp0NWaBX7nU13b8zpgF8";

export async function POST(req: NextRequest) {
  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const { idToken, clerkToken } = body as { idToken?: string; clerkToken?: string };

  // ─── Mode 1: Clerk session token ───
  if (clerkToken) {
    try {
      // Verify the Clerk session token by calling Clerk's backend API
      const clerkResp = await fetch("https://api.clerk.com/v1/users/me", {
        headers: {
          Authorization: `Bearer ${clerkToken}`,
          "Content-Type": "application/json",
        },
      });

      if (!clerkResp.ok) {
        // Try verifying as a session token instead
        const sessionResp = await fetch(`https://api.clerk.com/v1/sessions/verify`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${CLERK_SECRET_KEY}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ token: clerkToken }),
        });

        if (!sessionResp.ok) {
          return NextResponse.json({ error: "Invalid Clerk token" }, { status: 401 });
        }

        const sessionData = await sessionResp.json();
        const userId = sessionData.user_id;

        // Get user details from Clerk
        const userResp = await fetch(`https://api.clerk.com/v1/users/${userId}`, {
          headers: {
            Authorization: `Bearer ${CLERK_SECRET_KEY}`,
          },
        });

        if (!userResp.ok) {
          return NextResponse.json({ error: "Could not fetch Clerk user" }, { status: 502 });
        }

        const clerkUser = await userResp.json();
        const email = clerkUser.email_addresses?.[0]?.email_address?.toLowerCase().trim();

        if (!email) {
          return NextResponse.json({ error: "No email found in Clerk account" }, { status: 400 });
        }

        const name = `${clerkUser.first_name || ""} ${clerkUser.last_name || ""}`.trim() || email.split("@")[0];
        const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

        // Find or create user
        let user = await db.user.findFirst({ where: { email } });

        if (!user) {
          const randomPassword = crypto.randomUUID() + crypto.randomUUID();
          const passwordHash = await hashPassword(randomPassword);
          user = await db.user.create({
            data: {
              email,
              name,
              passwordHash,
              role: "STUDENT",
              isVerified: true,
              isBanned: false,
              signupMethod: "google",
            },
          });
        } else {
          if (user.isBanned) {
            return NextResponse.json({ error: "Account is suspended" }, { status: 403 });
          }
        }

        await createSession(user.id);
        await audit({
          actorId: user.id,
          action: "login_google",
          entity: "User",
          entityId: user.id,
          ip,
        });

        return NextResponse.json({
          ok: true,
          user: {
            id: user.id,
            name: user.name,
            email: user.email,
            phone: user.phone,
            role: user.role,
            username: user.username,
          },
        });
      }

      // Direct /users/me response
      const clerkUser = await clerkResp.json();
      const email = clerkUser.email_addresses?.[0]?.email_address?.toLowerCase().trim();

      if (!email) {
        return NextResponse.json({ error: "No email in Clerk account" }, { status: 400 });
      }

      const name = `${clerkUser.first_name || ""} ${clerkUser.last_name || ""}`.trim() || email.split("@")[0];
      const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

      let user = await db.user.findFirst({ where: { email } });

      if (!user) {
        const randomPassword = crypto.randomUUID() + crypto.randomUUID();
        const passwordHash = await hashPassword(randomPassword);
        user = await db.user.create({
          data: {
            email,
            name,
            passwordHash,
            role: "STUDENT",
            isVerified: true,
            isBanned: false,
            signupMethod: "google",
          },
        });
      }

      await createSession(user.id);
      await audit({
        actorId: user.id,
        action: "login_google",
        entity: "User",
        entityId: user.id,
        ip,
      });

      return NextResponse.json({
        ok: true,
        user: {
          id: user.id,
          name: user.name,
          email: user.email,
          phone: user.phone,
          role: user.role,
          username: user.username,
        },
      });
    } catch (e) {
      return NextResponse.json({ error: "Clerk verification failed" }, { status: 502 });
    }
  }

  // ─── Mode 2: Google ID token (original flow) ───
  if (!idToken || typeof idToken !== "string") {
    return NextResponse.json({ error: "Missing token" }, { status: 400 });
  }

  let googleUser: { email?: string; name?: string; picture?: string; sub?: string };
  try {
    const resp = await fetch(
      `https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`,
    );
    if (!resp.ok) {
      return NextResponse.json({ error: "Invalid Google token" }, { status: 401 });
    }
    googleUser = await resp.json();
  } catch {
    return NextResponse.json({ error: "Could not verify Google token" }, { status: 502 });
  }

  const email = googleUser.email?.toLowerCase().trim();
  if (!email) {
    return NextResponse.json({ error: "Google account has no email" }, { status: 400 });
  }

  const name = googleUser.name || email.split("@")[0];
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0] || "unknown";

  let user = await db.user.findFirst({ where: { email } });

  if (!user) {
    const randomPassword = crypto.randomUUID() + crypto.randomUUID();
    const passwordHash = await hashPassword(randomPassword);
    user = await db.user.create({
      data: {
        email,
        name,
        passwordHash,
        role: "STUDENT",
        isVerified: true,
        isBanned: false,
        signupMethod: "google",
      },
    });
  } else {
    if (user.isBanned) {
      return NextResponse.json({ error: "Account is suspended" }, { status: 403 });
    }
  }

  await createSession(user.id);
  await audit({
    actorId: user.id,
    action: "login_google",
    entity: "User",
    entityId: user.id,
    ip,
  });

  return NextResponse.json({
    ok: true,
    user: {
      id: user.id,
      name: user.name,
      email: user.email,
      phone: user.phone,
      role: user.role,
      username: user.username,
    },
  });
}
