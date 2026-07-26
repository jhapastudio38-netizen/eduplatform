"use client";

import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  LayoutDashboard, FileText, BookOpen, FileQuestion, Users, ShoppingBag,
  GraduationCap, School, Image, Layers, Package, UserCog, BookMarked,
  ClipboardList, BarChart3, Settings, LogOut, ChevronDown, ChevronRight,
  FolderTree, Library, Award, Bell, Search, Menu, X, Headphones, Radio, Video,
  LayoutGrid, Eye, Ticket
} from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { AdminOverview } from "./AdminOverview";
import { AdminContent } from "./AdminContent";
import { AdminTests } from "./AdminTests";
import { AdminAIGenerate } from "./AdminAIGenerate";
import { AdminUsers } from "./AdminUsers";
import { AdminNotifications } from "./AdminNotifications";
import { AdminLiveSessions } from "./AdminLiveSessions";
import { AdminBooks } from "./AdminBooks";
import { AdminEyeVision } from "./AdminEyeVision";
import { AdminHomeCards } from "./AdminHomeCards";
import { AdminAudioLessons } from "./AdminAudioLessons";
import { AdminVideoLessons } from "./AdminVideoLessons";
import { AdminLiveRooms } from "./AdminLiveRooms";
import { AdminStudentResults } from "./AdminStudentResults";
import { AdminPlaceholder } from "./AdminPlaceholder";
import { AdminBundles } from "./AdminBundles";
import { AdminTeacherInvites } from "./AdminTeacherInvites";
import {
  AdminQuestionCategories, AdminAllCourses, AdminBatch,
  AdminPDFViewer, AdminColorVision, AdminPackageResults,
  AdminClassroomResults, AdminOrders
} from "./AdminSections";

