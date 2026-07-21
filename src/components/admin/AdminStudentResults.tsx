"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Progress } from "@/components/ui/progress";
import { BarChart3, Award, Flame, BookOpen, Headphones, Clock } from "lucide-react";

interface StudentResult {
  id: string;
  name: string | null;
  email: string;
  role: string;
  _count: { submissions: number };
  stats?: {
    totalExamsTaken: number;
    totalCorrectAnswers: number;
    totalQuestionsAnswered: number;
    averageScore: number;
    studyStreakDays: number;
    totalTimeSpentMin: number;
    booksRead: number;
    audioLessonsCompleted: number;
  };
}

export function AdminStudentResults() {
  const [students, setStudents] = useState<StudentResult[]>([]);

  useEffect(() => {
    fetch("/api/admin/student-results")
      .then(r => r.json())
      .then(d => setStudents(d.students || []))
      .catch(() => {});
  }, []);

  const totals = students.reduce((acc, s) => ({
    exams: acc.exams + (s.stats?.totalExamsTaken || 0),
    avg: acc.avg + (s.stats?.averageScore || 0),
    streak: Math.max(acc.streak, s.stats?.studyStreakDays || 0),
    time: acc.time + (s.stats?.totalTimeSpentMin || 0),
  }), { exams: 0, avg: 0, streak: 0, time: 0 });

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold">Student Results</h1>
        <p className="text-sm text-muted-foreground">Real-time performance across all students.</p>
      </div>

      {/* Summary stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <Card><CardContent className="p-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg bg-emerald-50 grid place-items-center"><BarChart3 className="h-5 w-5 text-emerald-600" /></div>
            <div><div className="text-2xl font-bold">{totals.exams}</div><div className="text-xs text-muted-foreground">Total exams</div></div>
          </div>
        </CardContent></Card>
        <Card><CardContent className="p-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg bg-amber-50 grid place-items-center"><Award className="h-5 w-5 text-amber-600" /></div>
            <div><div className="text-2xl font-bold">{students.length > 0 ? Math.round(totals.avg / students.length) : 0}%</div><div className="text-xs text-muted-foreground">Avg score</div></div>
          </div>
        </CardContent></Card>
        <Card><CardContent className="p-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg bg-orange-50 grid place-items-center"><Flame className="h-5 w-5 text-orange-600" /></div>
            <div><div className="text-2xl font-bold">{totals.streak}</div><div className="text-xs text-muted-foreground">Best streak</div></div>
          </div>
        </CardContent></Card>
        <Card><CardContent className="p-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-lg bg-blue-50 grid place-items-center"><Clock className="h-5 w-5 text-blue-600" /></div>
            <div><div className="text-2xl font-bold">{Math.round(totals.time / 60)}h</div><div className="text-xs text-muted-foreground">Total study time</div></div>
          </div>
        </CardContent></Card>
      </div>

      {/* Student table */}
      <Card>
        <CardHeader><CardTitle className="text-base">All Students ({students.length})</CardTitle></CardHeader>
        <CardContent className="p-0">
          <div className="divide-y">
            {students.length === 0 ? (
              <div className="p-8 text-center text-muted-foreground text-sm">No students enrolled yet.</div>
            ) : students.map(s => (
              <div key={s.id} className="flex items-center gap-4 p-4 hover:bg-muted/40 transition">
                <Avatar><AvatarFallback className="bg-emerald-100 text-emerald-700 text-xs">{(s.name || s.email).slice(0, 2).toUpperCase()}</AvatarFallback></Avatar>
                <div className="flex-1 min-w-0">
                  <div className="font-medium truncate">{s.name || "Unnamed"}</div>
                  <div className="text-xs text-muted-foreground truncate">{s.email}</div>
                </div>
                <div className="hidden sm:grid grid-cols-4 gap-4 text-center text-xs">
                  <div>
                    <div className="font-bold text-sm">{s.stats?.totalExamsTaken || 0}</div>
                    <div className="text-muted-foreground">Exams</div>
                  </div>
                  <div>
                    <div className="font-bold text-sm">{s.stats?.averageScore.toFixed(0) || 0}%</div>
                    <div className="text-muted-foreground">Avg</div>
                  </div>
                  <div>
                    <div className="font-bold text-sm">{s.stats?.booksRead || 0}</div>
                    <div className="text-muted-foreground">Books</div>
                  </div>
                  <div>
                    <div className="font-bold text-sm">{s.stats?.audioLessonsCompleted || 0}</div>
                    <div className="text-muted-foreground">Audio</div>
                  </div>
                </div>
                <Badge variant={s.stats?.averageScore >= 80 ? "default" : s.stats?.averageScore >= 50 ? "secondary" : "outline"} className="text-xs">
                  {s.stats?.averageScore >= 80 ? "Excellent" : s.stats?.averageScore >= 50 ? "Good" : "Needs work"}
                </Badge>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
