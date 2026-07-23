"use client";

import { useEffect } from "react";
import PublicSite from "@/components/PublicSite";

export default function Home() {
  useEffect(() => {
    // Check if user is admin/teacher and redirect
    fetch("/api/auth/me")
      .then((r) => r.json())
      .then((d) => {
        if (d.user && (d.user.role === "ADMIN" || d.user.role === "TEACHER")) {
          window.location.href = "/admin-panel";
        }
      })
      .catch(() => {});
  }, []);

  return <PublicSite />;
}
