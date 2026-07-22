"use client";

/**
 * AppShell — handles logged-in teacher/admin dashboard.
 * If a STUDENT somehow logs in on web, they're redirected to download the app.
 * If not logged in, redirect to the public website (root page handles this).
 */

import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Toaster } from "@/components/ui/sonner";
import { useAuthStore } from "@/stores/auth";
import { TeacherApp } from "@/components/teacher/TeacherApp";
import { AdminApp } from "@/components/admin/AdminApp";
import { Smartphone, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AppShell() {
  const { user, loading, fetchUser, logout } = useAuthStore();
  const [booted, setBooted] = useState(false);

  useEffect(() => {
    fetchUser().finally(() => setBooted(true));
  }, [fetchUser]);

  // Check if we're on the dashboard route (hash-based)
  const isDashboard = typeof window !== "undefined" && window.location.hash.startsWith("#/dashboard");

  if (!isDashboard && !booted) {
    return null; // Public site handles the root
  }

  if (!booted || loading) {
    return (
      <div className="min-h-screen grid place-items-center bg-white">
        <div className="h-10 w-10 rounded-full border-2 border-blue-600 border-t-transparent animate-spin" />
      </div>
    );
  }

  // Students can't use the web dashboard
  if (user?.role === "STUDENT") {
    return (
      <div className="min-h-screen grid place-items-center bg-slate-50 p-6">
        <div className="max-w-md text-center bg-white rounded-3xl shadow-xl p-8">
          <div className="h-16 w-16 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 grid place-items-center text-white mx-auto mb-4">
            <Smartphone className="h-8 w-8" />
          </div>
          <h1 className="text-2xl font-bold mb-2">Use the Mobile App</h1>
          <p className="text-sm text-slate-500 mb-6">
            Students use the DreamKorea mobile app. This web dashboard is for teachers and admins only.
          </p>
          <Button className="w-full mb-2" onClick={() => window.open("https://github.com/jhapastudio38-netizen/eduplatform/actions/runs/29900757191")}>
            <Smartphone className="mr-2 h-4 w-4" /> Download App
          </Button>
          <Button variant="ghost" className="w-full" onClick={logout}>
            <LogOut className="mr-2 h-4 w-4" /> Sign out
          </Button>
        </div>
      </div>
    );
  }

  if (!user) {
    // Not logged in — redirect to public site
    if (typeof window !== "undefined") {
      window.location.href = "/";
    }
    return null;
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      <Toaster richColors position="top-center" />
      <AnimatePresence mode="wait">
        <motion.div
          key={user.role}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -8 }}
          transition={{ duration: 0.25 }}
          className="flex-1"
        >
          {user.role === "TEACHER" && <TeacherApp />}
          {user.role === "ADMIN" && <AdminApp />}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}
