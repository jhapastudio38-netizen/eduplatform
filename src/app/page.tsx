"use client";

/**
 * Root page — shows the PUBLIC WEBSITE (viewer only).
 *
 * Students use the mobile app.
 * Teachers login at /teacher-login/<token>
 * Admins login at /admin-login/<token>
 *
 * The public website shows institute info, features, contact — NO system access.
 */

import { PublicSite } from "@/components/PublicSite";

export default function Home() {
  return <PublicSite />;
}
