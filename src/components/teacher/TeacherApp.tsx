"use client";

import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Home, Radio, Calendar, Users, LogOut } from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import { Button } from "@/components/ui/button";
import { TeacherDashboard } from "./TeacherDashboard";
import { TeacherLive } from "./TeacherLive";
import { TeacherSchedule } from "./TeacherSchedule";
import { TeacherStudents } from "./TeacherStudents";
import { cn } from "@/lib/utils";

type View = "home" | "live" | "schedule" | "students";

const TABS: { id: View; label: string; icon: typeof Home }[] = [
  { id: "home", label: "Home", icon: Home },
  { id: "live", label: "Live", icon: Radio },
  { id: "schedule", label: "Schedule", icon: Calendar },
  { id: "students", label: "Students", icon: Users },
];

export function TeacherApp() {
  const [view, setView] = useState<View>("home");
  const { user, logout } = useAuthStore();

  useEffect(() => {
    const onHash = () => {
      const h = window.location.hash.replace(/^#\/?/, "") as View;
      if (TABS.some((t) => t.id === h)) setView(h);
    };
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  return (
    <div className="min-h-[100dvh] flex flex-col bg-gradient-to-b from-amber-50/40 via-background to-background">
      <header className="sticky top-0 z-30 backdrop-blur bg-background/80 border-b">
        <div className="max-w-5xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-7 w-7 rounded-lg bg-gradient-to-br from-amber-500 to-orange-600 grid place-items-center text-white">
              <Radio className="h-4 w-4" />
            </div>
            <span className="font-semibold">EduPlatform · Teacher</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground hidden sm:inline">
              {user?.name || "Teacher"}
            </span>
            <Button size="sm" variant="ghost" onClick={logout}>
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-5xl w-full mx-auto px-4 pb-24 pt-4">
        <AnimatePresence mode="wait">
          <motion.div
            key={view}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: 0.2 }}
          >
            {view === "home" && <TeacherDashboard onNavigate={(v) => setView(v)} />}
            {view === "live" && <TeacherLive />}
            {view === "schedule" && <TeacherSchedule />}
            {view === "students" && <TeacherStudents />}
          </motion.div>
        </AnimatePresence>
      </main>

      <nav className="fixed bottom-0 inset-x-0 z-30 border-t bg-background/95 backdrop-blur">
        <div className="max-w-5xl mx-auto grid grid-cols-4 h-16">
          {TABS.map((t) => {
            const Icon = t.icon;
            const active = view === t.id;
            return (
              <button
                key={t.id}
                onClick={() => {
                  setView(t.id);
                  history.replaceState(null, "", `#/${t.id}`);
                }}
                className={cn(
                  "flex flex-col items-center justify-center gap-1 text-xs transition-colors",
                  active ? "text-primary" : "text-muted-foreground hover:text-foreground",
                )}
              >
                <Icon className={cn("h-5 w-5 transition-transform", active && "scale-110")} />
                <span>{t.label}</span>
              </button>
            );
          })}
        </div>
      </nav>
    </div>
  );
}
