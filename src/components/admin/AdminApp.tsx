"use client";

import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  LayoutDashboard, FileText, BookOpen, FileQuestion, Users, ShoppingBag,
  GraduationCap, School, Image, Layers, Package, UserCog, BookMarked,
  ClipboardList, BarChart3, Settings, LogOut, ChevronDown, ChevronRight,
  FolderTree, Library, Award, Bell, Search, Menu, X
} from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { AdminOverview } from "./AdminOverview";
import { AdminContent } from "./AdminContent";
import { AdminQuestions } from "./AdminQuestions";
import { AdminTests } from "./AdminTests";
import { AdminUsers } from "./AdminUsers";
import { AdminPlaceholder } from "./AdminPlaceholder";

type View =
  | "overview" | "exams" | "color-vision" | "demo-exams" | "batch-exams" | "chapter-exams"
  | "question-bank" | "question-categories" | "all-books" | "all-courses"
  | "paid-exam-orders" | "batch-orders" | "course-orders" | "qb-orders"
  | "batch" | "student-results" | "package-results" | "classroom-results"
  | "students" | "teachers" | "pdf-viewer" | "settings"
  | "content" | "questions" | "tests" | "users";

interface NavItem {
  id: View;
  label: string;
  icon: typeof FileText;
  hasAdd?: boolean;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

const NAV_SECTIONS: NavSection[] = [
  {
    title: "Exam Management",
    items: [
      { id: "exams", label: "Exams", icon: FileText, hasAdd: true },
      { id: "color-vision", label: "Color Vision", icon: Image },
      { id: "demo-exams", label: "Demo Exams", icon: FileText, hasAdd: true },
      { id: "batch-exams", label: "All Batch Exams", icon: Layers, hasAdd: true },
      { id: "chapter-exams", label: "Chapter Exams", icon: BookOpen, hasAdd: true },
    ],
  },
  {
    title: "Content & Resources",
    items: [
      { id: "question-bank", label: "Question Bank", icon: FileQuestion, hasAdd: true },
      { id: "question-categories", label: "Question Categories", icon: FolderTree, hasAdd: true },
      { id: "all-books", label: "All Books", icon: BookMarked, hasAdd: true },
      { id: "all-courses", label: "All Courses", icon: Library, hasAdd: true },
    ],
  },
  {
    title: "Orders & Commerce",
    items: [
      { id: "paid-exam-orders", label: "Paid Exam Orders", icon: ShoppingBag, hasAdd: true },
      { id: "batch-orders", label: "Batch Orders", icon: ShoppingBag, hasAdd: true },
      { id: "course-orders", label: "Course Orders", icon: ShoppingBag, hasAdd: true },
      { id: "qb-orders", label: "Question Bank Orders", icon: ShoppingBag, hasAdd: true },
    ],
  },
  {
    title: "User & Batch Administration",
    items: [
      { id: "batch", label: "Batch", icon: Layers, hasAdd: true },
      { id: "student-results", label: "Student Results", icon: BarChart3 },
      { id: "package-results", label: "Package Results", icon: Package },
      { id: "classroom-results", label: "Classroom Results", icon: School },
    ],
  },
  {
    title: "User Roles & Tools",
    items: [
      { id: "students", label: "Student", icon: GraduationCap, hasAdd: true },
      { id: "teachers", label: "Teachers", icon: UserCog, hasAdd: true },
      { id: "all-courses", label: "Courses", icon: BookOpen, hasAdd: true },
      { id: "pdf-viewer", label: "PDF Viewer", icon: FileText },
      { id: "color-vision", label: "Color Vision", icon: Image },
    ],
  },
];

// Map legacy views to new ones for backward compat
const VIEW_MAP: Record<string, View> = {
  overview: "overview",
  content: "question-categories",
  questions: "question-bank",
  tests: "exams",
  users: "students",
};

export function AdminApp() {
  const [view, setView] = useState<View>("overview");
  const [mobileOpen, setMobileOpen] = useState(false);
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(["Exam Management"]));
  const { user, logout } = useAuthStore();

