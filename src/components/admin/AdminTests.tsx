"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Plus, FileText, Trash2 } from "lucide-react";
import type { Test, Question, Chapter } from "@/types";
import { toast } from "sonner";

export function AdminTests() {
  const [tests, setTests] = useState<Test[]>([]);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);

  function load() {
    fetch("/api/admin/tests").then((r) => r.json()).then((d) => setTests(d.tests || []));
    fetch("/api/admin/chapters").then((r) => r.json()).then((d) => setChapters(d.chapters || []));
    fetch("/api/admin/questions").then((r) => r.json()).then((d) => setQuestions(d.questions || []));
  }
  useEffect(load, []);

  async function del(id: string) {
    if (!confirm("Delete this test?")) return;
    await fetch(`/api/admin/tests/${id}`, { method: "DELETE" });
    load();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Tests & Exams ({tests.length})</h1>
          <p className="text-sm text-muted-foreground">Assemble questions into practice tests or graded exams.</p>
        </div>
        <TestDialog chapters={chapters} questions={questions} onSaved={load} />
      </div>

      <Card>
        <CardContent className="space-y-2 p-4">
          {tests.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-6">No tests yet. Create your first one.</p>
          ) : tests.map((t) => (
            <div key={t.id} className="flex items-center justify-between p-3 border rounded-lg">
              <div>
                <div className="font-medium flex items-center gap-2">
                  {t.title}
                  {t.isExam && <span className="text-xs bg-rose-100 text-rose-700 px-2 py-0.5 rounded-full">Exam</span>}
                </div>
                <div className="text-xs text-muted-foreground">
                  {t.durationMin} min · Pass {t.passScore}% · {t.isPublished ? "Published" : "Draft"}
                </div>
              </div>
              <Button size="icon" variant="ghost" onClick={() => del(t.id)}>
                <Trash2 className="h-4 w-4 text-rose-500" />
              </Button>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}

function TestDialog({ chapters, questions, onSaved }: { chapters: Chapter[]; questions: Question[]; onSaved: () => void }) {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [chapterId, setChapterId] = useState<string>("");
  const [durationMin, setDurationMin] = useState(30);
  const [passScore, setPassScore] = useState(40);
  const [isExam, setIsExam] = useState(false);
  const [selectedQs, setSelectedQs] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!title.trim()) { toast.error("Title required"); return; }
    if (selectedQs.length === 0) { toast.error("Pick at least one question"); return; }
    setBusy(true);
    const res = await fetch("/api/admin/tests", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title, description, chapterId: chapterId || undefined, durationMin, passScore, isExam, questionIds: selectedQs, isPublished: true }),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Test created");
    setOpen(false);
    setTitle(""); setDescription(""); setSelectedQs([]);
    onSaved();
  }

  function toggleQ(id: string) {
    setSelectedQs((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id]));
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button><Plus className="mr-1 h-4 w-4" /> New test</Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader><DialogTitle>Create test / exam</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div><Label>Title</Label><Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Algebra chapter quiz" /></div>
          <div><Label>Description</Label><Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} /></div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Chapter</Label>
              <Select value={chapterId} onValueChange={setChapterId}>
                <SelectTrigger><SelectValue placeholder="Optional" /></SelectTrigger>
                <SelectContent>
                  {chapters.map((c) => <SelectItem key={c.id} value={c.id}>{c.title}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div><Label>Duration (min)</Label><Input type="number" value={durationMin} onChange={(e) => setDurationMin(Number(e.target.value))} /></div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div><Label>Pass score (%)</Label><Input type="number" value={passScore} onChange={(e) => setPassScore(Number(e.target.value))} /></div>
            <div className="flex items-center gap-2 pt-6">
              <Switch checked={isExam} onCheckedChange={setIsExam} />
              <Label>Graded exam</Label>
            </div>
          </div>
          <div>
            <Label>Questions ({selectedQs.length} selected)</Label>
            <div className="max-h-64 overflow-y-auto border rounded-lg p-2 space-y-1">
              {questions.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4">No questions available. Add some first.</p>
              ) : questions.map((q) => (
                <label key={q.id} className="flex items-start gap-2 p-2 hover:bg-slate-50 rounded cursor-pointer">
                  <input
                    type="checkbox"
                    checked={selectedQs.includes(q.id)}
                    onChange={() => toggleQ(q.id)}
                    className="mt-1"
                  />
                  <div className="text-sm">
                    <div className="line-clamp-1">{q.stem}</div>
                    <div className="text-xs text-muted-foreground">{q.type.replace(/_/g, " ")} · {q.difficulty.toLowerCase()}</div>
                  </div>
                </label>
              ))}
            </div>
          </div>
          <Button onClick={save} disabled={busy}>{busy ? "Saving…" : "Create test"}</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