type View =
  | "overview" | "exams" | "color-vision" | "demo-exams" | "batch-exams" | "chapter-exams" | "live-rooms"
  | "question-bank" | "question-categories" | "all-books" | "all-courses" | "audio-lessons" | "video-lessons"
  | "paid-exam-orders" | "batch-orders" | "course-orders" | "qb-orders"
  | "batch" | "student-results" | "package-results" | "classroom-results"
  | "students" | "teachers" | "pdf-viewer" | "settings" | "home-cards"
  | "content" | "questions" | "tests" | "users" | "notifications" | "ai"
  | "eye-vision" | "books" | "live-sessions"
  | "bundles-qbank" | "bundles-batch" | "bundles-exam" | "bundles-chapter"
  | "teacher-invites";

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
      { id: "demo-exams", label: "Demo Exams", icon: FileText, hasAdd: true },
      { id: "batch-exams", label: "Batch Exams", icon: Layers, hasAdd: true },
      { id: "chapter-exams", label: "Chapter Exams", icon: BookOpen, hasAdd: true },
      { id: "question-bank", label: "Question Bank", icon: FileQuestion, hasAdd: true },
    ],
  },
  {
    title: "Packages",
    items: [
      { id: "bundles-qbank", label: "QBank Packages", icon: Package, hasAdd: true },
      { id: "bundles-batch", label: "Batch Packages", icon: Package, hasAdd: true },
      { id: "bundles-exam", label: "Exam Packages", icon: Package, hasAdd: true },
      { id: "bundles-chapter", label: "Chapter Packages", icon: Package, hasAdd: true },
    ],
  },
  {
    title: "Content",
    items: [
      { id: "home-cards", label: "Home Cards", icon: LayoutGrid, hasAdd: true },
      { id: "eye-vision", label: "Eye Vision", icon: Eye, hasAdd: true },
      { id: "books", label: "Books", icon: BookMarked, hasAdd: true },
    ],
  },
  {
    title: "Management",
    items: [
      { id: "live-sessions", label: "Live Sessions", icon: Radio, hasAdd: true },
      { id: "student-results", label: "Student Results", icon: BarChart3 },
      { id: "students", label: "Students", icon: GraduationCap },
      { id: "teachers", label: "Teachers", icon: UserCog, hasAdd: true },
      { id: "teacher-invites", label: "Teacher Invites", icon: Ticket, hasAdd: true },
    ],
  },
  {
    title: "Communication",
    items: [
      { id: "notifications", label: "Push Notifications", icon: Bell, hasAdd: true },
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
  const isAdmin = user?.role === "ADMIN";

  // ─── Role-based nav filtering ────────────────────────────────────────────────
  // Teachers only see:
  //   • Exam Management  (all 5 items)
  //   • Packages         (all 4 items)
  //   • Management       (only Live Sessions, Student Results, Students — NO
  //                       Teachers list, NO Teacher Invites, NO ability to
  //                       create other teachers)
  // Admins see everything.
  const visibleSections: NavSection[] = isAdmin
    ? NAV_SECTIONS
    : NAV_SECTIONS
        .filter((s) => s.title !== "Communication") // no Push Notifications for teachers
        .filter((s) => s.title !== "Content")        // no Home Cards / Eye Vision / Books for teachers
        .map((s) => {
          if (s.title === "Management") {
            // Teachers see only Live Sessions, Student Results, Students
            // (no Teachers, no Teacher Invites — those are admin-only)
            return {
              ...s,
              items: s.items.filter(
                (it) =>
                  it.id === "live-sessions" ||
                  it.id === "student-results" ||
                  it.id === "students",
              ),
            };
          }
          return s;
        });

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

  // Guard: if a teacher somehow lands on a view they're not allowed to see
  // (e.g. via direct URL), redirect them to overview.
  const TEACHER_FORBIDDEN: View[] = [
    "home-cards", "eye-vision", "books",
    "teachers", "teacher-invites", "notifications",
    "all-books", "question-categories", "all-courses", "content",
  ];
  useEffect(() => {
    if (!isAdmin && TEACHER_FORBIDDEN.includes(view)) {
      setView("overview");
    }
  }, [isAdmin, view]);

  function renderView() {
    switch (view) {
      case "overview":
        return <AdminOverview onNavigate={navigate} />;
      case "all-books":
        return <AdminBooks />;
      case "home-cards":
        return <AdminHomeCards />;
      case "question-categories":
        return <AdminQuestionCategories />;
      case "all-courses":
        return <AdminAllCourses />;
      case "content":
        return <AdminContent />;
      case "ai":
        return <AdminAIGenerate />;
      case "exams":
      case "tests":
        return <AdminTests testCategory="exam" />;
      case "demo-exams":
        return <AdminTests testCategory="demo" />;
      case "batch-exams":
        return <AdminTests testCategory="batch" />;
      case "chapter-exams":
        return <AdminTests testCategory="chapter" />;
      case "question-bank":
      case "questions":
        return <AdminTests testCategory="question_bank" />;
      case "eye-vision":
        return <AdminEyeVision />;
      case "books":
        return <AdminBooks />;
      case "students":
      case "users":
        return <AdminUsers role="STUDENT" />;
      case "teachers":
        return <AdminUsers role="TEACHER" />;
      case "audio-lessons":
        return <AdminAudioLessons />;
      case "video-lessons":
        return <AdminVideoLessons />;
      case "student-results":
        return <AdminStudentResults />;
      case "live-sessions":
        return <AdminLiveSessions />;
      case "notifications":
        return <AdminNotifications />;
      case "batch":
        return <AdminBatch />;
      case "color-vision":
        return <AdminColorVision />;
      case "pdf-viewer":
        return <AdminPDFViewer />;
      case "package-results":
        return <AdminPackageResults />;
      case "classroom-results":
        return <AdminClassroomResults />;
      case "paid-exam-orders":
        return <AdminOrders type="Paid Exam Orders" />;
      case "batch-orders":
        return <AdminOrders type="Batch Orders" />;
      case "course-orders":
        return <AdminOrders type="Course Orders" />;
      case "qb-orders":
        return <AdminOrders type="Question Bank Orders" />;
      case "bundles-qbank":
        return <AdminBundles initialKind="qbank" />;
      case "bundles-batch":
        return <AdminBundles initialKind="batch" />;
      case "bundles-exam":
        return <AdminBundles initialKind="exam" />;
      case "bundles-chapter":
        return <AdminBundles initialKind="chapter" />;
      case "teacher-invites":
        return <AdminTeacherInvites />;
      default:
        return <AdminOverview onNavigate={navigate} />;
    }
  }

  return (
    <div className="min-h-[100dvh] flex bg-slate-50">
      {/* Desktop sidebar */}
      <aside className="hidden md:flex flex-col w-72 border-r bg-white">
        <SidebarHeader isAdmin={isAdmin} />
        <nav className="flex-1 overflow-y-auto px-2 pb-4">
          {visibleSections.map((section) => (
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
            <SidebarHeader onClose={() => setMobileOpen(false)} isAdmin={isAdmin} />
            <nav className="flex-1 overflow-y-auto px-2 pb-4">
              {visibleSections.map((section) => (
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
  const all: string[] = ["overview", "exams", "color-vision", "demo-exams", "batch-exams", "chapter-exams", "live-rooms",
    "question-bank", "question-categories", "all-books", "all-courses", "audio-lessons", "video-lessons",
    "paid-exam-orders", "batch-orders", "course-orders", "qb-orders",
    "batch", "student-results", "package-results", "classroom-results",
    "students", "teachers", "pdf-viewer", "settings", "home-cards", "content", "questions", "tests", "users",
    "notifications", "eye-vision", "books", "ai",
    "bundles-qbank", "bundles-batch", "bundles-exam", "bundles-chapter", "teacher-invites"];
  return all.includes(v);
}

function SidebarHeader({ onClose, isAdmin = true }: { onClose?: () => void; isAdmin?: boolean }) {
  return (
    <div className="h-16 flex items-center gap-3 px-5 border-b shrink-0">
      <div className={`h-9 w-9 rounded-full grid place-items-center text-white shadow-sm ${
        isAdmin
          ? "bg-gradient-to-br from-emerald-500 to-teal-600"
          : "bg-gradient-to-br from-amber-500 to-orange-600"
      }`}>
        <span className="font-bold text-lg">{isAdmin ? "A" : "T"}</span>
      </div>
      <div>
        <div className="font-bold text-sm tracking-tight">DREAMKOREA</div>
        <div className="text-[10px] text-slate-500 -mt-0.5">
          SmartClass {isAdmin ? "Admin" : "Teacher"}
        </div>
      </div>
      {onClose && (
        <Button variant="ghost" size="icon" className="ml-auto md:hidden" onClick={onClose}>
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}

function SidebarFooter({ user, onLogout }: { user: { name?: string | null; email: string; role?: string } | null; onLogout: () => void }) {
  const isAdmin = user?.role === "ADMIN";
  return (
    <div className="p-3 border-t shrink-0">
      <div className="flex items-center gap-3 mb-3 px-2">
        <Avatar className="h-9 w-9">
          <AvatarFallback className={`text-xs font-semibold ${
            isAdmin
              ? "bg-emerald-100 text-emerald-700"
              : "bg-amber-100 text-amber-700"
          }`}>
            {(user?.name || user?.email || "A").slice(0, 2).toUpperCase()}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <div className="text-sm font-medium truncate">{user?.name || (isAdmin ? "Admin" : "Teacher")}</div>
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

function HeaderBar({ onMenuClick, user }: { onMenuClick: () => void; user: { name?: string | null; role?: string } | null }) {
  const isAdmin = user?.role === "ADMIN";
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
        {isAdmin ? (
          <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-200">
            <Award className="h-3 w-3 mr-1" /> Admin
          </Badge>
        ) : (
          <Badge variant="outline" className="bg-amber-50 text-amber-700 border-amber-200">
            <School className="h-3 w-3 mr-1" /> Teacher
          </Badge>
        )}
      </div>
    </header>
  );
}
