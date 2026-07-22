"use client";

/**
 * Distinct admin pages — each sidebar item gets its own unique page.
 * No two sections share the same component.
 */

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Layers, FileText, BookOpen, BookMarked, Image, Package, School,
  Upload, Eye, ShoppingBag, BarChart3, Users, Plus, Trash2, Search,
  Filter, Download, Calendar, DollarSign, CheckCircle2, Clock
} from "lucide-react";
import { toast } from "sonner";

// ─── Demo Exams — practice tests (not graded) ───────────────────────────────
export function AdminDemoExams() {
  const [tests, setTests] = useState<any[]>([]);
  useEffect(() => {
    fetch("/api/student/tests").then(r => r.json()).then(d => setTests((d.tests || []).filter((t: any) => !t.isExam))).catch(() => {});
  }, []);
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold">Demo Exams</h1><p className="text-sm text-muted-foreground">Practice tests — not graded, no timer pressure.</p></div>
        <Button><Plus className="mr-1 h-4 w-4" /> Create Demo Exam</Button>
      </div>
      <div className="grid sm:grid-cols-2 gap-3">
        {tests.length === 0 ? <EmptyCard icon={FileText} message="No demo exams yet." /> :
          tests.map(t => <TestCard key={t.id} test={t} />)}
      </div>
    </div>
  );
}

// ─── Batch Exams — exams assigned to student batches ────────────────────────
export function AdminBatchExams() {
  const [batches, setBatches] = useState<any[]>([]);
  useEffect(() => { fetch("/api/admin/batches").then(r => r.json()).then(d => setBatches(d.batches || [])).catch(() => {}); }, []);
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold">Batch Exams</h1><p className="text-sm text-muted-foreground">Exams assigned to student batches / cohorts.</p></div>
        <Button><Plus className="mr-1 h-4 w-4" /> Assign Exam to Batch</Button>
      </div>
      <div className="space-y-2">
        {batches.length === 0 ? <EmptyCard icon={Layers} message="No batches created yet. Create a batch first, then assign exams." /> :
          batches.map(b => <Card key={b.id}><CardContent className="p-4 flex items-center gap-4">
            <div className="h-10 w-10 rounded-lg bg-blue-50 grid place-items-center"><Layers className="h-5 w-5 text-blue-600" /></div>
            <div className="flex-1"><div className="font-medium">{b.name}</div><div className="text-xs text-muted-foreground">{b.description}</div></div>
            <Badge variant="secondary">{b._count?.students || 0} students</Badge>
          </CardContent></Card>)}
      </div>
    </div>
  );
}

// ─── Chapter Exams — exams linked to book chapters ──────────────────────────
export function AdminChapterExams() {
  const [tests, setTests] = useState<any[]>([]);
  useEffect(() => {
    fetch("/api/student/tests").then(r => r.json()).then(d => setTests((d.tests || []).filter((t: any) => t.chapterId))).catch(() => {});
  }, []);
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold">Chapter Exams</h1><p className="text-sm text-muted-foreground">Exams tied to specific book chapters.</p></div>
        <Button><Plus className="mr-1 h-4 w-4" /> Create Chapter Exam</Button>
      </div>
      <div className="grid sm:grid-cols-2 gap-3">
        {tests.length === 0 ? <EmptyCard icon={BookOpen} message="No chapter exams yet. Link an exam to a book chapter." /> :
          tests.map(t => <TestCard key={t.id} test={t} />)}
      </div>
    </div>
  );
}

// ─── Question Categories — manage subject/chapter taxonomy ──────────────────
export function AdminQuestionCategories() {
  const [subjects, setSubjects] = useState<any[]>([]);
  useEffect(() => { fetch("/api/admin/subjects").then(r => r.json()).then(d => setSubjects(d.subjects || [])).catch(() => {}); }, []);
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold">Question Categories</h1><p className="text-sm text-muted-foreground">Organize questions by subject and chapter.</p></div>
        <Button><Plus className="mr-1 h-4 w-4" /> Add Category</Button>
      </div>
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
        {subjects.length === 0 ? <EmptyCard icon={BookMarked} message="No categories yet. Create subjects to organize questions." /> :
          subjects.map(s => <Card key={s.id}><CardContent className="p-4">
            <div className="font-semibold">{s.name}</div>
            <div className="text-xs text-muted-foreground mt-1">{s.description}</div>
            <div className="flex items-center justify-between mt-3">
              <Badge variant="secondary">{s._count?.chapters || 0} chapters</Badge>
              <Button size="sm" variant="ghost"><Plus className="h-3 w-3" /></Button>
            </div>
          </CardContent></Card>)}
      </div>
    </div>
  );
}

