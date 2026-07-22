"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { BookOpen, FileQuestion, FileText, Users, TrendingUp, ArrowRight } from "lucide-react";

export function AdminOverview({ onNavigate }: { onNavigate: (v: "content" | "questions" | "tests" | "ai" | "users") => void }) {
  const [stats, setStats] = useState({
    subjects: 0, chapters: 0, lessons: 0, questions: 0, tests: 0, students: 0,
  });

  useEffect(() => {
    fetch("/api/admin/overview")
      .then((r) => r.json())
      .then((d) => setStats(d))
      .catch(() => null);
  }, []);

  const cards = [
    { label: "Subjects", value: stats.subjects, icon: BookOpen, color: "from-emerald-500 to-teal-500", view: "content" as const },
    { label: "Chapters", value: stats.chapters, icon: BookOpen, color: "from-amber-500 to-orange-500", view: "content" as const },
    { label: "Lessons", value: stats.lessons, icon: FileText, color: "from-blue-500 to-cyan-500", view: "content" as const },
    { label: "Questions", value: stats.questions, icon: FileQuestion, color: "from-rose-500 to-pink-500", view: "questions" as const },
    { label: "Tests / Exams", value: stats.tests, icon: FileText, color: "from-purple-500 to-fuchsia-500", view: "tests" as const },
    { label: "Students", value: stats.students, icon: Users, color: "from-slate-600 to-slate-800", view: "users" as const },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Admin overview</h1>
        <p className="text-sm text-muted-foreground mt-1">Manage all content, users and tests across the platform.</p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
        {cards.map((c) => {
          const Icon = c.icon;
          return (
            <Card key={c.label} className="hover:shadow-md transition cursor-pointer" onClick={() => onNavigate(c.view)}>
              <CardContent className="p-4">
                <div className={`h-10 w-10 rounded-lg bg-gradient-to-br ${c.color} grid place-items-center text-white mb-3`}>
                  <Icon className="h-5 w-5" />
                </div>
                <div className="text-2xl font-bold">{c.value}</div>
                <div className="text-xs text-muted-foreground flex items-center justify-between">
                  <span>{c.label}</span>
                  <ArrowRight className="h-3 w-3" />
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <Card>
        <CardHeader><CardTitle className="text-base flex items-center gap-2"><TrendingUp className="h-4 w-4" /> Quick actions</CardTitle></CardHeader>
        <CardContent className="grid sm:grid-cols-2 gap-2">
          <Button variant="outline" onClick={() => onNavigate("ai")}>Generate questions with AI</Button>
          <Button variant="outline" onClick={() => onNavigate("content")}>Add a new chapter</Button>
          <Button variant="outline" onClick={() => onNavigate("tests")}>Create a test or exam</Button>
          <Button variant="outline" onClick={() => onNavigate("questions")}>Upload questions from file</Button>
        </CardContent>
      </Card>
    </div>
  );
}
