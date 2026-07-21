"use client";

/**
 * AppShell — teacher/admin web app only.
 *
 * Students must use the native mobile app (Rust + Slint, see /student-app-rust).
 * If a STUDENT role logs in here, they are redirected to download the app.
 */

import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Toaster } from "@/components/ui/sonner";
import { useAuthStore } from "@/stores/auth";
import { AuthFlow } from "@/components/auth/AuthFlow";
import { TeacherApp } from "@/components/teacher/TeacherApp";
import { AdminApp } from "@/components/admin/AdminApp";
import { Smartphone, LogOut, Download } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AppShell() {
  const { user, loading, fetchUser, logout } = useAuthStore();
  const [booted, setBooted] = useState(false);

  useEffect(() => {
    fetchUser().finally(() => setBooted(true));
  }, [fetchUser]);

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <Toaster richColors position="top-center" />
      <AnimatePresence mode="wait">
        {!booted || loading ? (
          <motion.div
            key="boot"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex-1 grid place-items-center"
          >
            <div className="flex flex-col items-center gap-3">
              <div className="h-10 w-10 rounded-full border-2 border-primary border-t-transparent animate-spin" />
              <p className="text-sm text-muted-foreground">Loading EduPlatform…</p>
            </div>
          </motion.div>
        ) : !user ? (
          <motion.div
            key="auth"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.25 }}
            className="flex-1"
          >
            <AuthFlow />
          </motion.div>
        ) : user.role === "STUDENT" ? (
          <motion.div
            key="student-blocked"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            className="flex-1 grid place-items-center p-6 bg-gradient-to-br from-slate-50 to-slate-100"
          >
            <div className="max-w-md text-center bg-white rounded-3xl shadow-xl p-8">
              <div className="h-16 w-16 rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 grid place-items-center text-white mx-auto mb-4">
                <Smartphone className="h-8 w-8" />
              </div>
              <h1 className="text-2xl font-bold mb-2">Get the mobile app</h1>
              <p className="text-sm text-muted-foreground mb-6">
                Students use the native EduPlatform mobile app for the best learning experience.
                This web dashboard is for teachers and admins only.
              </p>
              <div className="flex flex-col gap-2">
                <a href="https://play.google.com/store/apps/details?id=app.eduplatform.student" target="_blank" rel="noreferrer">
                  <Button className="w-full">
                    <Download className="mr-2 h-4 w-4" /> Download for Android
                  </Button>
                </a>
                <a href="https://apps.apple.com/app/eduplatform/id000000000" target="_blank" rel="noreferrer">
                  <Button variant="outline" className="w-full">
                    <Download className="mr-2 h-4 w-4" /> Download for iOS
                  </Button>
                </a>
                <Button variant="ghost" className="w-full mt-2" onClick={logout}>
                  <LogOut className="mr-2 h-4 w-4" /> Sign out
                </Button>
              </div>
            </div>
          </motion.div>
        ) : (
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
        )}
      </AnimatePresence>
    </div>
  );
}
