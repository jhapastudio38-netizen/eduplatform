"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";

const AdminApp = dynamic(() => import("@/components/admin/AdminApp").then(m => ({ default: m.AdminApp })), { ssr: false });

export default function AdminPanelPage() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [allowed, setAllowed] = useState(false);

  useEffect(() => {
    fetch("/api/auth/me")
      .then((r) => r.json())
      .then((d) => {
        if (d.user && (d.user.role === "ADMIN" || d.user.role === "TEACHER")) {
          setAllowed(true);
        } else {
          router.push("/admin");
        }
      })
      .catch(() => router.push("/admin"))
      .finally(() => setReady(true));
  }, [router]);

  if (!ready) return null;
  if (!allowed) return null;
  return <AdminApp />;
}
