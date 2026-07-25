"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { BarChart3, Award, Loader2, Clock, ListChecks } from "lucide-react";

interface SubmissionRow {
  id: string;
  score: number | null;
  maxScore: number | null;
  startedAt: string;
  submittedAt: string | null;
  graded: boolean;
  user?: {
    id: string;
    name: string | null;
    email: string;
  } | null;
  test?: {
    id: string;
    title: string;
    examType?: string;
    testCategory?: string;
  } | null;
}

interface Stats {
  total: number;
  graded: number;
  averagePct: number;
  bestPct: number;
}

export function AdminStudentResults() {
  const [rows, setRows] = useState<SubmissionRow[]>([]);
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/admin/student-results")
      .then((r) => r.json())
      .then((d) => {
        setRows(d.submissions || []);
        setStats(d.stats || null);
      })
      .catch(() => {
        setRows([]);
        setStats(null);
      })
      .finally(() => setLoading(false));
  }, []);

  function pct(s: SubmissionRow): number | null {
    if (s.score == null || s.maxScore == null || s.maxScore === 0) return null;
    return Math.round((s.score / s.maxScore) * 100);
  }

  function pctBadge(p: number | null) {
    if (p == null) return <Badge variant="outline">Pending</Badge>;
    if (p >= 80) return <Badge className="bg-emerald-500">Excellent</Badge>;
    if (p >= 50) return <Badge className="bg-amber-500">Good</Badge>;
    return <Badge variant="destructive">Needs work</Badge>;
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Student Results</h1>
          <p className="text-sm text-muted-foreground mt-1">
            All exam submissions with scores and grading status.
          </p>
        </div>
        <BarChart3 className="w-8 h-8 text-muted-foreground" />
      </div>

      {/* Summary stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <Card>
            <CardContent className="p-4 flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-emerald-50 grid place-items-center">
                <ListChecks className="h-5 w-5 text-emerald-600" />
              </div>
              <div>
                <div className="text-2xl font-bold">{stats.total}</div>
                <div className="text-xs text-muted-foreground">Total submissions</div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4 flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-blue-50 grid place-items-center">
                <BarChart3 className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <div className="text-2xl font-bold">{stats.graded}</div>
                <div className="text-xs text-muted-foreground">Graded</div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4 flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-amber-50 grid place-items-center">
                <Award className="h-5 w-5 text-amber-600" />
              </div>
              <div>
                <div className="text-2xl font-bold">{stats.averagePct}%</div>
                <div className="text-xs text-muted-foreground">Average score</div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4 flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-purple-50 grid place-items-center">
                <Clock className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <div className="text-2xl font-bold">{stats.bestPct}%</div>
                <div className="text-xs text-muted-foreground">Best score</div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Submissions table */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">All Exam Submissions ({rows.length})</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : rows.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground text-sm">
              No exam submissions yet.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 text-xs text-muted-foreground uppercase tracking-wider">
                  <tr>
                    <th className="text-left px-4 py-3 font-semibold">Student</th>
                    <th className="text-left px-4 py-3 font-semibold">Exam</th>
                    <th className="text-center px-4 py-3 font-semibold">Score</th>
                    <th className="text-center px-4 py-3 font-semibold">Max</th>
                    <th className="text-center px-4 py-3 font-semibold">%</th>
                    <th className="text-center px-4 py-3 font-semibold">Status</th>
                    <th className="text-left px-4 py-3 font-semibold">Submitted</th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {rows.map((s) => {
                    const p = pct(s);
                    return (
                      <tr key={s.id} className="hover:bg-muted/40 transition-colors">
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <Avatar className="h-8 w-8">
                              <AvatarFallback className="bg-emerald-100 text-emerald-700 text-xs">
                                {(s.user?.name || s.user?.email || "U").slice(0, 2).toUpperCase()}
                              </AvatarFallback>
                            </Avatar>
                            <div className="min-w-0">
                              <div className="font-medium truncate">
                                {s.user?.name || "Unnamed"}
                              </div>
                              <div className="text-xs text-muted-foreground truncate">
                                {s.user?.email}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="font-medium truncate max-w-[240px]">
                            {s.test?.title || "—"}
                          </div>
                          {s.test?.examType && (
                            <Badge variant="outline" className="text-[10px] mt-0.5">
                              {s.test.examType}
                            </Badge>
                          )}
                        </td>
                        <td className="px-4 py-3 text-center font-semibold">
                          {s.score ?? "—"}
                        </td>
                        <td className="px-4 py-3 text-center text-muted-foreground">
                          {s.maxScore ?? "—"}
                        </td>
                        <td className="px-4 py-3 text-center">
                          <span className="font-semibold">
                            {p == null ? "—" : `${p}%`}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-center">
                          {pctBadge(p)}
                        </td>
                        <td className="px-4 py-3 text-xs text-muted-foreground">
                          {s.submittedAt
                            ? new Date(s.submittedAt).toLocaleString()
                            : <Badge variant="outline">In progress</Badge>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
