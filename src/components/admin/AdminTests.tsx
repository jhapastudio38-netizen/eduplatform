"use client";

import { useEffect, useState, useRef } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { FileText, Plus, Trash2, Clock, Upload, X, ChevronRight, ChevronLeft, Image as ImageIcon, Headphones, CheckCircle2, Copy, ClipboardPaste, Save } from "lucide-react";
import { toast } from "sonner";

interface Test {
  id: string;
  title: string;
  description?: string;
  durationMin: number;
  isExam: boolean;
  examType: string;
  testCategory?: string | null;
  passScore: number;
  isActive: boolean;
  isPublished: boolean;
  category?: string | null;
  featuredImage?: string | null;
  price?: number | null;
  audioPlayMode?: string | null;
  audioGapSec?: number | null;
  textBlockCount?: number | null;
  audioBlockCount?: number | null;
  _count?: { items: number };
}

interface QuestionData {
  id?: string;
  testItemId?: string;
  blockType: "text" | "audio";
  blockNumber: number;
  stem: string;
  descType: "none" | "text" | "image" | "audio";
  descText: string;
  descImageUrl: string;
  descAudioUrl: string;
  mediaType: "none" | "text" | "image" | "audio";
  mediaText: string;
  mediaImageUrl: string;
  mediaAudioUrl: string;
  answerType: "text" | "image" | "audio" | "choose";
  options: string[];
  optionImages: string[];
  optionAudios: string[];
  correctOption: number;
  explanation: string;
}

function emptyQuestion(blockType: "text" | "audio", blockNumber: number): QuestionData {
  return {
    blockType,
    blockNumber,
    stem: "",
    descType: "none",
    descText: "",
    descImageUrl: "",
    descAudioUrl: "",
    mediaType: blockType === "audio" ? "audio" : "none",
    mediaText: "",
    mediaImageUrl: "",
    mediaAudioUrl: "",
    answerType: "text",
    options: ["", "", "", ""],
    optionImages: ["", "", "", ""],
    optionAudios: ["", "", "", ""],
    correctOption: 0,
    explanation: "",
  };
}

// Categories that use the simple "flat list" question editor
// (instead of the 20+20 block grid used by exam & demo).
const SIMPLE_CATEGORIES = ["batch", "chapter", "question_bank"];

const CATEGORY_META: Record<string, { title: string; subtitle: string; createLabel: string }> = {
  exam:          { title: "Exams",          subtitle: "Block-based exam builder — 20 text + 20 audio questions", createLabel: "New Exam" },
  demo:          { title: "Demo Exams",     subtitle: "Practice tests — same block builder as exams",            createLabel: "New Demo Exam" },
  batch:         { title: "Batch Exams",    subtitle: "Exams assigned to student batches — add questions freely",  createLabel: "New Batch Exam" },
  chapter:       { title: "Chapter Exams",  subtitle: "Chapter-scoped exams — add questions freely",                createLabel: "New Chapter Exam" },
  question_bank: { title: "Question Bank",  subtitle: "Reusable question bank — add questions freely",              createLabel: "New Question Set" },
};

