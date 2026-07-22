"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Plus, BookOpen, FileText, Trash2, Pencil, ChevronDown, ChevronRight } from "lucide-react";
import type { Subject, Chapter, Lesson } from "@/types";
import { toast } from "sonner";

export function AdminContent() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [lessonsByChapter, setLessonsByChapter] = useState<Record<string, Lesson[]>>({});

  function load() {
    fetch("/api/admin/subjects")
      .then((r) => r.json())
      .then((d) => setSubjects(d.subjects || []));
    fetch("/api/admin/chapters")
      .then((r) => r.json())
      .then((d) => setChapters(d.chapters || []));
  }
  useEffect(load, []);

  function toggleChapter(id: string) {
    if (expanded === id) { setExpanded(null); return; }
    setExpanded(id);
    if (!lessonsByChapter[id]) {
      fetch(`/api/admin/chapters/${id}/lessons`)
        .then((r) => r.json())
        .then((d) => setLessonsByChapter((p) => ({ ...p, [id]: d.lessons || [] })));
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Chapters & Lessons</h1>
          <p className="text-sm text-muted-foreground">Organize your content into subjects, chapters, and lessons.</p>
        </div>
        <SubjectDialog onSaved={load} subjects={subjects} />
      </div>

      {/* Subjects list */}
      <Card>
        <CardHeader><CardTitle className="text-base">Subjects ({subjects.length})</CardTitle></CardHeader>
        <CardContent className="space-y-2">
          {subjects.length === 0 ? (
            <p className="text-sm text-muted-foreground">No subjects yet. Create one to start adding chapters.</p>
          ) : subjects.map((s) => {
            const subjectChapters = chapters.filter((c) => c.subjectId === s.id);
            return (
              <div key={s.id} className="border rounded-lg">
                <div className="p-3 flex items-center gap-3">
                  <div className="h-9 w-9 rounded-lg bg-emerald-100 text-emerald-700 grid place-items-center">
                    <BookOpen className="h-4 w-4" />
                  </div>
                  <div className="flex-1">
                    <div className="font-medium">{s.name}</div>
                    <div className="text-xs text-muted-foreground">{subjectChapters.length} chapter(s)</div>
                  </div>
                  <ChapterDialog subjectId={s.id} onSaved={load} />
                </div>
                <div className="border-t">
                  {subjectChapters.length === 0 ? (
                    <p className="p-3 text-sm text-muted-foreground">No chapters yet.</p>
                  ) : subjectChapters.map((c) => (
                    <div key={c.id}>
                      <div className="flex items-center gap-2 p-3 hover:bg-slate-50">
                        <button onClick={() => toggleChapter(c.id)}>
                          {expanded === c.id ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                        </button>
                        <div className="flex-1">
                          <div className="font-medium text-sm">{c.title}</div>
                          <div className="text-xs text-muted-foreground">{c.slug}</div>
                        </div>
                        <span className={`text-xs px-2 py-0.5 rounded-full ${c.isPublished ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>
                          {c.isPublished ? "Published" : "Draft"}
                        </span>
                        <LessonDialog chapterId={c.id} onSaved={() => toggleChapter(c.id)} />
                        <ChapterDelete id={c.id} onDeleted={load} />
                      </div>
                      {expanded === c.id && (
                        <div className="pl-10 py-2 space-y-1">
                          {(lessonsByChapter[c.id] || []).length === 0 ? (
                            <p className="text-xs text-muted-foreground">No lessons.</p>
                          ) : lessonsByChapter[c.id].map((l) => (
                            <div key={l.id} className="flex items-center gap-2 p-2 text-sm">
                              <FileText className="h-3 w-3 text-muted-foreground" />
                              <span className="flex-1">{l.title}</span>
                              <span className="text-xs text-muted-foreground">{l.type}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </CardContent>
      </Card>
    </div>
  );
}

function SubjectDialog({ onSaved, subjects }: { onSaved: () => void; subjects: Subject[] }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!name.trim()) return;
    setBusy(true);
    const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    const res = await fetch("/api/admin/subjects", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, slug, description }),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Subject created");
    setName(""); setDescription("");
    setOpen(false);
    onSaved();
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm"><Plus className="mr-1 h-4 w-4" /> Subject</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader><DialogTitle>New subject</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div><Label>Name</Label><Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Mathematics" /></div>
          <div><Label>Description</Label><Textarea value={description} onChange={(e) => setDescription(e.target.value)} /></div>
          <Button onClick={save} disabled={busy}>{busy ? "Saving…" : "Create"}</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function ChapterDialog({ subjectId, onSaved }: { subjectId: string; onSaved: () => void }) {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!title.trim()) return;
    setBusy(true);
    const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    const res = await fetch("/api/admin/chapters", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ subjectId, title, slug, description, isPublished: true }),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Chapter created");
    setTitle(""); setDescription("");
    setOpen(false);
    onSaved();
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm" variant="outline"><Plus className="mr-1 h-3 w-3" /> Chapter</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader><DialogTitle>New chapter</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div><Label>Title</Label><Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Linear Equations" /></div>
          <div><Label>Description</Label><Textarea value={description} onChange={(e) => setDescription(e.target.value)} /></div>
          <Button onClick={save} disabled={busy}>{busy ? "Saving…" : "Create"}</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function LessonDialog({ chapterId, onSaved }: { chapterId: string; onSaved: () => void }) {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [type, setType] = useState<"TEXT" | "VIDEO" | "PDF" | "INTERACTIVE">("TEXT");
  const [content, setContent] = useState("");
  const [videoUrl, setVideoUrl] = useState("");
  const [durationMin, setDurationMin] = useState(10);
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!title.trim()) return;
    setBusy(true);
    const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    const res = await fetch("/api/admin/lessons", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ chapterId, title, slug, type, content, videoUrl, durationMin, isPublished: true }),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Lesson created");
    setTitle(""); setContent(""); setVideoUrl("");
    setOpen(false);
    onSaved();
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm" variant="ghost"><Plus className="mr-1 h-3 w-3" /> Lesson</Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader><DialogTitle>New lesson</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div><Label>Title</Label><Input value={title} onChange={(e) => setTitle(e.target.value)} /></div>
          <div>
            <Label>Type</Label>
            <Select value={type} onValueChange={(v) => setType(v as "TEXT" | "VIDEO" | "PDF" | "INTERACTIVE")}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="TEXT">Text / Markdown</SelectItem>
                <SelectItem value="VIDEO">Video</SelectItem>
                <SelectItem value="PDF">PDF</SelectItem>
                <SelectItem value="INTERACTIVE">Interactive</SelectItem>
              </SelectContent>
            </Select>
          </div>
          {type === "VIDEO" && (
            <div><Label>Video URL</Label><Input value={videoUrl} onChange={(e) => setVideoUrl(e.target.value)} placeholder="https://…" /></div>
          )}
          {type === "TEXT" && (
            <div><Label>Content (Markdown)</Label><Textarea rows={6} value={content} onChange={(e) => setContent(e.target.value)} /></div>
          )}
          <div><Label>Duration (min)</Label><Input type="number" value={durationMin} onChange={(e) => setDurationMin(Number(e.target.value))} /></div>
          <Button onClick={save} disabled={busy}>{busy ? "Saving…" : "Create lesson"}</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function ChapterDelete({ id, onDeleted }: { id: string; onDeleted: () => void }) {
  async function del() {
    if (!confirm("Delete this chapter and all its lessons?")) return;
    const res = await fetch(`/api/admin/chapters/${id}`, { method: "DELETE" });
    if (res.ok) { toast.success("Deleted"); onDeleted(); }
    else toast.error("Failed");
  }
  return <Button size="icon" variant="ghost" onClick={del}><Trash2 className="h-4 w-4 text-rose-500" /></Button>;
}
