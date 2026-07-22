"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Progress } from "@/components/ui/progress";

interface Row {
  id: string;
  name: string | null;
  email: string;
  lessonsCompleted: number;
  testsTaken: number;
  avgScore: number;
}

export function TeacherStudents() {
  const [rows, setRows] = useState<Row[]>([]);

  useEffect(() => {
    fetch("/api/teacher/students")
      .then((r) => r.json())
      .then((d) => setRows(d.students || []))
      .catch(() => null);
  }, []);

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold">Student progress</h2>
      <div className="space-y-2">
        {rows.length === 0 ? (
          <Card><CardContent className="p-6 text-center text-sm text-muted-foreground">
            No students enrolled in your subjects yet.
          </CardContent></Card>
        ) : rows.map((s) => (
          <Card key={s.id}>
            <CardContent className="p-4 flex items-center gap-3">
              <Avatar>
                <AvatarFallback className="bg-emerald-100 text-emerald-700">
                  {(s.name || s.email).slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <div className="font-medium truncate">{s.name || s.email}</div>
                <div className="text-xs text-muted-foreground">{s.email}</div>
                <div className="grid grid-cols-3 gap-2 mt-2 text-xs">
                  <div>
                    <div className="text-muted-foreground">Lessons</div>
                    <div className="font-semibold">{s.lessonsCompleted}</div>
                  </div>
                  <div>
                    <div className="text-muted-foreground">Tests</div>
                    <div className="font-semibold">{s.testsTaken}</div>
                  </div>
                  <div>
                    <div className="text-muted-foreground">Avg</div>
                    <div className="font-semibold">{s.avgScore}%</div>
                  </div>
                </div>
                <Progress value={s.avgScore} className="h-1.5 mt-2" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
