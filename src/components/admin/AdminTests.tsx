"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { FileText, Plus, Trash2, Clock, Power, PowerOff, Calendar, AlertCircle } from "lucide-react";
import { toast } from "sonner";

interface Test {
  id: string;
  title: string;
  description?: string;
  durationMin: number;
  isExam: boolean;
  examType: string;
  passScore: number;
  startAt?: string | null;
  endAt?: string | null;
  isActive: boolean;
  isPublished: boolean;
  negativeMarking: number;
  shuffleQuestions: boolean;
  showResultImmediately: boolean;
  maxAttempts: number;
  _count?: { items: number; submissions: number };
}

const EXAM_TYPES = [
  { value: "REGULAR", label: "Regular Practice" },
  { value: "UBT", label: "UBT (Computer-Based Test)" },
  { value: "TOPIK_I", label: "TOPIK I (Levels 1-2)" },
  { value: "TOPIK_II", label: "TOPIK II (Levels 3-6)" },
  { value: "DEMO", label: "Demo Exam" },
  { value: "CHAPTER", label: "Chapter Exam" },
  { value: "BATCH", label: "Batch Exam" },
  { value: "PLACEMENT", label: "Placement Test" },
];

export function AdminTests() {
  const [tests, setTests] = useState<Test[]>([]);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({
    title: "", description: "", durationMin: 30, isExam: false, examType: "REGULAR",
    passScore: 40, startAt: "", endAt: "", negativeMarking: 0, shuffleQuestions: false,
    showResultImmediately: true, maxAttempts: 1, isPublished: true,
  });

  function load() {
    fetch("/api/admin/tests").then(r => r.json()).then(d => setTests(d.tests || [])).catch(() => {});
  }
  useEffect(load, []);

  async function save() {
    if (!form.title.trim()) { toast.error("Title required"); return; }
    setBusy(true);
    const body = {
      ...form,
      durationMin: Number(form.durationMin),
      passScore: Number(form.passScore),
      negativeMarking: Number(form.negativeMarking),
      maxAttempts: Number(form.maxAttempts),
      startAt: form.startAt ? new Date(form.startAt).toISOString() : null,
      endAt: form.endAt ? new Date(form.endAt).toISOString() : null,
      questionIds: [], // questions are added separately via AdminQuestions
    };
    const res = await fetch("/api/admin/tests", {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Exam created — now add questions via Question Bank");
    setOpen(false);
    setForm({ title: "", description: "", durationMin: 30, isExam: false, examType: "REGULAR", passScore: 40, startAt: "", endAt: "", negativeMarking: 0, shuffleQuestions: false, showResultImmediately: true, maxAttempts: 1, isPublished: true });
    load();
  }

  async function toggleActive(id: string) {
    const res = await fetch(`/api/admin/tests/${id}/toggle-active`, { method: "POST" });
    if (res.ok) { toast.success("Exam status updated"); load(); }
  }

  async function del(id: string) {
    if (!confirm("Delete this exam and all its questions?")) return;
    await fetch(`/api/admin/tests/${id}`, { method: "DELETE" });
    toast.success("Deleted");
    load();
  }

  function examTypeBadge(type: string) {
    const colors: Record<string, string> = {
      UBT: "bg-purple-100 text-purple-700",
      TOPIK_I: "bg-blue-100 text-blue-700",
      TOPIK_II: "bg-indigo-100 text-indigo-700",
      DEMO: "bg-slate-100 text-slate-700",
    };
    return colors[type] || "bg-emerald-100 text-emerald-700";
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Exams & Tests ({tests.length})</h1>
          <p className="text-sm text-muted-foreground">Create exams with timer, type, schedule, and auto-submit.</p>
        </div>
        <Button onClick={() => setOpen(true)}><Plus className="mr-1 h-4 w-4" /> Create Exam</Button>
      </div>

      <div className="space-y-2">
        {tests.length === 0 ? (
          <Card><CardContent className="p-8 text-center text-muted-foreground">
            <FileText className="h-10 w-10 mx-auto mb-2 opacity-40" />
            <p>No exams yet. Create your first exam with timer and exam type.</p>
          </CardContent></Card>
        ) : tests.map(t => (
          <Card key={t.id} className={!t.isActive ? "opacity-60" : ""}>
            <CardContent className="p-4">
              <div className="flex items-start gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <div className="font-semibold">{t.title}</div>
                    <Badge className={`text-xs ${examTypeBadge(t.examType)}`}>{t.examType.replace(/_/g, " ")}</Badge>
                    {t.isExam && <Badge variant="destructive" className="text-xs">GRADED</Badge>}
                    {!t.isActive && <Badge variant="outline" className="text-xs text-rose-600">DEACTIVATED</Badge>}
                  </div>
                  {t.description && <p className="text-xs text-muted-foreground line-clamp-1">{t.description}</p>}
                  <div className="flex flex-wrap items-center gap-3 mt-2 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1"><Clock className="h-3 w-3" /> {t.durationMin} min</span>
                    <span>Pass: {t.passScore}%</span>
                    {t.negativeMarking > 0 && <span className="text-rose-500">−{t.negativeMarking}/wrong</span>}
                    {t.maxAttempts > 0 && <span>Max {t.maxAttempts} attempts</span>}
                    {t.shuffleQuestions && <span>Shuffled</span>}
                    <span>{t._count?.items || 0} questions</span>
                    <span>{t._count?.submissions || 0} submissions</span>
                    {t.startAt && <span className="flex items-center gap-1"><Calendar className="h-3 w-3" /> Start: {new Date(t.startAt).toLocaleString()}</span>}
                    {t.endAt && <span className="flex items-center gap-1"><Calendar className="h-3 w-3" /> End: {new Date(t.endAt).toLocaleString()}</span>}
                  </div>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  <Button size="sm" variant={t.isActive ? "outline" : "default"}
                    onClick={() => toggleActive(t.id)}
                    className={t.isActive ? "text-rose-600 border-rose-300 hover:bg-rose-50" : "bg-emerald-600 hover:bg-emerald-700"}>
                    {t.isActive ? <><PowerOff className="h-3 w-3 mr-1" /> Deactivate</> : <><Power className="h-3 w-3 mr-1" /> Activate</>}
                  </Button>
                  <Button size="sm" variant="ghost" onClick={() => del(t.id)}><Trash2 className="h-3 w-3 text-rose-500" /></Button>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Create exam dialog — full detail */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>Create Exam / Test</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <div><Label>Title</Label><Input value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder="TOPIK II Listening Practice" /></div>
            <div><Label>Description</Label><Textarea rows={2} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} /></div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>Exam Type</Label>
                <Select value={form.examType} onValueChange={v => setForm(f => ({ ...f, examType: v }))}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {EXAM_TYPES.map(t => <SelectItem key={t.value} value={t.value}>{t.label}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Duration (minutes)</Label>
                <Input type="number" value={form.durationMin} onChange={e => setForm(f => ({ ...f, durationMin: Number(e.target.value) }))} />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div><Label>Pass Score (%)</Label><Input type="number" value={form.passScore} onChange={e => setForm(f => ({ ...f, passScore: Number(e.target.value) }))} /></div>
              <div><Label>Negative Marking</Label><Input type="number" step="0.25" value={form.negativeMarking} onChange={e => setForm(f => ({ ...f, negativeMarking: Number(e.target.value) }))} /></div>
              <div><Label>Max Attempts (0=∞)</Label><Input type="number" value={form.maxAttempts} onChange={e => setForm(f => ({ ...f, maxAttempts: Number(e.target.value) }))} /></div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div><Label>Start Date/Time (optional)</Label><Input type="datetime-local" value={form.startAt} onChange={e => setForm(f => ({ ...f, startAt: e.target.value }))} /></div>
              <div><Label>End Date/Time (optional)</Label><Input type="datetime-local" value={form.endAt} onChange={e => setForm(f => ({ ...f, endAt: e.target.value }))} /></div>
            </div>

            <div className="flex items-center gap-4 pt-2">
              <label className="flex items-center gap-2 text-sm">
                <Switch checked={form.isExam} onCheckedChange={c => setForm(f => ({ ...f, isExam: c }))} /> Graded Exam
              </label>
              <label className="flex items-center gap-2 text-sm">
                <Switch checked={form.shuffleQuestions} onCheckedChange={c => setForm(f => ({ ...f, shuffleQuestions: c }))} /> Shuffle Questions
              </label>
              <label className="flex items-center gap-2 text-sm">
                <Switch checked={form.showResultImmediately} onCheckedChange={c => setForm(f => ({ ...f, showResultImmediately: c }))} /> Show Result Immediately
              </label>
            </div>

            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 flex items-start gap-2">
              <AlertCircle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
              <p className="text-xs text-amber-800">
                When the timer expires, the student's exam is <strong>auto-submitted</strong> and their result is shown immediately.
                If the exam is deactivated, students cannot start new attempts.
              </p>
            </div>

            <Button onClick={save} disabled={busy} className="w-full">{busy ? "Creating…" : "Create Exam"}</Button>
            <p className="text-xs text-center text-muted-foreground">After creating, add questions via the Question Bank and link them to this exam.</p>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
