"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import { toast } from "sonner";

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

  // Surface any uncaught JS error as a toast so the user sees something
  // useful instead of a silently broken button.
  useEffect(() => {
    const onErr = (e: ErrorEvent) => {
      console.error("[admin-panel] uncaught error:", e.error || e.message);
      toast.error("JS error: " + (e.message || "unknown"), { duration: 8000 });
    };
    const onRejection = (e: PromiseRejectionEvent) => {
      console.error("[admin-panel] unhandled rejection:", e.reason);
      toast.error("Promise error: " + (e.reason?.message || String(e.reason)), { duration: 8000 });
    };
    window.addEventListener("error", onErr);
    window.addEventListener("unhandledrejection", onRejection);
    return () => {
      window.removeEventListener("error", onErr);
      window.removeEventListener("unhandledrejection", onRejection);
    };
  }, []);

  if (!ready) return null;
  if (!allowed) return null;
  return <AdminApp />;
}