// ─── All Courses — manage subjects/courses ──────────────────────────────────
export function AdminAllCourses() {
  const [subjects, setSubjects] = useState<any[]>([]);
  useEffect(() => { fetch("/api/admin/subjects").then(r => r.json()).then(d => setSubjects(d.subjects || [])).catch(() => {}); }, []);
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold">All Courses</h1><p className="text-sm text-muted-foreground">Manage learning subjects and their chapters.</p></div>
        <Button><Plus className="mr-1 h-4 w-4" /> Add Course</Button>
      </div>
      <div className="space-y-2">
        {subjects.length === 0 ? <EmptyCard icon={BookOpen} message="No courses yet. Add a subject to start building curriculum." /> :
          subjects.map(s => <Card key={s.id}><CardContent className="p-4 flex items-center gap-4">
            <div className="h-10 w-10 rounded-lg bg-emerald-50 grid place-items-center"><BookOpen className="h-5 w-5 text-emerald-600" /></div>
            <div className="flex-1"><div className="font-medium">{s.name}</div><div className="text-xs text-muted-foreground">{s.description}</div></div>
            <div className="flex items-center gap-3 text-xs text-muted-foreground">
              <span>{s._count?.chapters || 0} chapters</span>
              <span>{s._count?.lessons || 0} lessons</span>
            </div>
          </CardContent></Card>)}
      </div>
    </div>
  );
}

// ─── Batch Management — create/manage student cohorts ───────────────────────
export function AdminBatch() {
  const [batches, setBatches] = useState<any[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ name: "", description: "" });

  function load() { fetch("/api/admin/batches").then(r => r.json()).then(d => setBatches(d.batches || [])).catch(() => {}); }
  useEffect(load, []);

  async function create() {
    if (!form.name.trim()) return;
    const slug = form.name.toLowerCase().replace(/[^a-z0-9]+/g, "-");
    const res = await fetch("/api/admin/batches", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ ...form, slug }) });
    if (res.ok) { toast.success("Batch created"); setOpen(false); setForm({ name: "", description: "" }); load(); }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold">Batch Management</h1><p className="text-sm text-muted-foreground">Create student batches, assign exams and track progress.</p></div>
        <Button onClick={() => setOpen(true)}><Plus className="mr-1 h-4 w-4" /> Create Batch</Button>
      </div>
      <div className="space-y-2">
        {batches.length === 0 ? <EmptyCard icon={Layers} message="No batches yet. Create a batch to group students." /> :
          batches.map(b => <Card key={b.id}><CardContent className="p-4 flex items-center gap-4">
            <div className="h-10 w-10 rounded-lg bg-blue-50 grid place-items-center"><Layers className="h-5 w-5 text-blue-600" /></div>
            <div className="flex-1"><div className="font-medium">{b.name}</div><div className="text-xs text-muted-foreground">{b.description}</div></div>
            <Badge variant="secondary">{b._count?.students || 0} students</Badge>
            <Badge variant="outline">{b._count?.exams || 0} exams</Badge>
          </CardContent></Card>)}
      </div>
      <Dialog open={open} onOpenChange={setOpen}><DialogContent>
        <DialogHeader><DialogTitle>Create Batch</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div><Label>Batch Name</Label><Input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="TOPIK II — Morning Batch" /></div>
          <div><Label>Description</Label><Textarea rows={2} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} /></div>
          <Button onClick={create} className="w-full">Create Batch</Button>
        </div>
      </DialogContent></Dialog>
    </div>
  );
}

// ─── PDF Viewer — upload and view PDF files ─────────────────────────────────
export function AdminPDFViewer() {
  const [url, setUrl] = useState("");
  const [uploaded, setUploaded] = useState(false);
  return (
    <div className="space-y-4">
      <div><h1 className="text-2xl font-bold">PDF Viewer</h1><p className="text-sm text-muted-foreground">Upload and view PDF documents — textbooks, worksheets, exam papers.</p></div>
      <Card><CardContent className="p-6">
        <div className="border-2 border-dashed border-slate-300 rounded-xl p-8 text-center">
          <Upload className="h-10 w-10 mx-auto mb-2 text-slate-400" />
          <p className="text-sm text-muted-foreground mb-3">Drop a PDF here or click to upload</p>
          <Input type="file" accept=".pdf" className="max-w-xs mx-auto" onChange={e => { const f = e.target.files?.[0]; if (f) { setUrl(URL.createObjectURL(f)); setUploaded(true); toast.success("PDF loaded"); } }} />
        </div>
        {uploaded && url && (
          <div className="mt-4">
            <iframe src={url} className="w-full h-[600px] rounded-lg border" title="PDF Viewer" />
          </div>
        )}
      </CardContent></Card>
    </div>
  );
}

