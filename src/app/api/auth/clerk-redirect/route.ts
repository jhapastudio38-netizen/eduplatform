/**
 * GET /api/auth/clerk-redirect
 *
 * After Clerk sign-in completes on the web, this route captures the Clerk session
 * and redirects back to the Android app via deep link with the session token.
 */
import { NextResponse } from "next/server";
import { auth, currentUser } from "@clerk/nextjs/server";
import { db } from "@/lib/db";
import { createSession } from "@/lib/session";
import { hashPassword } from "@/lib/password";

export async function GET(req: Request) {
  try {
    // Get the Clerk user
    const user = await currentUser();
    if (!user) {
      return NextResponse.redirect(new URL("/sign-in", req.url));
    }

    const email = user.emailAddresses?.[0]?.emailAddress?.toLowerCase().trim();
    if (!email) {
      return new Response("No email found in Clerk account", { status: 400 });
    }

    const name = `${user.firstName || ""} ${user.lastName || ""}`.trim() || email.split("@")[0];

    // Find or create user in our database
    let dbUser = await db.user.findFirst({ where: { email } });

    if (!dbUser) {
      const randomPassword = crypto.randomUUID() + crypto.randomUUID();
      const passwordHash = await hashPassword(randomPassword);
      dbUser = await db.user.create({
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

    // Create our app session
    await createSession(dbUser.id);

    // Build the user data to pass back to the app
    const userData = {
      id: dbUser.id,
      name: dbUser.name,
      email: dbUser.email,
      phone: dbUser.phone,
      role: dbUser.role,
    };

    // Redirect to the Android app via deep link with user data
    const redirectUrl = `dreamkorea://auth-callback?userId=${encodeURIComponent(dbUser.id)}&name=${encodeURIComponent(dbUser.name || "")}&email=${encodeURIComponent(dbUser.email)}&phone=${encodeURIComponent(dbUser.phone || "")}&role=${encodeURIComponent(dbUser.role)}`;

    return NextResponse.redirect(redirectUrl);
  } catch (e) {
    console.error("Clerk redirect error:", e);
    return new Response("Authentication failed", { status: 500 });
  }
}
