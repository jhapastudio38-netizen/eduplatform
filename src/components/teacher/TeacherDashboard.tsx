"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Radio, Users, Calendar, Clock, Plus } from "lucide-react";
import type { LiveClass } from "@/types";

export function TeacherDashboard({ onNavigate }: { onNavigate: (v: "live" | "schedule" | "students") => void }) {
  const [live, setLive] = useState<LiveClass | null>(null);
  const [upcoming, setUpcoming] = useState<LiveClass[]>([]);

  useEffect(() => {
    fetch("/api/teacher/classes")
      .then((r) => r.json())
      .then((d) => {
        setLive(d.live || null);
        setUpcoming(d.upcoming || []);
      })
      .catch(() => null);
  }, []);

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-gradient-to-br from-amber-500 via-orange-500 to-rose-500 text-white p-6 shadow-lg shadow-amber-500/20">
        <p className="text-sm text-white/80">Teacher dashboard</p>
        <h2 className="text-2xl font-bold">Ready to teach? 🎓</h2>
        <div className="mt-4 flex gap-2">
          <Button size="sm" variant="secondary" onClick={() => onNavigate("live")}>
            <Radio className="mr-2 h-4 w-4" /> Go live now
          </Button>
          <Button size="sm" variant="outline" className="bg-white/10 text-white border-white/20 hover:bg-white/20 hover:text-white" onClick={() => onNavigate("schedule")}>
            <Plus className="mr-2 h-4 w-4" /> Schedule class
          </Button>
        </div>
      </div>

      <div className="grid sm:grid-cols-3 gap-3">
        {[
          { label: "Live now", icon: Radio, value: live ? "1" : "0", color: "text-rose-600 bg-rose-50" },
          { label: "Scheduled", icon: Calendar, value: String(upcoming.length), color: "text-amber-600 bg-amber-50" },
          { label: "Students", icon: Users, value: "—", color: "text-emerald-600 bg-emerald-50" },
        ].map((c) => {
          const Icon = c.icon;
          return (
            <Card key={c.label}>
              <CardContent className="p-4 flex items-center gap-3">
                <div className={`h-10 w-10 rounded-lg ${c.color} grid place-items-center`}>
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <div className="text-2xl font-bold leading-tight">{c.value}</div>
                  <div className="text-xs text-muted-foreground">{c.label}</div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Clock className="h-4 w-4" /> Upcoming classes
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {upcoming.length === 0 ? (
            <p className="text-sm text-muted-foreground">No upcoming classes. Schedule one to get started.</p>
          ) : upcoming.map((c) => (
            <div key={c.id} className="flex items-center justify-between p-3 rounded-lg border">
              <div>
                <div className="font-medium">{c.title}</div>
                <div className="text-xs text-muted-foreground">
                  {new Date(c.scheduledAt).toLocaleString()} · {c.durationMin} min
                </div>
              </div>
              <Button size="sm" variant="outline" onClick={() => onNavigate("live")}>
                Start
              </Button>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