// ─── Color Vision Test — Ishihara-style screening ───────────────────────────
export function AdminColorVision() {
  return (
    <div className="space-y-4">
      <div><h1 className="text-2xl font-bold">Color Vision Test</h1><p className="text-sm text-muted-foreground">Ishihara-style color vision screening for student assessment.</p></div>
      <div className="grid sm:grid-cols-3 gap-4">
        {["Plate 1", "Plate 2", "Plate 3"].map((p, i) => (
          <Card key={p}><CardContent className="p-6 text-center">
            <div className="h-32 w-32 mx-auto rounded-full mb-3" style={{ background: `hsl(${i * 120}, 70%, 60%)` }} />
            <div className="font-medium">{p}</div>
            <div className="text-xs text-muted-foreground mt-1">Number: {12 + i * 3}</div>
          </CardContent></Card>
        ))}
      </div>
      <Card><CardContent className="p-4">
        <p className="text-sm text-muted-foreground">Students view plates and enter the number they see. Results are logged to their profile for accessibility accommodations.</p>
        <Button className="mt-3"><Plus className="mr-1 h-4 w-4" /> Add Test Plate</Button>
      </CardContent></Card>
    </div>
  );
}

// ─── Package Results — results by exam package ──────────────────────────────
export function AdminPackageResults() {
  return (
    <div className="space-y-4">
      <div><h1 className="text-2xl font-bold">Package Results</h1><p className="text-sm text-muted-foreground">Performance data for bundled exam packages.</p></div>
      <Card><CardContent className="p-8 text-center text-muted-foreground">
        <Package className="h-10 w-10 mx-auto mb-2 opacity-40" />
        <p>Create exam packages to track aggregate results across multiple tests.</p>
        <Button className="mt-3" variant="outline"><Plus className="mr-1 h-4 w-4" /> Create Package</Button>
      </CardContent></Card>
    </div>
  );
}

// ─── Classroom Results — results by classroom/batch ─────────────────────────
export function AdminClassroomResults() {
  return (
    <div className="space-y-4">
      <div><h1 className="text-2xl font-bold">Classroom Results</h1><p className="text-sm text-muted-foreground">Performance data by classroom and batch.</p></div>
      <Card><CardContent className="p-8 text-center text-muted-foreground">
        <School className="h-10 w-10 mx-auto mb-2 opacity-40" />
        <p>Assign students to classrooms, then view aggregate and individual results per class.</p>
        <Button className="mt-3" variant="outline"><Plus className="mr-1 h-4 w-4" /> Create Classroom</Button>
      </CardContent></Card>
    </div>
  );
}

// ─── Orders — paid exam/course orders ───────────────────────────────────────
export function AdminOrders({ type }: { type: string }) {
  return (
    <div className="space-y-4">
      <div><h1 className="text-2xl font-bold">{type}</h1><p className="text-sm text-muted-foreground">Track and manage purchases.</p></div>
      <div className="grid grid-cols-3 gap-3">
        <Card><CardContent className="p-4"><div className="flex items-center gap-3"><div className="h-10 w-10 rounded-lg bg-emerald-50 grid place-items-center"><DollarSign className="h-5 w-5 text-emerald-600" /></div><div><div className="text-2xl font-bold">₩0</div><div className="text-xs text-muted-foreground">Revenue</div></div></div></CardContent></Card>
        <Card><CardContent className="p-4"><div className="flex items-center gap-3"><div className="h-10 w-10 rounded-lg bg-blue-50 grid place-items-center"><ShoppingBag className="h-5 w-5 text-blue-600" /></div><div><div className="text-2xl font-bold">0</div><div className="text-xs text-muted-foreground">Orders</div></div></div></CardContent></Card>
        <Card><CardContent className="p-4"><div className="flex items-center gap-3"><div className="h-10 w-10 rounded-lg bg-amber-50 grid place-items-center"><Clock className="h-5 w-5 text-amber-600" /></div><div><div className="text-2xl font-bold">0</div><div className="text-xs text-muted-foreground">Pending</div></div></div></CardContent></Card>
      </div>
      <Card><CardContent className="p-8 text-center text-muted-foreground">
        <ShoppingBag className="h-10 w-10 mx-auto mb-2 opacity-40" />
        <p>No orders yet. Enable paid exams to start tracking revenue.</p>
      </CardContent></Card>
    </div>
  );
}

// ─── Shared helpers ─────────────────────────────────────────────────────────
function EmptyCard({ icon: Icon, message }: { icon: any; message: string }) {
  return <Card className="col-span-full"><CardContent className="p-8 text-center text-muted-foreground"><Icon className="h-10 w-10 mx-auto mb-2 opacity-40" /><p>{message}</p></CardContent></Card>;
}

function TestCard({ test }: { test: any }) {
  return (
    <Card><CardContent className="p-4">
      <div className="flex items-start justify-between mb-2">
        <div className="font-semibold">{test.title}</div>
        {test.isExam ? <Badge variant="destructive" className="text-xs">Exam</Badge> : <Badge variant="secondary" className="text-xs">Practice</Badge>}
      </div>
      <p className="text-xs text-muted-foreground line-clamp-2">{test.description}</p>
      <div className="flex items-center gap-3 mt-2 text-xs text-muted-foreground">
        <span className="flex items-center gap-1"><Clock className="h-3 w-3" /> {test.durationMin} min</span>
        <span>{test.questionCount || 0} questions</span>
      </div>
    </CardContent></Card>
  );
}
