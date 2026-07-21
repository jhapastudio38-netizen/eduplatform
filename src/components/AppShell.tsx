"use client";

/**
 * AppShell — single-page app shell that swaps between auth, student, teacher,
 * and admin views based on the current user's role.
 */

import { useEffect, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Toaster } from "@/components/ui/sonner";
import { useAuthStore } from "@/stores/auth";
import { AuthFlow } from "@/components/auth/AuthFlow";
import { StudentApp } from "@/components/student/StudentApp";
import { TeacherApp } from "@/components/teacher/TeacherApp";
import { AdminApp } from "@/components/admin/AdminApp";

export default function AppShell() {
  const { user, loading, fetchUser } = useAuthStore();
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
        ) : (
          <motion.div
            key={user.role}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.25 }}
            className="flex-1"
          >
            {user.role === "STUDENT" && <StudentApp />}
            {user.role === "TEACHER" && <TeacherApp />}
            {user.role === "ADMIN" && <AdminApp />}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
