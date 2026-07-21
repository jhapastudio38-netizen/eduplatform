"use client";

import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { LayoutDashboard, BookOpen, FileQuestion, FileText, Sparkles, Users, LogOut } from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { AdminOverview } from "./AdminOverview";
import { AdminContent } from "./AdminContent";
import { AdminQuestions } from "./AdminQuestions";
import { AdminTests } from "./AdminTests";
import { AdminAIGenerate } from "./AdminAIGenerate";
import { AdminUsers } from "./AdminUsers";

type View = "overview" | "content" | "questions" | "tests" | "ai" | "users";

const NAV: { id: View; label: string; icon: typeof LayoutDashboard }[] = [
  { id: "overview", label: "Overview", icon: LayoutDashboard },
  { id: "content", label: "Chapters & Lessons", icon: BookOpen },
  { id: "questions", label: "Questions", icon: FileQuestion },
  { id: "tests", label: "Tests & Exams", icon: FileText },
  { id: "ai", label: "AI Generator", icon: Sparkles },
  { id: "users", label: "Users", icon: Users },
];

export function AdminApp() {
  const [view, setView] = useState<View>("overview");
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, logout } = useAuthStore();

  useEffect(() => {
    const onHash = () => {
      const h = window.location.hash.replace(/^#\/?/, "") as View;
      if (NAV.some((t) => t.id === h)) setView(h);
    };
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  return (
    <div className="min-h-[100dvh] flex bg-slate-50">
      {/* Sidebar — desktop */}
      <aside className="hidden md:flex flex-col w-60 border-r bg-white">
        <div className="h-14 flex items-center gap-2 px-4 border-b">
          <div className="h-7 w-7 rounded-lg bg-gradient-to-br from-rose-500 to-pink-600 grid place-items-center text-white">
            <LayoutDashboard className="h-4 w-4" />
          </div>
          <span className="font-semibold text-sm">Admin Console</span>
        </div>
        <nav className="flex-1 p-2 space-y-1">
          {NAV.map((n) => {
            const Icon = n.icon;
            const active = view === n.id;
            return (
              <button
                key={n.id}
                onClick={() => setView(n.id)}
                className={cn(
                  "w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm transition-colors",
                  active ? "bg-rose-50 text-rose-700 font-medium" : "text-slate-600 hover:bg-slate-100",
                )}
              >
                <Icon className="h-4 w-4" />
                {n.label}
              </button>
            );
          })}
        </nav>
        <div className="p-3 border-t">
          <div className="text-xs text-slate-500 mb-1">Signed in as</div>
          <div className="font-medium text-sm truncate">{user?.name || user?.email}</div>
          <Button size="sm" variant="outline" className="mt-2 w-full" onClick={logout}>
            <LogOut className="mr-1 h-3 w-3" /> Sign out
          </Button>
        </div>
      </aside>

      {/* Mobile header */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="md:hidden h-14 flex items-center justify-between px-4 border-b bg-white">
          <button onClick={() => setMobileOpen(true)} className="text-slate-700">
            <LayoutDashboard className="h-5 w-5" />
          </button>
          <span className="font-semibold text-sm">Admin</span>
          <Button size="sm" variant="ghost" onClick={logout}>
            <LogOut className="h-4 w-4" />
          </Button>
        </header>

        <main className="flex-1 p-4 md:p-8 overflow-y-auto">
          <AnimatePresence mode="wait">
            <motion.div
              key={view}
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.2 }}
            >
              {view === "overview" && <AdminOverview onNavigate={(v) => setView(v)} />}
              {view === "content" && <AdminContent />}
              {view === "questions" && <AdminQuestions />}
              {view === "tests" && <AdminTests />}
              {view === "ai" && <AdminAIGenerate />}
              {view === "users" && <AdminUsers />}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>

      {/* Mobile drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 md:hidden">
          <div className="absolute inset-0 bg-black/30" onClick={() => setMobileOpen(false)} />
          <div className="absolute left-0 top-0 bottom-0 w-64 bg-white p-3 space-y-1">
            {NAV.map((n) => {
              const Icon = n.icon;
              const active = view === n.id;
              return (
                <button
                  key={n.id}
                  onClick={() => { setView(n.id); setMobileOpen(false); }}
                  className={cn(
                    "w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm",
                    active ? "bg-rose-50 text-rose-700 font-medium" : "text-slate-600 hover:bg-slate-100",
                  )}
                >
                  <Icon className="h-4 w-4" />
                  {n.label}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
