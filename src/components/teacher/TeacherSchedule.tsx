"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Calendar, Plus, Trash2 } from "lucide-react";
import type { LiveClass } from "@/types";
import { toast } from "sonner";

export function TeacherSchedule() {
  const [list, setList] = useState<LiveClass[]>([]);
  const [title, setTitle] = useState("");
  const [when, setWhen] = useState("");
  const [duration, setDuration] = useState(60);

  function load() {
    fetch("/api/teacher/classes")
      .then((r) => r.json())
      .then((d) => setList([...(d.upcoming || []), ...(d.past || [])]))
      .catch(() => null);
  }
  useEffect(load, []);

  async function create() {
    if (!title.trim() || !when) {
      toast.error("Title and date required");
      return;
    }
    const res = await fetch("/api/teacher/classes", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title, scheduledAt: new Date(when).toISOString(), durationMin: duration }),
    });
    const d = await res.json();
    if (!res.ok) { toast.error(d.error || "Failed"); return; }
    toast.success("Class scheduled");
    setTitle(""); setWhen("");
    load();
  }

  async function remove(id: string) {
    await fetch(`/api/teacher/classes/${id}`, { method: "DELETE" });
    load();
  }

  return (
    <div className="space-y-4 max-w-2xl mx-auto">
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Plus className="h-4 w-4" /> Schedule new class
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div>
            <Label>Title</Label>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Chapter review session" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>When</Label>
              <Input type="datetime-local" value={when} onChange={(e) => setWhen(e.target.value)} />
            </div>
            <div>
              <Label>Duration (min)</Label>
              <Input type="number" min={15} max={240} value={duration} onChange={(e) => setDuration(Number(e.target.value))} />
            </div>
          </div>
          <Button onClick={create} className="w-full">Schedule</Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Calendar className="h-4 w-4" /> All classes
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {list.length === 0 ? (
            <p className="text-sm text-muted-foreground">No classes yet.</p>
          ) : list.map((c) => (
            <div key={c.id} className="flex items-center justify-between p-3 rounded-lg border">
              <div>
                <div className="font-medium">{c.title}</div>
                <div className="text-xs text-muted-foreground">
                  {new Date(c.scheduledAt).toLocaleString()} · {c.durationMin} min
                  {c.isLive && <span className="ml-2 text-rose-600 font-medium">● LIVE</span>}
                </div>
              </div>
              <Button size="icon" variant="ghost" onClick={() => remove(c.id)}>
                <Trash2 className="h-4 w-4 text-rose-500" />
              </Button>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
