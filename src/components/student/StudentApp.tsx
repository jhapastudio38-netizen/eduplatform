"use client";

/**
 * StudentApp — bottom-nav mobile layout with 5 views:
 *   Home  |  Learn  |  Tests  |  Q&A  |  Profile
 *
 * Uses hash-based routing within the SPA so the page URL stays at "/".
 */

import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Home, BookOpen, FileText, MessageSquare, User, LogOut, Video } from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import { Button } from "@/components/ui/button";
import { StudentHome } from "./StudentHome";
import { StudentLearn } from "./StudentLearn";
import { StudentTests } from "./StudentTests";
import { StudentQA } from "./StudentQA";
import { StudentProfile } from "./StudentProfile";
import { StudentVideos } from "./StudentVideos";
import { StudentBooks } from "./StudentBooks";
import { cn } from "@/lib/utils";

type View = "home" | "learn" | "books" | "tests" | "videos" | "qa" | "profile";

const TABS: { id: View; label: string; icon: typeof Home }[] = [
  { id: "home", label: "Home", icon: Home },
  { id: "learn", label: "Learn", icon: BookOpen },
  { id: "books", label: "Books", icon: BookOpen },
  { id: "tests", label: "Tests", icon: FileText },
  { id: "videos", label: "Videos", icon: Video },
  { id: "qa", label: "Q&A", icon: MessageSquare },
  { id: "profile", label: "Profile", icon: User },
];

export function StudentApp() {
  const [view, setView] = useState<View>("home");
  const { user, logout } = useAuthStore();

  // Sync hash → view (read once on mount, then subscribe to changes)
  useEffect(() => {
    const apply = () => {
      const h = window.location.hash.replace(/^#\/?/, "") as View;
      if (TABS.some((t) => t.id === h)) setView(h);
    };
    apply();
    window.addEventListener("hashchange", apply);
    return () => window.removeEventListener("hashchange", apply);
  }, []);

  return (
    <div className="min-h-[100dvh] flex flex-col bg-gradient-to-b from-emerald-50/40 via-background to-background">
      {/* Top bar */}
      <header className="sticky top-0 z-30 backdrop-blur bg-background/80 border-b">
        <div className="max-w-5xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-7 w-7 rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 grid place-items-center text-white">
              <BookOpen className="h-4 w-4" />
            </div>
            <span className="font-semibold">DreamKorea</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground hidden sm:inline">
              Hi, {user?.name || "Student"}
            </span>
            <Button size="sm" variant="ghost" onClick={logout}>
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </header>

      {/* Main */}
      <main className="flex-1 max-w-5xl w-full mx-auto px-4 pb-24 pt-4">
        <AnimatePresence mode="wait">
          <motion.div
            key={view}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            transition={{ duration: 0.2 }}
          >
            {view === "home" && <StudentHome onNavigate={(v) => setView(v as any)} />}
            {view === "learn" && <StudentLearn />}
            {view === "books" && <StudentBooks />}
            {view === "tests" && <StudentTests />}
            {view === "videos" && <StudentVideos />}
            {view === "qa" && <StudentQA />}
            {view === "profile" && <StudentProfile />}
          </motion.div>
        </AnimatePresence>
      </main>

      {/* Bottom nav (mobile-first, also fine on desktop) */}
      <nav className="fixed bottom-0 inset-x-0 z-30 border-t bg-background/95 backdrop-blur pb-[env(safe-area-inset-bottom)]">
        <div className="max-w-5xl mx-auto grid grid-cols-7 h-16">
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
