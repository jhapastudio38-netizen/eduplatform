"use client";

import { useEffect } from "react";
import PublicSite from "@/components/PublicSite";

export default function Home() {
  useEffect(() => {
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
// v9.8.4-force-rebuild 1785768342