export function AdminTests({ testCategory = "exam" }: { testCategory?: string }) {
  const [tests, setTests] = useState<Test[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [editingTest, setEditingTest] = useState<Test | null>(null);

  const meta = CATEGORY_META[testCategory] || CATEGORY_META.exam;

  function load() {
    setLoading(true);
    fetch(`/api/admin/tests?category=${encodeURIComponent(testCategory)}`)
      .then((r) => r.json())
      .then((d) => setTests(d.tests || []))
      .finally(() => setLoading(false));
  }
  useEffect(load, [testCategory]);

  async function deleteTest(test: Test) {
    if (!confirm(`Delete "${test.title}"? This removes all questions.`)) return;
    await fetch(`/api/admin/tests/${test.id}`, { method: "DELETE" });
    toast.success("Exam deleted");
    load();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{meta.title} ({tests.length})</h1>
          <p className="text-sm text-muted-foreground">{meta.subtitle}</p>
        </div>
        <Button size="lg" onClick={() => setCreateOpen(true)}>
          <Plus className="w-4 h-4 mr-1" /> {meta.createLabel}
        </Button>
      </div>

      {loading ? (
        <p className="text-center py-8 text-muted-foreground">Loading…</p>
      ) : tests.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <FileText className="w-12 h-12 mx-auto mb-3 text-muted-foreground" />
            <p className="text-muted-foreground">Nothing here yet. Click "{meta.createLabel}" to create one.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-3">
          {tests.map((t) => (
            <Card key={t.id} className="hover:shadow-md transition-shadow cursor-pointer" onClick={() => setEditingTest(t)}>
              <CardContent className="flex items-center gap-4 py-4">
                {t.featuredImage ? (
                  <img src={t.featuredImage} alt="" className="w-16 h-16 rounded-lg object-cover" />
                ) : (
                  <div className="w-16 h-16 rounded-lg bg-slate-100 flex items-center justify-center">
                    <FileText className="w-6 h-6 text-slate-400" />
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-semibold truncate">{t.title}</h3>
                    <Badge variant="outline">{t.examType}</Badge>
                    {t.category && <Badge variant="secondary">{t.category}</Badge>}
                    {t.price ? <Badge>₩{t.price}</Badge> : <Badge variant="outline">Free</Badge>}
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">
                    {t.durationMin} min • {t._count?.items || 0} questions • Pass {t.passScore}%
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  {t.isPublished ? (
                    <Badge className="bg-green-500">🚀 Live</Badge>
                  ) : (
                    <Badge variant="secondary">📝 Draft</Badge>
                  )}
                  <Button variant="ghost" size="icon" onClick={(e) => { e.stopPropagation(); deleteTest(t); }}>
                    <Trash2 className="w-4 h-4 text-red-500" />
                  </Button>
                  <ChevronRight className="w-5 h-5 text-muted-foreground" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {createOpen && (
        <CreateExamDialog
          open={createOpen}
          testCategory={testCategory}
          onOpenChange={setCreateOpen}
          onCreated={(t) => { setCreateOpen(false); setEditingTest(t); load(); }}
        />
      )}

      {editingTest && (
        <ExamEditor
          test={editingTest}
          testCategory={testCategory}
          onClose={() => { setEditingTest(null); load(); }}
        />
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// CREATE EXAM DIALOG — Step 1: exam details
// ═══════════════════════════════════════════════════════════════════════════

function CreateExamDialog({ open, testCategory, onOpenChange, onCreated }: {
  open: boolean;
  testCategory: string;
  onOpenChange: (v: boolean) => void;
  onCreated: (t: Test) => void;
}) {
  const isSimple = SIMPLE_CATEGORIES.includes(testCategory);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({
    title: "",
    description: "",
    durationMin: 60,
    examType: "UBT",
    category: "",
    price: "",
    featuredImage: "",
    audioPlayMode: "single" as "single" | "double",
    audioGapSec: 2,
    textBlockCount: isSimple ? 0 : 20,
    audioBlockCount: isSimple ? 0 : 20,
  });

  async function uploadFile(file: File, folder: string): Promise<string> {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("folder", folder);
    const res = await fetch("/api/admin/file-upload", { method: "POST", body: fd });
    if (!res.ok) { const d = await res.json(); throw new Error(d.error || "Upload failed"); }
    const d = await res.json();
    return d.url;
  }

  async function create() {
    if (!form.title.trim()) { toast.error("Exam name required"); return; }
    setBusy(true);
    try {
      const payload = {
        title: form.title,
        description: form.description,
        durationMin: form.durationMin,
        examType: form.examType,
        testCategory,
        category: form.category || undefined,
        price: form.price ? parseFloat(form.price) : undefined,
        featuredImage: form.featuredImage || undefined,
        audioPlayMode: form.audioPlayMode,
        audioGapSec: form.audioGapSec,
        textBlockCount: form.textBlockCount,
        audioBlockCount: form.audioBlockCount,
        isExam: true,
        isPublished: false, // Draft — admin pushes when ready
      };
      const res = await fetch("/api/admin/tests", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || `Failed (HTTP ${res.status})`); return; }
      toast.success("Created — now add questions");
      onCreated(d.test);
    } catch (e: any) {
      toast.error("Create failed: " + (e.message || "network error"));
    } finally { setBusy(false); }
  }

  const dialogTitle = isSimple
    ? `Create New ${CATEGORY_META[testCategory]?.title || "Set"}`
    : "Create New Exam";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader><DialogTitle className="text-xl">{dialogTitle}</DialogTitle></DialogHeader>
        <div className="space-y-4">
          {/* Exam Name */}
          <div>
            <Label className="text-sm font-semibold">Name *</Label>
            <Input
              value={form.title}
              onChange={(e) => setForm(f => ({ ...f, title: e.target.value }))}
              placeholder="e.g. UBT Mock Test 1"
              className="h-12 text-base"
            />
          </div>

          {/* Exam Details */}
          <div>
            <Label className="text-sm font-semibold">Description</Label>
            <Textarea
              rows={2}
              value={form.description}
              onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))}
              placeholder="Brief description of the exam"
            />
          </div>

          {/* Exam Time + Price */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label className="text-sm font-semibold">Time (minutes) *</Label>
              <Input type="number" value={form.durationMin} onChange={(e) => setForm(f => ({ ...f, durationMin: parseInt(e.target.value) || 60 }))} min={1} />
            </div>
            <div>
              <Label className="text-sm font-semibold">Price (optional)</Label>
              <Input type="number" value={form.price} onChange={(e) => setForm(f => ({ ...f, price: e.target.value }))} placeholder="0 = free" min={0} />
            </div>
          </div>

          {/* Exam Type + Category */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label className="text-sm font-semibold">Exam Type *</Label>
              <Select value={form.examType} onValueChange={(v) => setForm(f => ({ ...f, examType: v }))}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="UBT">UBT</SelectItem>
                  <SelectItem value="CBT">CBT</SelectItem>
                  <SelectItem value="CHAPTER">Chapter</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label className="text-sm font-semibold">Category (shows in app)</Label>
              <Input value={form.category} onChange={(e) => setForm(f => ({ ...f, category: e.target.value }))} placeholder="e.g. Beginner, TOPIK 1" />
            </div>
          </div>

          {/* Featured Image */}
          <div>
            <Label className="text-sm font-semibold">Featured Image</Label>
            <div className="flex gap-2">
              <Input value={form.featuredImage} onChange={(e) => setForm(f => ({ ...f, featuredImage: e.target.value }))} placeholder="Upload or paste URL…" className="flex-1" />
              <label className="cursor-pointer">
                <input type="file" accept="image/*" className="hidden" onChange={async (e) => {
                  const f = e.target.files?.[0]; if (!f) return;
                  try { const url = await uploadFile(f, "exam-featured"); setForm(p => ({ ...p, featuredImage: url })); toast.success("Image uploaded"); }
                  catch (err: any) { toast.error(err.message); }
                }} />
                <span className="inline-flex items-center h-10 px-4 rounded-md bg-primary text-primary-foreground text-sm cursor-pointer hover:bg-primary/90">📁 Upload</span>
              </label>
            </div>
            {form.featuredImage && <img src={form.featuredImage} alt="Featured" className="mt-2 max-h-32 rounded border" />}
          </div>

          {/* Audio Settings — only for exam & demo (block-based) */}
          {!isSimple && (
            <div className="p-4 border rounded-lg bg-slate-50 space-y-3">
              <Label className="text-base font-semibold">Audio Settings (optional)</Label>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label>Play Mode</Label>
                  <Select value={form.audioPlayMode} onValueChange={(v: any) => setForm(f => ({ ...f, audioPlayMode: v }))}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="single">Single (play once)</SelectItem>
                      <SelectItem value="double">Double (play twice)</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label>Gap Between Plays (seconds)</Label>
                  <Input type="number" value={form.audioGapSec} onChange={(e) => setForm(f => ({ ...f, audioGapSec: parseInt(e.target.value) || 2 }))} min={0} max={60} />
                </div>
              </div>
            </div>
          )}

          {/* Block Counts — only for exam & demo */}
          {!isSimple && (
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label className="text-sm font-semibold">Text Questions Count</Label>
                <Input type="number" value={form.textBlockCount} onChange={(e) => setForm(f => ({ ...f, textBlockCount: parseInt(e.target.value) || 20 }))} min={1} max={100} />
              </div>
              <div>
                <Label className="text-sm font-semibold">Audio Questions Count</Label>
                <Input type="number" value={form.audioBlockCount} onChange={(e) => setForm(f => ({ ...f, audioBlockCount: parseInt(e.target.value) || 20 }))} min={1} max={100} />
              </div>
            </div>
          )}

          {/* Simple mode hint */}
          {isSimple && (
            <div className="p-4 border rounded-lg bg-emerald-50 border-emerald-200 text-sm text-emerald-800">
              Questions can be added freely after creation — there is no fixed block layout for this category.
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-2 border-t">
          <Button variant="outline" size="lg" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button size="lg" onClick={create} disabled={busy}>
            {busy ? "Creating…" : <><Plus className="w-4 h-4 mr-1" /> Create</>}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// EXAM EDITOR — Step 2: block-based question editor
// ═══════════════════════════════════════════════════════════════════════════

function ExamEditor({ test, testCategory, onClose }: { test: Test; testCategory: string; onClose: () => void }) {
  const isSimple = SIMPLE_CATEGORIES.includes(testCategory);
  const [activeBlock, setActiveBlock] = useState<"text" | "audio">("text");
  const [activeNumber, setActiveNumber] = useState(1);
  const [questions, setQuestions] = useState<Record<string, QuestionData>>({});
  const [loading, setLoading] = useState(true);
  const [clipboard, setClipboard] = useState<string>("");
  const [showPasteDialog, setShowPasteDialog] = useState(false);
  const [pasteCode, setPasteCode] = useState("");
  const [pushing, setPushing] = useState(false);
  const [isPublished, setIsPublished] = useState(test.isPublished);

  const textCount = test.textBlockCount || (isSimple ? 0 : 20);
  const audioCount = test.audioBlockCount || (isSimple ? 0 : 20);

  function key(blockType: string, blockNumber: number) {
    return `${blockType}-${blockNumber}`;
  }

  useEffect(() => {
    // Load existing questions
    fetch(`/api/admin/tests/${test.id}/questions`)
      .then((r) => r.json())
      .then((d) => {
        const map: Record<string, QuestionData> = {};
        for (const q of d.questions || []) {
          map[key(q.blockType, q.blockNumber)] = q;
        }
        setQuestions(map);
      })
      .finally(() => setLoading(false));
  }, [test.id]);

  const currentKey = key(activeBlock, activeNumber);
  const currentQuestion = questions[currentKey] || emptyQuestion(activeBlock, activeNumber);

  function updateQuestion(q: QuestionData) {
    setQuestions((prev) => ({ ...prev, [currentKey]: q }));
  }

  async function saveQuestion() {
    const q = currentQuestion;
    if (!q.stem.trim()) { toast.error("Question text required"); return; }
    try {
      // Strip fields the API doesn't expect
      const payload = {
        blockType: q.blockType,
        blockNumber: q.blockNumber,
        stem: q.stem,
        descType: q.descType,
        descText: q.descText || "",
        descImageUrl: q.descImageUrl || "",
        descAudioUrl: q.descAudioUrl || "",
        mediaType: q.mediaType,
        mediaText: q.mediaText || "",
        mediaImageUrl: q.mediaImageUrl || "",
        mediaAudioUrl: q.mediaAudioUrl || "",
        answerType: q.answerType,
        options: q.options || [],
        optionImages: q.optionImages || [],
        optionAudios: q.optionAudios || [],
        correctOption: q.correctOption,
        explanation: q.explanation || "",
      };
      const res = await fetch(`/api/admin/tests/${test.id}/questions`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const d = await res.json();
      if (!res.ok) {
        toast.error(d.error || `Save failed (HTTP ${res.status})`);
        return;
      }
      toast.success(`Question ${q.blockNumber} saved`);
    } catch (e: any) {
      toast.error("Save failed: " + (e.message || "network error"));
    }
  }

  function copyQuestion() {
    const q = currentQuestion;
    if (!q.stem.trim()) { toast.error("Nothing to copy — question is empty"); return; }
    // Generate a copy code — admin can define their own prefix
    const code = `DK-${activeBlock.toUpperCase()}-${q.blockNumber}-${Date.now().toString(36).toUpperCase()}`;
    const data = JSON.stringify({ code, question: q });
    // Store in localStorage so admin can paste later
    const allCopies = JSON.parse(localStorage.getItem("dk_copies") || "{}");
    allCopies[code] = data;
    localStorage.setItem("dk_copies", JSON.stringify(allCopies));
    setClipboard(code);
    // Copy code to clipboard
    navigator.clipboard.writeText(code);
    toast.success(`Copied! Code: ${code}`);
  }

  function pasteQuestion() {
    setShowPasteDialog(true);
  }

  async function pushToApp() {
    const filledCount = Object.values(questions).filter(q => q.stem.trim()).length;
    if (filledCount === 0) {
      toast.error("Cannot push: add at least one question first");
      return;
    }
    if (!confirm(`Push this exam to all student apps?\n\n${filledCount} question(s) will be live.\n\nStudents will see it in their Exams page immediately.`)) {
      return;
    }
    setPushing(true);
    try {
      const res = await fetch(`/api/admin/tests/${test.id}/publish`, { method: "POST" });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Push failed"); return; }
      setIsPublished(true);
      toast.success(d.message || "Pushed to app — students can now see this exam");
    } catch {
      toast.error("Push failed — check your connection");
    } finally {
      setPushing(false);
    }
  }

  function doPaste() {
    const allCopies = JSON.parse(localStorage.getItem("dk_copies") || "{}");
    const data = allCopies[pasteCode.trim()];
    if (!data) { toast.error("Invalid paste code"); return; }
    const parsed = JSON.parse(data);
    const q = parsed.question as QuestionData;
    // Paste into current slot — keep current block number/type
    const pasted: QuestionData = {
      ...q,
      blockType: activeBlock,
      blockNumber: activeNumber,
    };
    updateQuestion(pasted);
    toast.success("Question pasted — click Save to persist");
    setShowPasteDialog(false);
    setPasteCode("");
  }

  const blockNumbers = activeBlock === "text"
    ? Array.from({ length: textCount }, (_, i) => i + 1)
    : Array.from({ length: audioCount }, (_, i) => i + 1);

  // Simple-mode list of saved questions (sorted by block number)
  const simpleList = Object.values(questions)
    .filter((q) => q.stem.trim())
    .sort((a, b) => a.blockNumber - b.blockNumber);

  function addQuestion() {
    // Pick the next free block number for the "text" block type
    const used = new Set(Object.values(questions).map((q) => q.blockNumber));
    let next = 1;
    while (used.has(next)) next++;
    setActiveBlock("text");
    setActiveNumber(next);
    // Pre-seed an empty question so it appears in the list immediately
    const k = key("text", next);
    if (!questions[k]) {
      setQuestions((prev) => ({ ...prev, [k]: emptyQuestion("text", next) }));
    }
  }

  function deleteActiveQuestion() {
    const k = key(activeBlock, activeNumber);
    const q = questions[k];
    if (!q) return;
    if (!confirm(`Delete question ${q.blockNumber}?`)) return;
    // Remove locally
    const next = { ...questions };
    delete next[k];
    setQuestions(next);
    // If it was persisted server-side, also delete via API
    if (q.testItemId) {
      fetch(`/api/admin/tests/${test.id}/questions/${q.testItemId}`, { method: "DELETE" }).catch(() => {});
    }
    toast.success("Question removed");
    // Move selection to the first remaining question
    const remaining = Object.values(next).sort((a, b) => a.blockNumber - b.blockNumber);
    if (remaining.length > 0) {
      setActiveBlock(remaining[0].blockType);
      setActiveNumber(remaining[0].blockNumber);
    }
  }

  return (
    <Dialog open={true} onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent className="max-w-5xl max-h-[95vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <span>{test.title}</span>
            <Badge variant="outline">{test.examType}</Badge>
            {test.category && <Badge variant="secondary">{test.category}</Badge>}
          </DialogTitle>
        </DialogHeader>

        {/* Block tabs — only for exam & demo */}
        {!isSimple && (
          <div className="flex gap-2 border-b pb-2">
            <Button
              variant={activeBlock === "text" ? "default" : "outline"}
              size="sm"
              onClick={() => { setActiveBlock("text"); setActiveNumber(1); }}
            >
              <FileText className="w-4 h-4 mr-1" /> Text Block (1-{textCount})
            </Button>
            <Button
              variant={activeBlock === "audio" ? "default" : "outline"}
              size="sm"
              onClick={() => { setActiveBlock("audio"); setActiveNumber(1); }}
            >
              <Headphones className="w-4 h-4 mr-1" /> Audio Block ({textCount + 1}-{textCount + audioCount})
            </Button>
          </div>
        )}

        {/* Block number selector — grid of numbered buttons (exam & demo only) */}
        {!isSimple && (
          <div className="grid grid-cols-10 gap-1 max-h-24 overflow-y-auto p-1 bg-slate-50 rounded">
            {blockNumbers.map((num) => {
              const k = key(activeBlock, num);
              const isFilled = questions[k] && questions[k].stem.trim();
              const isActive = num === activeNumber;
              return (
                <button
                  key={num}
                  onClick={() => setActiveNumber(num)}
                  className={`h-8 rounded text-xs font-medium transition-colors ${
                    isActive ? "bg-primary text-primary-foreground" :
                    isFilled ? "bg-green-100 text-green-700 border border-green-300" :
                    "bg-white border hover:bg-slate-100"
                  }`}
                >
                  {num}
                </button>
              );
            })}
          </div>
        )}

        {/* Simple question list — for batch / chapter / question_bank */}
        {isSimple && (
          <div className="space-y-2 border-b pb-3">
            <div className="flex items-center justify-between">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                Questions ({simpleList.length})
              </p>
              <Button size="sm" onClick={addQuestion}>
                <Plus className="w-4 h-4 mr-1" /> Add Question
              </Button>
            </div>
            <div className="flex gap-2 flex-wrap max-h-24 overflow-y-auto p-1 bg-slate-50 rounded">
              {simpleList.length === 0 && (
                <p className="text-xs text-muted-foreground p-2">
                  No questions yet. Click &ldquo;Add Question&rdquo; to start.
                </p>
              )}
              {simpleList.map((q) => {
                const isActive = q.blockType === activeBlock && q.blockNumber === activeNumber;
                return (
                  <button
                    key={key(q.blockType, q.blockNumber)}
                    onClick={() => { setActiveBlock(q.blockType); setActiveNumber(q.blockNumber); }}
                    className={`h-8 px-3 rounded text-xs font-medium transition-colors ${
                      isActive ? "bg-primary text-primary-foreground" :
                      "bg-green-100 text-green-700 border border-green-300 hover:bg-green-200"
                    }`}
                    title={q.stem}
                  >
                    Q{q.blockNumber}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Question editor */}
        <div className="flex-1 overflow-y-auto pr-1">
          {loading ? (
            <p className="text-center py-8 text-muted-foreground">Loading questions…</p>
          ) : (
            <QuestionEditor
              question={currentQuestion}
              onChange={updateQuestion}
              blockLabel={`Question ${activeBlock === "audio" ? textCount + activeNumber : activeNumber}`}
              isAudioBlock={activeBlock === "audio"}
            />
          )}
        </div>

        {/* Action buttons — Done, Copy, Paste, Delete (simple), Push to App */}
        <div className="flex items-center justify-between border-t pt-3">
          <div className="flex gap-2 flex-wrap">
            <Button onClick={saveQuestion} variant="default" size="lg">
              <Save className="w-4 h-4 mr-1" /> Save
            </Button>
            <Button onClick={copyQuestion} variant="outline" size="lg">
              <Copy className="w-4 h-4 mr-1" /> Copy
            </Button>
            <Button onClick={pasteQuestion} variant="outline" size="lg">
              <ClipboardPaste className="w-4 h-4 mr-1" /> Paste
            </Button>
            {isSimple && (
              <Button onClick={deleteActiveQuestion} variant="outline" size="lg" className="text-rose-600 hover:text-rose-700">
                <Trash2 className="w-4 h-4 mr-1" /> Delete
              </Button>
            )}
          </div>
          <div className="flex items-center gap-2">
            {isPublished && (
              <Badge className="bg-green-500">
                <CheckCircle2 className="w-3 h-3 mr-1" /> Live in App
              </Badge>
            )}
            <Button
              onClick={pushToApp}
              disabled={pushing || isPublished}
              variant={isPublished ? "outline" : "default"}
              size="lg"
              className={isPublished ? "" : "bg-green-600 hover:bg-green-700"}
            >
              {pushing ? (
                <><span className="animate-spin mr-1">⏳</span> Pushing…</>
              ) : isPublished ? (
                <><CheckCircle2 className="w-4 h-4 mr-1" /> Pushed</>
              ) : (
                <><span className="mr-1">🚀</span> Push to App</>
              )}
            </Button>
            <Button variant="ghost" size="lg" onClick={onClose}>Close</Button>
          </div>
        </div>
      </DialogContent>

      {/* Paste dialog */}
      {showPasteDialog && (
        <Dialog open={true} onOpenChange={setShowPasteDialog}>
          <DialogContent className="max-w-md">
            <DialogHeader><DialogTitle>Paste Question</DialogTitle></DialogHeader>
            <div className="space-y-3">
              <Label>Enter paste code</Label>
              <Input value={pasteCode} onChange={(e) => setPasteCode(e.target.value)} placeholder="DK-TEXT-1-XXXX" />
              <p className="text-xs text-muted-foreground">Paste the code you got from "Copy Question" to duplicate that question here.</p>
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setShowPasteDialog(false)}>Cancel</Button>
              <Button onClick={doPaste}>Paste</Button>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </Dialog>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// QUESTION EDITOR — individual question form
// ═══════════════════════════════════════════════════════════════════════════

function QuestionEditor({ question, onChange, blockLabel, isAudioBlock }: {
  question: QuestionData;
  onChange: (q: QuestionData) => void;
  blockLabel: string;
  isAudioBlock: boolean;
}) {
  async function uploadFile(file: File, folder: string): Promise<string> {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("folder", folder);
    const res = await fetch("/api/admin/file-upload", { method: "POST", body: fd });
    if (!res.ok) { const d = await res.json(); throw new Error(d.error || "Upload failed"); }
    const d = await res.json();
    return d.url;
  }

  function UploadButton({ onUpload, accept }: { onUpload: (url: string) => void; accept: string }) {
    return (
      <label className="cursor-pointer">
        <input type="file" accept={accept} className="hidden" onChange={async (e) => {
          const f = e.target.files?.[0]; if (!f) return;
          try { const url = await uploadFile(f, "questions"); onUpload(url); toast.success("File uploaded"); }
          catch (err: any) { toast.error(err.message); }
        }} />
        <span className="inline-flex items-center h-8 px-2 rounded-md bg-primary text-primary-foreground text-xs cursor-pointer hover:bg-primary/90">📁 Upload</span>
      </label>
    );
  }

  return (
    <div className="space-y-4">
      {/* Question number badge */}
      <div className="flex items-center gap-2">
        <div className="w-10 h-10 rounded-full bg-primary text-primary-foreground flex items-center justify-center font-bold">
          {question.blockNumber}
        </div>
        <div>
          <p className="font-semibold">{blockLabel}</p>
          <p className="text-xs text-muted-foreground">{isAudioBlock ? "Audio question" : "Text question"}</p>
        </div>
      </div>

      {/* Question Description type */}
      <div className="space-y-2">
        <Label className="text-sm font-semibold">Question Description Type</Label>
        <div className="flex gap-2 flex-wrap">
          {(["none", "text", "image", "audio"] as const).map((t) => (
            <Button
              key={t}
              variant={question.descType === t ? "default" : "outline"}
              size="sm"
              onClick={() => onChange({ ...question, descType: t })}
            >
              {t === "none" ? "None" : t === "text" ? "Text" : t === "image" ? "Image" : "Audio"}
            </Button>
          ))}
        </div>
        {/* Description content based on type */}
        {question.descType === "text" && (
          <Textarea rows={2} value={question.descText} onChange={(e) => onChange({ ...question, descText: e.target.value })} placeholder="Description text…" />
        )}
        {question.descType === "image" && (
          <div className="flex gap-2">
            <Input value={question.descImageUrl} onChange={(e) => onChange({ ...question, descImageUrl: e.target.value })} placeholder="Image URL…" className="flex-1" />
            <UploadButton accept="image/*" onUpload={(url) => onChange({ ...question, descImageUrl: url })} />
          </div>
        )}
        {question.descType === "image" && question.descImageUrl && (
          <img src={question.descImageUrl} alt="Desc" className="max-h-32 rounded border" />
        )}
        {question.descType === "audio" && (
          <div className="flex gap-2">
            <Input value={question.descAudioUrl} onChange={(e) => onChange({ ...question, descAudioUrl: e.target.value })} placeholder="Audio URL…" className="flex-1" />
            <UploadButton accept="audio/*" onUpload={(url) => onChange({ ...question, descAudioUrl: url })} />
          </div>
        )}
        {question.descType === "audio" && question.descAudioUrl && (
          <audio controls src={question.descAudioUrl} className="w-full h-8" />
        )}
      </div>

      {/* Question text */}
      <div className="space-y-2">
        <Label className="text-sm font-semibold">Question *</Label>
        <Textarea rows={2} value={question.stem} onChange={(e) => onChange({ ...question, stem: e.target.value })} placeholder="What is the question?" />
      </div>

      {/* Question Media type */}
      <div className="space-y-2">
        <Label className="text-sm font-semibold">Question Media (shows in exam)</Label>
        <div className="flex gap-2 flex-wrap">
          {(["none", "text", "image", "audio"] as const).map((t) => (
            <Button
              key={t}
              variant={question.mediaType === t ? "default" : "outline"}
              size="sm"
              onClick={() => onChange({ ...question, mediaType: t })}
              disabled={isAudioBlock && t === "none"} // audio block must have audio media
            >
              {t === "none" ? "None" : t === "text" ? "Text" : t === "image" ? "Image" : "Audio"}
            </Button>
          ))}
        </div>
        {isAudioBlock && (
          <p className="text-xs text-amber-600">Audio block: media type is set to Audio automatically</p>
        )}
        {question.mediaType === "text" && (
          <Textarea rows={2} value={question.mediaText} onChange={(e) => onChange({ ...question, mediaText: e.target.value })} placeholder="Media text…" />
        )}
        {question.mediaType === "image" && (
          <>
            <div className="flex gap-2">
              <Input value={question.mediaImageUrl} onChange={(e) => onChange({ ...question, mediaImageUrl: e.target.value })} placeholder="Image URL…" className="flex-1" />
              <UploadButton accept="image/*" onUpload={(url) => onChange({ ...question, mediaImageUrl: url })} />
            </div>
            {question.mediaImageUrl && <img src={question.mediaImageUrl} alt="Media" className="max-h-40 rounded border" />}
          </>
        )}
        {question.mediaType === "audio" && (
          <>
            <div className="flex gap-2">
              <Input value={question.mediaAudioUrl} onChange={(e) => onChange({ ...question, mediaAudioUrl: e.target.value })} placeholder="Audio URL…" className="flex-1" />
              <UploadButton accept="audio/*" onUpload={(url) => onChange({ ...question, mediaAudioUrl: url })} />
            </div>
            {question.mediaAudioUrl && <audio controls src={question.mediaAudioUrl} className="w-full h-8" />}
          </>
        )}
      </div>

      {/* Answer type */}
      <div className="space-y-2">
        <Label className="text-sm font-semibold">Answer Type</Label>
        <div className="flex gap-2 flex-wrap">
          {(["text", "image", "audio", "choose"] as const).map((t) => (
            <Button
              key={t}
              variant={question.answerType === t ? "default" : "outline"}
              size="sm"
              onClick={() => onChange({ ...question, answerType: t })}
            >
              {t === "text" ? "Text" : t === "image" ? "Image" : t === "audio" ? "Audio" : "Choose Correct"}
            </Button>
          ))}
        </div>
      </div>

      {/* Options based on answer type */}
      {(question.answerType === "text" || question.answerType === "choose") && (
        <div className="space-y-2 p-3 border rounded-lg bg-slate-50">
          <Label className="text-sm font-semibold">Options (4)</Label>
          <div className="grid grid-cols-2 gap-2">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="flex items-center gap-2">
                <button
                  onClick={() => onChange({ ...question, correctOption: i })}
                  className={`w-6 h-6 rounded-full border-2 flex items-center justify-center text-xs font-bold ${
                    question.correctOption === i ? "bg-green-500 text-white border-green-500" : "border-slate-300"
                  }`}
                >
                  {question.correctOption === i ? "✓" : String.fromCharCode(65 + i)}
                </button>
                <Input
                  value={question.options[i] || ""}
                  onChange={(e) => {
                    const opts = [...question.options];
                    opts[i] = e.target.value;
                    onChange({ ...question, options: opts });
                  }}
                  placeholder={`Option ${String.fromCharCode(65 + i)}`}
                  className="flex-1"
                />
              </div>
            ))}
          </div>
          {question.answerType === "choose" && (
            <p className="text-xs text-muted-foreground">Underline style: correct option has ✓ green circle</p>
          )}
        </div>
      )}

      {question.answerType === "image" && (
        <div className="space-y-2 p-3 border rounded-lg bg-slate-50">
          <Label className="text-sm font-semibold">Image Options (4)</Label>
          <div className="grid grid-cols-2 gap-2">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="space-y-1">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => onChange({ ...question, correctOption: i })}
                    className={`w-6 h-6 rounded-full border-2 flex items-center justify-center text-xs font-bold ${
                      question.correctOption === i ? "bg-green-500 text-white border-green-500" : "border-slate-300"
                    }`}
                  >
                    {question.correctOption === i ? "✓" : String.fromCharCode(65 + i)}
                  </button>
                  <span className="text-xs">Option {String.fromCharCode(65 + i)}</span>
                  <UploadButton accept="image/*" onUpload={(url) => {
                    const imgs = [...question.optionImages];
                    imgs[i] = url;
                    onChange({ ...question, optionImages: imgs });
                  }} />
                </div>
                <Input
                  value={question.optionImages[i] || ""}
                  onChange={(e) => {
                    const imgs = [...question.optionImages];
                    imgs[i] = e.target.value;
                    onChange({ ...question, optionImages: imgs });
                  }}
                  placeholder="Image URL…"
                />
                {question.optionImages[i] && <img src={question.optionImages[i]} alt="" className="h-20 rounded border" />}
              </div>
            ))}
          </div>
        </div>
      )}

      {question.answerType === "audio" && (
        <div className="space-y-2 p-3 border rounded-lg bg-slate-50">
          <Label className="text-sm font-semibold">Audio Options (4) — click to play</Label>
          <div className="grid grid-cols-2 gap-2">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="space-y-1">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => onChange({ ...question, correctOption: i })}
                    className={`w-6 h-6 rounded-full border-2 flex items-center justify-center text-xs font-bold ${
                      question.correctOption === i ? "bg-green-500 text-white border-green-500" : "border-slate-300"
                    }`}
                  >
                    {question.correctOption === i ? "✓" : String.fromCharCode(65 + i)}
                  </button>
                  <span className="text-xs">Audio {String.fromCharCode(65 + i)}</span>
                  <UploadButton accept="audio/*" onUpload={(url) => {
                    const auds = [...question.optionAudios];
                    auds[i] = url;
                    onChange({ ...question, optionAudios: auds });
                  }} />
                </div>
                <Input
                  value={question.optionAudios[i] || ""}
                  onChange={(e) => {
                    const auds = [...question.optionAudios];
                    auds[i] = e.target.value;
                    onChange({ ...question, optionAudios: auds });
                  }}
                  placeholder="Audio URL…"
                />
                {question.optionAudios[i] && <audio controls src={question.optionAudios[i]} className="w-full h-8" />}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Answer description */}
      <div className="space-y-2">
        <Label className="text-sm font-semibold">Answer Description (optional)</Label>
        <Textarea rows={2} value={question.explanation} onChange={(e) => onChange({ ...question, explanation: e.target.value })} placeholder="Explanation shown after answering…" />
      </div>
    </div>
  );
}
