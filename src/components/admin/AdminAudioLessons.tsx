"use client";

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Headphones, Plus, Trash2, Pencil, Play, Clock } from "lucide-react";
import { toast } from "sonner";

interface AudioLesson {
  id: string;
  title: string;
  slug: string;
  description?: string;
  audioUrl: string;
  durationSec: number;
  transcript?: string;
  translation?: string;
  level?: string;
  category?: string;
  isPublished: boolean;
  plays: number;
}

export function AdminAudioLessons() {
  const [lessons, setLessons] = useState<AudioLesson[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AudioLesson | null>(null);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({
    title: "", description: "", audioUrl: "", durationSec: 180,
    transcript: "", translation: "", level: "Beginner", category: "Conversation", isPublished: true,
  });

  function load() {
    fetch("/api/admin/audio-lessons").then(r => r.json()).then(d => setLessons(d.lessons || [])).catch(() => {});
  }
  useEffect(load, []);

  function openCreate() {
    setEditing(null);
    setForm({ title: "", description: "", audioUrl: "", durationSec: 180, transcript: "", translation: "", level: "Beginner", category: "Conversation", isPublished: true });
    setOpen(true);
  }

  function openEdit(l: AudioLesson) {
    setEditing(l);
    setForm({
      title: l.title, description: l.description || "", audioUrl: l.audioUrl,
      durationSec: l.durationSec, transcript: l.transcript || "", translation: l.translation || "",
      level: l.level || "Beginner", category: l.category || "Conversation", isPublished: l.isPublished,
    });
    setOpen(true);
  }

  async function save() {
    if (!form.title.trim() || !form.audioUrl.trim()) { toast.error("Title and audio URL required"); return; }
    setBusy(true);
    const slug = form.title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    const body = { ...form, slug, durationSec: Number(form.durationSec) };
    const res = await fetch("/api/admin/audio-lessons", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Audio lesson created");
    setOpen(false);
    load();
  }

  async function del(id: string) {
    if (!confirm("Delete this audio lesson?")) return;
    await fetch(`/api/admin/audio-lessons/${id}`, { method: "DELETE" });
    toast.success("Deleted");
    load();
  }

  function fmtDuration(sec: number) {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s.toString().padStart(2, "0")}`;
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Audio Lessons ({lessons.length})</h1>
          <p className="text-sm text-muted-foreground">Korean listening practice — conversation, news, pronunciation.</p>
        </div>
        <Button onClick={openCreate}><Plus className="mr-1 h-4 w-4" /> Add Lesson</Button>
      </div>

      <div className="space-y-2">
        {lessons.length === 0 ? (
          <Card><CardContent className="p-8 text-center text-muted-foreground">
            <Headphones className="h-10 w-10 mx-auto mb-2 opacity-40" />
            <p>No audio lessons yet. Add your first Korean listening lesson.</p>
          </CardContent></Card>
        ) : lessons.map(l => (
          <Card key={l.id} className="hover:shadow-md transition">
            <CardContent className="p-4 flex items-center gap-4">
              <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-amber-400 to-orange-500 grid place-items-center shrink-0">
                <Headphones className="h-5 w-5 text-white" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="font-semibold truncate">{l.title}</div>
                <div className="text-xs text-muted-foreground line-clamp-1">{l.description}</div>
                <div className="flex items-center gap-2 mt-1">
                  {l.level && <Badge variant="secondary" className="text-xs">{l.level}</Badge>}
                  {l.category && <Badge variant="outline" className="text-xs">{l.category}</Badge>}
                  <span className="text-xs text-muted-foreground flex items-center gap-1">
                    <Clock className="h-3 w-3" /> {fmtDuration(l.durationSec)}
                  </span>
                  <span className="text-xs text-muted-foreground">{l.plays} plays</span>
                </div>
              </div>
              <div className="flex gap-1">
                <Button size="sm" variant="ghost" onClick={() => openEdit(l)}><Pencil className="h-3 w-3" /></Button>
                <Button size="sm" variant="ghost" onClick={() => del(l.id)}><Trash2 className="h-3 w-3 text-rose-500" /></Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>{editing ? "Edit Audio Lesson" : "Add Audio Lesson"}</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <div><Label>Title</Label><Input value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder="Daily Korean Conversation — Episode 1" /></div>
            <div><Label>Description</Label><Textarea rows={2} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} /></div>
            <div><Label>Audio URL (MP3/M4A)</Label><Input value={form.audioUrl} onChange={e => setForm(f => ({ ...f, audioUrl: e.target.value }))} placeholder="https://s3.amazonaws.com/audio/lesson-1.mp3" /></div>
            <div className="grid grid-cols-3 gap-3">
              <div><Label>Duration (sec)</Label><Input type="number" value={form.durationSec} onChange={e => setForm(f => ({ ...f, durationSec: Number(e.target.value) }))} /></div>
              <div><Label>Level</Label>
                <Select value={form.level} onValueChange={v => setForm(f => ({ ...f, level: v }))}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Beginner">Beginner</SelectItem>
                    <SelectItem value="Intermediate">Intermediate</SelectItem>
                    <SelectItem value="Advanced">Advanced</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div><Label>Category</Label>
                <Select value={form.category} onValueChange={v => setForm(f => ({ ...f, category: v }))}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Conversation">Conversation</SelectItem>
                    <SelectItem value="News">News</SelectItem>
                    <SelectItem value="Story">Story</SelectItem>
                    <SelectItem value="Pronunciation">Pronunciation</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div><Label>Transcript (Korean)</Label><Textarea rows={3} value={form.transcript} onChange={e => setForm(f => ({ ...f, transcript: e.target.value }))} placeholder="안녕하세요. 만나서 반갑습니다." /></div>
            <div><Label>Translation (English)</Label><Textarea rows={2} value={form.translation} onChange={e => setForm(f => ({ ...f, translation: e.target.value }))} placeholder="Hello. Nice to meet you." /></div>
            <Button onClick={save} disabled={busy} className="w-full">{busy ? "Saving…" : "Create Audio Lesson"}</Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