  useEffect(() => {
    const onHash = () => {
      const h = (window.location.hash.replace(/^#\/?/, "") || "overview") as View;
      const mapped = VIEW_MAP[h] || h;
      if (isValidView(mapped)) setView(mapped);
    };
    onHash();
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  function toggleSection(title: string) {
    setExpandedSections(prev => {
      const next = new Set(prev);
      if (next.has(title)) next.delete(title);
      else next.add(title);
      return next;
    });
  }

  function navigate(v: View) {
    setView(v);
    setMobileOpen(false);
    history.replaceState(null, "", `#/${v}`);
  }

  function renderView() {
    switch (view) {
      case "overview":
        return <AdminOverview onNavigate={navigate} />;
      case "question-categories":
      case "all-books":
      case "all-courses":
      case "content":
        return <AdminContent />;
      case "question-bank":
      case "questions":
        return <AdminQuestions />;
      case "exams":
      case "demo-exams":
      case "batch-exams":
      case "chapter-exams":
      case "tests":
        return <AdminTests />;
      case "students":
      case "teachers":
      case "users":
        return <AdminUsers />;
      case "color-vision":
        return <AdminPlaceholder title="Color Vision Test" description="Ishihara-style color vision testing module for student screening." />;
      case "pdf-viewer":
        return <AdminPlaceholder title="PDF Viewer" description="Upload and view PDF documents — textbooks, worksheets, exam papers." />;
      case "batch":
        return <AdminPlaceholder title="Batch Management" description="Create and manage student batches / cohorts. Assign exams and courses to batches." />;
      case "student-results":
        return <AdminPlaceholder title="Student Results" description="Aggregated performance data across all students and exams." />;
      case "package-results":
        return <AdminPlaceholder title="Package Results" description="Results for bundled exam packages." />;
      case "classroom-results":
        return <AdminPlaceholder title="Classroom Results" description="Performance data for virtual and physical classrooms." />;
      case "paid-exam-orders":
      case "batch-orders":
      case "course-orders":
      case "qb-orders":
        return <AdminPlaceholder title="Orders" description="Track and manage purchases for exams, courses, and question bank access." />;
      default:
        return <AdminOverview onNavigate={navigate} />;
    }
  }

  return (
    <div className="min-h-[100dvh] flex bg-slate-50">
      {/* Desktop sidebar */}
      <aside className="hidden md:flex flex-col w-72 border-r bg-white">
        <SidebarHeader />
        <nav className="flex-1 overflow-y-auto px-2 pb-4">
          {NAV_SECTIONS.map((section) => (
            <NavSectionView
              key={section.title}
              section={section}
              expanded={expandedSections.has(section.title)}
              onToggle={() => toggleSection(section.title)}
              current={view}
              onNavigate={navigate}
            />
          ))}
        </nav>
        <SidebarFooter user={user} onLogout={logout} />
      </aside>

      {/* Mobile drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-50 md:hidden">
          <div className="absolute inset-0 bg-black/30" onClick={() => setMobileOpen(false)} />
          <div className="absolute left-0 top-0 bottom-0 w-72 bg-white flex flex-col">
            <SidebarHeader onClose={() => setMobileOpen(false)} />
            <nav className="flex-1 overflow-y-auto px-2 pb-4">
              {NAV_SECTIONS.map((section) => (
                <NavSectionView
                  key={section.title}
                  section={section}
                  expanded={expandedSections.has(section.title)}
                  onToggle={() => toggleSection(section.title)}
                  current={view}
                  onNavigate={navigate}
                />
              ))}
            </nav>
            <SidebarFooter user={user} onLogout={logout} />
          </div>
        </div>
      )}

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        <HeaderBar onMenuClick={() => setMobileOpen(true)} user={user} />
        <main className="flex-1 p-4 md:p-8 overflow-y-auto bg-slate-50">
          <AnimatePresence mode="wait">
            <motion.div
              key={view}
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={{ duration: 0.2 }}
            >
              {renderView()}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}

function isValidView(v: string): v is View {
  const all: string[] = ["overview", "exams", "color-vision", "demo-exams", "batch-exams", "chapter-exams",
    "question-bank", "question-categories", "all-books", "all-courses",
    "paid-exam-orders", "batch-orders", "course-orders", "qb-orders",
    "batch", "student-results", "package-results", "classroom-results",
    "students", "teachers", "pdf-viewer", "settings", "content", "questions", "tests", "users"];
  return all.includes(v);
}

function SidebarHeader({ onClose }: { onClose?: () => void }) {
  return (
    <div className="h-16 flex items-center gap-3 px-5 border-b shrink-0">
      <div className="h-9 w-9 rounded-full bg-gradient-to-br from-emerald-500 to-teal-600 grid place-items-center text-white shadow-sm">
        <span className="font-bold text-lg">A</span>
      </div>
      <div>
        <div className="font-bold text-sm tracking-tight">DREAMKOREA</div>
        <div className="text-[10px] text-slate-500 -mt-0.5">SmartClass Admin</div>
      </div>
      {onClose && (
        <Button variant="ghost" size="icon" className="ml-auto md:hidden" onClick={onClose}>
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}

function SidebarFooter({ user, onLogout }: { user: { name?: string | null; email: string } | null; onLogout: () => void }) {
  return (
    <div className="p-3 border-t shrink-0">
      <div className="flex items-center gap-3 mb-3 px-2">
        <Avatar className="h-9 w-9">
          <AvatarFallback className="bg-emerald-100 text-emerald-700 text-xs font-semibold">
            {(user?.name || user?.email || "A").slice(0, 2).toUpperCase()}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="text-sm font-medium truncate">{user?.name || "Admin"}</div>
          <div className="text-xs text-slate-500 truncate">{user?.email}</div>
        </div>
      </div>
      <Button size="sm" variant="outline" className="w-full" onClick={onLogout}>
        <LogOut className="mr-1 h-3 w-3" /> Sign out
      </Button>
    </div>
  );
}

function NavSectionView({
  section, expanded, onToggle, current, onNavigate
}: {
  section: NavSection;
  expanded: boolean;
  onToggle: () => void;
  current: View;
  onNavigate: (v: View) => void;
}) {
  return (
    <div className="mb-1">
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between px-3 py-2 text-xs font-semibold text-slate-500 uppercase tracking-wider hover:bg-slate-50 rounded-md transition-colors"
      >
        {section.title}
        {expanded ? <ChevronDown className="h-3 w-3" /> : <ChevronRight className="h-3 w-3" />}
      </button>
      {expanded && (
        <div className="space-y-0.5 mt-1">
          {section.items.map((item) => {
            const Icon = item.icon;
            const active = current === item.id;
            return (
              <button
                key={`${section.title}-${item.label}`}
                onClick={() => onNavigate(item.id)}
                className={cn(
                  "w-full flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors group",
                  active
                    ? "bg-emerald-50 text-emerald-700 font-medium"
                    : "text-slate-700 hover:bg-slate-100"
                )}
              >
                <Icon className={cn("h-4 w-4 shrink-0", active ? "text-emerald-600" : "text-slate-400")} />
                <span className="flex-1 text-left">{item.label}</span>
                {item.hasAdd && (
                  <span className={cn(
                    "h-5 w-5 rounded grid place-items-center text-xs transition-colors",
                    active ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500 group-hover:bg-slate-200"
                  )}>+</span>
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

function HeaderBar({ onMenuClick, user }: { onMenuClick: () => void; user: { name?: string | null } | null }) {
  return (
    <header className="h-16 border-b bg-white px-4 md:px-8 flex items-center gap-4 shrink-0">
      <Button variant="ghost" size="icon" className="md:hidden" onClick={onMenuClick}>
        <Menu className="h-5 w-5" />
      </Button>
      <div className="flex-1 max-w-md">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input
            placeholder="Search exams, students, questions…"
            className="pl-9 h-9 bg-slate-50 border-slate-200"
          />
        </div>
      </div>
      <Button variant="ghost" size="icon" className="relative">
        <Bell className="h-5 w-5 text-slate-600" />
        <span className="absolute top-2 right-2 h-2 w-2 rounded-full bg-rose-500" />
      </Button>
      <div className="hidden sm:flex items-center gap-2">
        <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200">
          <Award className="h-3 w-3 mr-1" /> Admin
        </Badge>
      </div>
    </header>
  );
}
