"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { BookOpen, FileText, MessageSquare, Flame, Trophy, Clock } from "lucide-react";
import { useAuthStore } from "@/stores/auth";

interface HomeStats {
  lessonsCompleted: number;
  testsTaken: number;
  qaAsked: number;
  streak: number;
  recentActivity: { id: string; label: string; ts: string }[];
}

export function StudentHome({ onNavigate }: { onNavigate: (v: "learn" | "tests" | "qa") => void }) {
  const { user } = useAuthStore();
  const [stats, setStats] = useState<HomeStats | null>(null);

  useEffect(() => {
    fetch("/api/student/home")
      .then((r) => (r.ok ? r.json() : null))
      .then((d) => d && setStats(d))
      .catch(() => null);
  }, []);

  const cards = [
    { label: "Lessons completed", value: stats?.lessonsCompleted ?? 0, icon: BookOpen, color: "text-emerald-600", bg: "bg-emerald-50" },
    { label: "Tests taken", value: stats?.testsTaken ?? 0, icon: FileText, color: "text-amber-600", bg: "bg-amber-50" },
    { label: "Q&A asked", value: stats?.qaAsked ?? 0, icon: MessageSquare, color: "text-rose-600", bg: "bg-rose-50" },
    { label: "Day streak", value: stats?.streak ?? 0, icon: Flame, color: "text-orange-600", bg: "bg-orange-50" },
  ];

  return (
    <div className="space-y-6">
      {/* Greeting */}
      <div className="rounded-2xl bg-gradient-to-br from-emerald-500 via-teal-500 to-emerald-600 text-white p-6 shadow-lg shadow-emerald-500/20">
        <p className="text-sm text-white/80">Welcome back,</p>
        <h2 className="text-2xl font-bold">{user?.name || "Student"}</h2>
        <p className="text-sm text-white/80 mt-1">Keep your momentum going today.</p>
        <div className="mt-4 flex gap-2">
          <Button size="sm" variant="secondary" onClick={() => onNavigate("learn")}>
            <BookOpen className="mr-2 h-4 w-4" /> Continue learning
          </Button>
          <Button size="sm" variant="outline" className="bg-white/10 text-white border-white/20 hover:bg-white/20 hover:text-white" onClick={() => onNavigate("tests")}>
            <FileText className="mr-2 h-4 w-4" /> Take a test
          </Button>
        </div>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {cards.map((c) => {
          const Icon = c.icon;
          return (
            <Card key={c.label} className="overflow-hidden">
              <CardContent className="p-4">
                <div className={`h-9 w-9 rounded-lg ${c.bg} grid place-items-center mb-2`}>
                  <Icon className={`h-4 w-4 ${c.color}`} />
                </div>
                <div className="text-2xl font-bold">{c.value}</div>
                <div className="text-xs text-muted-foreground">{c.label}</div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* Continue learning */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Trophy className="h-4 w-4 text-amber-500" /> Recommended for you
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {[
            { id: 1, title: "Algebra: Linear equations", pct: 60, lesson: "Chapter 3 · Lesson 2" },
            { id: 2, title: "Photosynthesis explained", pct: 0, lesson: "Biology · Chapter 1" },
            { id: 3, title: "World War II overview", pct: 100, lesson: "History · Chapter 5" },
          ].map((r) => (
            <div key={r.id} className="flex items-center gap-3 p-3 rounded-xl hover:bg-muted/40 transition">
              <div className="flex-1 min-w-0">
                <div className="font-medium truncate">{r.title}</div>
                <div className="text-xs text-muted-foreground">{r.lesson}</div>
                <Progress value={r.pct} className="h-1.5 mt-2" />
              </div>
              <Button size="sm" variant="ghost" onClick={() => onNavigate("learn")}>
                {r.pct === 100 ? "Review" : r.pct === 0 ? "Start" : "Resume"}
              </Button>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Recent activity */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Clock className="h-4 w-4" /> Recent activity
          </CardTitle>
        </CardHeader>
        <CardContent>
          {stats?.recentActivity?.length ? (
            <ul className="space-y-2 text-sm">
              {stats.recentActivity.map((a) => (
                <li key={a.id} className="flex justify-between py-1.5 border-b last:border-0">
                  <span>{a.label}</span>
                  <span className="text-muted-foreground text-xs">
                    {new Date(a.ts).toLocaleDateString()}
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-muted-foreground">No activity yet. Start learning to see your history here.</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
