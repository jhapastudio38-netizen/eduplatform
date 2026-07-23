"use client";

import { useEffect, useState } from "react";
import PublicSite from "@/components/PublicSite";
import { useAuthStore } from "@/stores/auth";

export default function Home() {
  const { user, fetchUser, loading } = useAuthStore();
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    fetchUser().finally(() => setChecked(true));
  }, [fetchUser]);

  // If admin or teacher is logged in, show admin panel
  if (checked && user && (user.role === "ADMIN" || user.role === "TEACHER")) {
    const AdminApp = require("@/components/admin/AdminApp").default;
    return <AdminApp />;
  }

  // Otherwise show public site
  return <PublicSite />;
}
