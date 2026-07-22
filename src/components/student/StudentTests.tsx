"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Clock, Award, ChevronRight } from "lucide-react";
import type { Test } from "@/types";
import { StudentTestRunner } from "./StudentTestRunner";

export function StudentTests() {
  const [tests, setTests] = useState<Test[]>([]);
  const [active, setActive] = useState<Test | null>(null);

  useEffect(() => {
    fetch("/api/student/tests")
      .then((r) => r.json())
      .then((d) => setTests(d.tests || []))
      .catch(() => null);
  }, []);

  if (active) {
    return <StudentTestRunner test={active} onExit={() => setActive(null)} />;
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold">Tests & Exams</h2>

      <div className="grid sm:grid-cols-2 gap-3">
        {tests.length === 0 ? (
          <p className="text-sm text-muted-foreground col-span-full">No tests available right now. Check back soon.</p>
        ) : tests.map((t) => (
          <Card key={t.id} className="hover:shadow-md transition">
            <CardHeader>
              <div className="flex items-start justify-between gap-2">
                <CardTitle className="text-base">{t.title}</CardTitle>
                {t.isExam ? (
                  <Badge variant="destructive">Exam</Badge>
                ) : (
                  <Badge variant="secondary">Practice</Badge>
                )}
              </div>
            </CardHeader>
            <CardContent>
              {t.description && (
                <p className="text-sm text-muted-foreground mb-3 line-clamp-2">{t.description}</p>
              )}
              <div className="flex items-center gap-4 text-xs text-muted-foreground mb-3">
                <span className="flex items-center gap-1"><Clock className="h-3 w-3" /> {t.durationMin} min</span>
                <span className="flex items-center gap-1"><Award className="h-3 w-3" /> Pass: {t.passScore}%</span>
                {t.items?.length && <span>{t.items.length} questions</span>}
              </div>
              <Button className="w-full" onClick={() => setActive(t)}>
                Start {t.isExam ? "exam" : "test"} <ChevronRight className="ml-1 h-4 w-4" />
              </Button>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
