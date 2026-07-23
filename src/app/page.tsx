"use client";

import { useEffect, useState } from "react";
import PublicSite from "@/components/PublicSite";
import AdminApp from "@/components/admin/AdminApp";

export default function Home() {
  const [user, setUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/auth/me")
      .then((r) => r.json())
      .then((d) => setUser(d.user || null))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return null;

  if (user && (user.role === "ADMIN" || user.role === "TEACHER")) {
    return <AdminApp />;
  }

  return <PublicSite />;
}
