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
  title: string;
  isFree: boolean;
  audioLoop: number;
  audioLoopDelay: number;
  setNumber?: number;
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
  optionBlanks: string[];
  correctOption: number;
  explanation: string;
}

function emptyQuestion(blockType: "text" | "audio", blockNumber: number): QuestionData {
  return {
    blockType,
    blockNumber,
    stem: "",
    title: "",
    isFree: false,
    audioLoop: 2,
    audioLoopDelay: 0,
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
    optionBlanks: ["", "", "", ""],
    correctOption: 0,
    explanation: "",
  };
}

// Module-level clipboard — simple, reliable, no state/ref issues
let _clipboardData: { type: "single" | "all"; data: any } | null = null;

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

// ─── Featured Image Upload (drag & drop + instant preview) ──────────────────
function FeaturedImageUpload({ url, onUpload, onClear, uploadFn }: {
  url: string;
  onUpload: (url: string) => void;
  onClear: () => void;
  uploadFn: (file: File) => Promise<string>;
}) {
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [localPreview, setLocalPreview] = useState("");

  async function compressImage(file: File): Promise<File> {
    if (file.size < 200_000) return file;
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          const canvas = document.createElement("canvas");
          const maxDim = 800;
          let { width, height } = img;
          if (width > maxDim || height > maxDim) {
            if (width > height) { height = (height / width) * maxDim; width = maxDim; }
            else { width = (width / height) * maxDim; height = maxDim; }
          }
          canvas.width = width; canvas.height = height;
          const ctx = canvas.getContext("2d")!;
          ctx.drawImage(img, 0, 0, width, height);
          canvas.toBlob((blob) => {
            if (blob) resolve(new File([blob], "featured.jpg", { type: "image/jpeg" }));
            else resolve(file);
          }, "image/jpeg", 0.8);
        };
        img.src = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    });
  }

  async function handleFile(file: File) {
    const localUrl = URL.createObjectURL(file);
    setLocalPreview(localUrl);
    setUploading(true);
    try {
      const compressed = await compressImage(file);
      const u = await uploadFn(compressed);
      onUpload(u);
      toast.success("Image uploaded");
    } catch (err: any) {
      toast.error(err.message || "Upload failed");
      setLocalPreview("");
    } finally {
      setUploading(false);
      setTimeout(() => URL.revokeObjectURL(localUrl), 1000);
    }
  }

  const displayUrl = uploading && localPreview ? localPreview : url;

  return (
    <div className="mt-1">
      {displayUrl ? (
        <div className="relative inline-block">
          <img src={displayUrl} alt="Featured" className="w-24 h-24 rounded-lg object-cover border-2 border-slate-200" />
          {uploading && (
            <div className="absolute inset-0 bg-black/40 rounded-lg flex items-center justify-center">
              <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
            </div>
          )}
          {!uploading && (
            <button
              onClick={() => { onClear(); setLocalPreview(""); }}
              className="absolute -top-2 -right-2 w-7 h-7 bg-red-500 text-white rounded-full flex items-center justify-center text-sm font-bold hover:bg-red-600 shadow"
            >✕</button>
          )}
        </div>
      ) : (
        <div
          onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => {
            e.preventDefault(); setDragOver(false);
            const f = e.dataTransfer.files?.[0]; if (f) handleFile(f);
          }}
          className={`flex flex-col items-center justify-center w-24 h-24 border-2 border-dashed rounded-lg cursor-pointer transition-all ${
            dragOver ? "border-primary bg-primary/10 scale-105" : "border-slate-300 hover:border-primary hover:bg-slate-50"
          }`}
        >
          <input type="file" accept="image/*" className="hidden" onChange={(e) => {
            const f = e.target.files?.[0]; if (!f) return;
            handleFile(f); e.target.value = "";
          }} />
          <ImageIcon className="w-7 h-7 text-slate-400 mb-1" />
          <span className="text-[10px] text-slate-500 text-center px-1">Drag & drop<br/>or click</span>
        </div>
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
      <DialogContent className="sm:max-w-4xl max-h-[90vh] overflow-y-auto">
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

          {/* Featured Image — drag & drop with instant preview */}
          <div>
            <Label className="text-sm font-semibold">Featured Image (optional)</Label>
            <FeaturedImageUpload
              url={form.featuredImage}
              onUpload={(url) => setForm(f => ({ ...f, featuredImage: url }))}
              onClear={() => setForm(f => ({ ...f, featuredImage: "" }))}
              uploadFn={(file) => uploadFile(file, "exam-featured")}
            />
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
  const [showAppPasteDialog, setShowAppPasteDialog] = useState(false);
  const [appPasteJson, setAppPasteJson] = useState("");
  const appPasteTaRef = useRef<HTMLTextAreaElement>(null);
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
        title: q.title || "",
        isFree: q.isFree ?? false,
        audioLoop: q.audioLoop ?? 2,
        audioLoopDelay: q.audioLoopDelay ?? 0,
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
        optionBlanks: q.optionBlanks || [],
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

  // ─── DEEP COPY: ensures arrays (options, optionImages, optionAudios, optionBlanks)
  // are copied by VALUE not reference, so editing pasted question doesn't affect original ──
  function deepCopyQuestion(q: QuestionData): QuestionData {
    return {
      ...q,
      options: [...(q.options || ["", "", "", ""])],
      optionImages: [...(q.optionImages || ["", "", "", ""])],
      optionAudios: [...(q.optionAudios || ["", "", "", ""])],
      optionBlanks: [...(q.optionBlanks || ["", "", "", ""])],
    };
  }

  // ─── COPY: saves current question (ALL fields) to module-level variable ──
  function copyQuestion() {
    const q = currentQuestion;
    _clipboardData = { type: "single", data: deepCopyQuestion(q) };
    toast.success("Question copied (all fields)! Click Paste in any block to import.");
  }

  // ─── PASTE: reads from module-level variable, fills current block ────────
  function pasteQuestion() {
    if (!_clipboardData) {
      toast.error("Nothing copied \u2014 click Copy first");
      return;
    }
    if (_clipboardData.type === "single") {
      const q = _clipboardData.data as QuestionData;
      const pasted: QuestionData = {
        ...deepCopyQuestion(q),
        blockType: activeBlock,
        blockNumber: activeNumber,
      };
      delete (pasted as any).id;
      delete (pasted as any).testItemId;
      updateQuestion(pasted);
      toast.success("Question pasted (all fields)! Click Save to persist.");
    } else if (_clipboardData.type === "all") {
      const allQs = _clipboardData.data as QuestionData[];
      const next = { ...questions };
      let count = 0;
      for (const q of allQs) {
        const k = key(q.blockType, q.blockNumber);
        const copy = deepCopyQuestion(q);
        delete (copy as any).id;
        delete (copy as any).testItemId;
        next[k] = copy;
        count++;
      }
      setQuestions(next);
      toast.success(`Pasted ${count} questions (all fields)! Click Save on each to persist.`);
    }
  }

  // ─── COPY ALL: saves all filled questions (ALL fields) to module-level variable ──
  function copyAll() {
    const filled = Object.values(questions).filter(q => {
      return q.stem?.trim() || q.title?.trim() || q.mediaImageUrl?.trim() || q.mediaAudioUrl?.trim() || q.descImageUrl?.trim() || q.descAudioUrl?.trim() || q.options?.some((o: string) => o?.trim()) || q.optionAudios?.some((a: string) => a?.trim()) || q.optionImages?.some((img: string) => img?.trim());
    });
    if (filled.length === 0) { toast.error("No questions to copy \u2014 add some first"); return; }
    _clipboardData = { type: "all", data: filled.map(q => deepCopyQuestion(q)) };
    toast.success(`Copied ${filled.length} questions (all fields including audio/images)! Open another test and click Paste.`);
  }

  // ─── PASTE FROM APP: reads JSON from textarea ref ─────────────────────────
  function pasteFromApp() {
    const raw = (appPasteTaRef.current?.value || "").trim();
    if (!raw) { toast.error("Paste the JSON from your other app first"); return; }
    try {
      const lines = raw.split("\n").filter((l) => l.trim().startsWith("{"));
      if (lines.length > 1) {
        const parsed: any[] = [];
        for (const line of lines) { try { parsed.push(JSON.parse(line)); } catch {} }
        if (parsed.length === 0) { toast.error("No valid JSON found"); return; }
        importMultipleFromApp(parsed);
        return;
      }
      if (lines.length === 1) {
        applyAppJsonToQuestion(JSON.parse(lines[0]));
        return;
      }
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        if (parsed.length === 0) { toast.error("Empty array"); return; }
        if (parsed.length === 1) { applyAppJsonToQuestion(parsed[0]); return; }
        importMultipleFromApp(parsed);
        return;
      }
      applyAppJsonToQuestion(parsed);
    } catch (e: any) {
      toast.error("Invalid JSON: " + (e.message || "parse error"));
    }
  }

  function applyAppJsonToQuestion(data: any) {
    const q = currentQuestion;
    const qNum = parseInt(data.question_number || "0");
    const detectedBlockType: "text" | "audio" = qNum >= 21 ? "audio" : "text";
    const detectedBlockNumber = qNum >= 21 ? qNum - 20 : qNum;

    // Collect raw options from app JSON
    const rawOptions = [data.option_1 || "", data.option_2 || "", data.option_3 || "", data.option_4 || ""];

    // Use answer_media_type from JSON (most reliable) — fallback to URL extension detection
    const isImageUrl = (s: string) => s && s.startsWith("http") && /\.(jpeg|jpg|png|webp|gif)/i.test(s);
    const isAudioUrl = (s: string) => s && s.startsWith("http") && /\.(mp3|wav|ogg|m4a|aac)/i.test(s);
    const allImages = rawOptions.every(o => !o || isImageUrl(o));
    const allAudios = rawOptions.every(o => !o || isAudioUrl(o));

    let detectedAnswerType = "text" as "text" | "image" | "audio" | "choose";
    // Priority 1: explicit answer_media_type from JSON
    if (data.answer_media_type === "image") detectedAnswerType = "image";
    else if (data.answer_media_type === "audio") detectedAnswerType = "audio";
    // Priority 2: detect from URL extensions
    else if (allImages && rawOptions.some(o => o)) detectedAnswerType = "image";
    else if (allAudios && rawOptions.some(o => o)) detectedAnswerType = "audio";

    // Route options to the correct array based on type
    const textOptions = detectedAnswerType === "text" ? rawOptions : ["", "", "", ""];
    const imageOptions = detectedAnswerType === "image" ? rawOptions : ["", "", "", ""];
    const audioOptions = detectedAnswerType === "audio" ? rawOptions : ["", "", "", ""];

    // Description: question_description field holds the URL when type is image/audio
    const descType = (data.question_description_type || "none") as "none" | "text" | "image" | "audio";
    const descUrl = data.question_description || "";  // This is the URL for image/audio, or text for text
    const descText = descType === "text" ? descUrl : "";
    const descImageUrl = descType === "image" ? descUrl : "";
    const descAudioUrl = descType === "audio" ? descUrl : "";

    // Question media: question_media field holds the URL
    const mediaType = (data.question_media_type || "none") as "none" | "text" | "image" | "audio";
    const mediaUrl = data.question_media || "";
    const mediaText = data.question_text || (mediaType === "text" ? mediaUrl : "");
    const mediaImageUrl = mediaType === "image" ? mediaUrl : "";
    const mediaAudioUrl = mediaType === "audio" ? mediaUrl : "";

    const updated: QuestionData = {
      ...q,
      blockType: detectedBlockType,
      blockNumber: detectedBlockNumber || q.blockNumber,
      stem: data.question || data.question_text || "",
      title: data.question_number ? `Question ${data.question_number}` : q.title,
      mediaType,
      mediaText,
      mediaImageUrl,
      mediaAudioUrl,
      descType,
      descText,
      descImageUrl,
      descAudioUrl,
      options: textOptions,
      optionImages: imageOptions,
      optionAudios: audioOptions,
      optionBlanks: ["", "", "", ""],
      correctOption: data.correct_answer ? (() => {
        const m = data.correct_answer.match(/option\s*(\d+)/i);
        return m ? parseInt(m[1]) - 1 : 0;
      })() : (q.correctOption || 0),
      answerType: detectedAnswerType,
      explanation: data.answer_description || "",
    };
    updateQuestion(updated);
    toast.success("Question imported from app JSON!");
    setShowAppPasteDialog(false);
    setAppPasteJson("");
  }

  function importMultipleFromApp(items: any[]) {
    const next = { ...questions };
    let imported = 0;
    for (const data of items) {
      const qNum = parseInt(data.question_number || "0");
      if (!qNum) continue;
      const blockType: "text" | "audio" = qNum >= 21 ? "audio" : "text";
      const blockNumber = qNum >= 21 ? qNum - 20 : qNum;
      const k = key(blockType, blockNumber);
      const existing = next[k] || emptyQuestion(blockType, blockNumber);

      // Detect option types — use answer_media_type first, fallback to URL extension
      const rawOptions = [data.option_1 || "", data.option_2 || "", data.option_3 || "", data.option_4 || ""];
      const isImageUrl = (s: string) => s && s.startsWith("http") && /\.(jpeg|jpg|png|webp|gif)/i.test(s);
      const isAudioUrl = (s: string) => s && s.startsWith("http") && /\.(mp3|wav|ogg|m4a|aac)/i.test(s);
      const allImages = rawOptions.every(o => !o || isImageUrl(o));
      const allAudios = rawOptions.every(o => !o || isAudioUrl(o));

      let answerType = "text" as "text" | "image" | "audio" | "choose";
      if (data.answer_media_type === "image") answerType = "image";
      else if (data.answer_media_type === "audio") answerType = "audio";
      else if (allImages && rawOptions.some(o => o)) answerType = "image";
      else if (allAudios && rawOptions.some(o => o)) answerType = "audio";

      // Description: question_description holds URL for image/audio
      const descType = (data.question_description_type || "none") as "none" | "text" | "image" | "audio";
      const descUrl = data.question_description || "";
      const descText = descType === "text" ? descUrl : "";
      const descImageUrl = descType === "image" ? descUrl : "";
      const descAudioUrl = descType === "audio" ? descUrl : "";

      // Question media
      const mediaType = (data.question_media_type || "none") as "none" | "text" | "image" | "audio";
      const mediaUrl = data.question_media || "";
      const mediaText = data.question_text || (mediaType === "text" ? mediaUrl : "");
      const mediaImageUrl = mediaType === "image" ? mediaUrl : "";
      const mediaAudioUrl = mediaType === "audio" ? mediaUrl : "";

      next[k] = {
        ...existing,
        blockType, blockNumber,
        stem: data.question || data.question_text || "",
        title: `Question ${qNum}`,
        mediaType,
        mediaText,
        mediaImageUrl,
        mediaAudioUrl,
        descType,
        descText,
        descImageUrl,
        descAudioUrl,
        options: answerType === "text" ? rawOptions : ["", "", "", ""],
        optionImages: answerType === "image" ? rawOptions : ["", "", "", ""],
        optionAudios: answerType === "audio" ? rawOptions : ["", "", "", ""],
        optionBlanks: ["", "", "", ""],
        correctOption: data.correct_answer ? (() => {
          const m = data.correct_answer.match(/option\s*(\d+)/i);
          return m ? parseInt(m[1]) - 1 : 0;
        })() : 0,
        answerType,
        explanation: data.answer_description || "",
      };
      imported++;
    }
    setQuestions(next);
    toast.success(`Imported ${imported} questions! Click Save on each to persist.`);
    setShowAppPasteDialog(false);
    setAppPasteJson("");
  }

  async function pushToApp() {
    // Count questions that have been filled in
    const filledQuestions = Object.values(questions).filter(q => q.stem.trim());
    if (filledQuestions.length === 0) {
      toast.error("Cannot push: add at least one question first");
      return;
    }

    // Auto-save any unsaved questions before pushing
    setPushing(true);
    try {
      // Save all filled questions that haven't been saved yet
      for (const q of filledQuestions) {
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
        await fetch(`/api/admin/tests/${test.id}/questions`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });
      }

      // Now publish
      const res = await fetch(`/api/admin/tests/${test.id}/publish`, { method: "POST" });
      const d = await res.json();
      if (!res.ok) {
        toast.error(d.error || `Push failed (HTTP ${res.status})`);
        return;
      }
      setIsPublished(true);
      toast.success(d.message || "Pushed to app — students can now see this exam");
    } catch (e: any) {
      toast.error("Push failed: " + (e.message || "network error"));
    } finally {
      setPushing(false);
    }
  }

  // Block numbers: Reading shows 1-20, Listening shows 21-40
  // This matches what the student sees in the app
  const blockNumbers = activeBlock === "text"
    ? Array.from({ length: textCount }, (_, i) => i + 1)        // 1-20
    : Array.from({ length: audioCount }, (_, i) => i + 21);     // 21-40

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
      <DialogContent className="sm:max-w-5xl max-h-[95vh] overflow-hidden flex flex-col">
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
        {/* Reading: 1-20, Listening: 21-40 (display) → stored as 1-20 internally */}
        {!isSimple && (
          <div className="grid grid-cols-10 gap-1 max-h-24 overflow-y-auto p-1 bg-slate-50 rounded">
            {blockNumbers.map((num) => {
              // Convert display number to internal blockNumber
              // Reading: 1-20 → 1-20, Listening: 21-40 → 1-20
              const internalBlockNumber = activeBlock === "audio" ? num - 20 : num;
              const k = key(activeBlock, internalBlockNumber);
              const isFilled = questions[k] && questions[k].stem.trim();
              const isActive = internalBlockNumber === activeNumber;
              return (
                <button
                  key={num}
                  onClick={() => setActiveNumber(internalBlockNumber)}
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
              blockLabel={`Question ${activeBlock === "audio" ? activeNumber + 20 : activeNumber}`}
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
            <Button onClick={() => setShowAppPasteDialog(true)} variant="outline" size="lg" className="text-blue-600 hover:text-blue-700 border-blue-300 hover:border-blue-400">
              <ClipboardPaste className="w-4 h-4 mr-1" /> Paste from App
            </Button>
            <Button onClick={copyAll} variant="outline" size="lg" className="text-purple-600 hover:text-purple-700">
              <Copy className="w-4 h-4 mr-1" /> Copy All
            </Button>
            {/* Set audio play count for ALL questions at once — default 2 */}
            <Button
              onClick={async () => {
                const next = { ...questions };
                for (const k of Object.keys(next)) {
                  next[k] = { ...next[k], audioLoop: 2 };
                }
                setQuestions(next);
                // Also save to server
                try {
                  for (const q of Object.values(next)) {
                    if (q.stem?.trim() || q.mediaImageUrl?.trim() || q.mediaAudioUrl?.trim()) {
                      await fetch(`/api/admin/tests/${test.id}/questions`, {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                          blockType: q.blockType,
                          blockNumber: q.blockNumber,
                          stem: q.stem,
                          title: q.title || "",
                          isFree: q.isFree || false,
                          audioLoop: 2,
                          audioLoopDelay: q.audioLoopDelay || 0,
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
                          optionBlanks: q.optionBlanks || [],
                          correctOption: q.correctOption,
                          explanation: q.explanation || "",
                        }),
                      });
                    }
                  }
                  toast.success("All audio set to play 2 times — saved!");
                } catch {
                  toast.success("Set locally — click Save on each to persist");
                }
              }}
              variant="outline"
              size="lg"
              className="text-amber-600 hover:text-amber-700 border-amber-300 hover:border-amber-400"
              title="Set all audio (question + options) to play 2 times and save"
            >
              <Headphones className="w-4 h-4 mr-1" /> Audio ×2 All
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

      {/* Paste from App dialog */}
      {showAppPasteDialog && (
        <Dialog open={true} onOpenChange={setShowAppPasteDialog}>
          <DialogContent className="max-w-2xl">
            <DialogHeader><DialogTitle>Paste from App</DialogTitle></DialogHeader>
            <div className="space-y-3">
              <Label>Paste the JSON from your other DreamKorea app</Label>
              <Textarea
                ref={appPasteTaRef}
                rows={10}
                defaultValue={appPasteJson}
                onChange={(e) => setAppPasteJson(e.target.value)}
                placeholder={`{"question_number":"21","question":"들은 것을 고르십시오.","question_media":"https://api.dreamkoreaubttest.com/...mp3","question_media_type":"audio","option_1":"불이","option_2":"부리","option_3":"물리","option_4":"무리","correct_answer":"option 4","answer_media_type":"text"}`}
                className="font-mono text-xs"
              />
              <p className="text-xs text-muted-foreground">
                Paste the JSON code from your other app. The system will decode it and fill all fields
                automatically. If multiple JSON objects are pasted (one per line), ALL are imported at once.
                Question numbers 1-20 go to Reading, 21-40 go to Listening.
              </p>
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setShowAppPasteDialog(false)}>Cancel</Button>
              <Button onClick={pasteFromApp} className="bg-blue-600 hover:bg-blue-700">
                <ClipboardPaste className="w-4 h-4 mr-1" /> Import
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </Dialog>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════════════════════════
// QUESTION EDITOR — clean, PC-optimized, two-column layout
// ═══════════════════════════════════════════════════════════════════════════

function QuestionEditor({ question, onChange, blockLabel, isAudioBlock }: {
  question: QuestionData;
  onChange: (q: QuestionData) => void;
  blockLabel: string;
  isAudioBlock: boolean;
}) {
  async function uploadFile(file: File): Promise<string> {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("folder", "questions");
    const res = await fetch("/api/admin/file-upload", { method: "POST", body: fd });
    if (!res.ok) { const d = await res.json(); throw new Error(d.error || "Upload failed"); }
    const d = await res.json();
    return d.url;
  }

  // ─── Reusable upload field with drag-drop + instant preview ───────────────
  function MediaUpload({
    label, accept, url, onUpload, onClear, type,
  }: {
    label: string;
    accept: string;
    url: string;
    onUpload: (url: string) => void;
    onClear: () => void;
    type: "image" | "audio";
  }) {
    const [uploading, setUploading] = useState(false);
    const [dragOver, setDragOver] = useState(false);
    const [localPreview, setLocalPreview] = useState<string>(""); // instant blob URL

    // Compress images before upload (max 800px, 80% quality) — much faster
    async function compressImage(file: File): Promise<File> {
      if (file.size < 200_000) return file; // skip small images
      return new Promise((resolve) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          const img = new Image();
          img.onload = () => {
            const canvas = document.createElement("canvas");
            const maxDim = 800;
            let { width, height } = img;
            if (width > maxDim || height > maxDim) {
              if (width > height) { height = (height / width) * maxDim; width = maxDim; }
              else { width = (width / height) * maxDim; height = maxDim; }
            }
            canvas.width = width; canvas.height = height;
            const ctx = canvas.getContext("2d")!;
            ctx.drawImage(img, 0, 0, width, height);
            canvas.toBlob((blob) => {
              if (blob) resolve(new File([blob], file.name.replace(/\.\w+$/, ".jpg"), { type: "image/jpeg" }));
              else resolve(file);
            }, "image/jpeg", 0.8);
          };
          img.src = e.target?.result as string;
        };
        reader.readAsDataURL(file);
      });
    }

    async function handleFile(file: File) {
      // Instant local preview (no waiting for upload)
      const localUrl = URL.createObjectURL(file);
      setLocalPreview(localUrl);

      setUploading(true);
      try {
        // Compress images for faster upload
        const fileToUpload = type === "image" ? await compressImage(file) : file;
        const u = await uploadFile(fileToUpload);
        onUpload(u);
        toast.success("Uploaded");
      } catch (err: any) {
        toast.error(err.message || "Upload failed");
        setLocalPreview(""); // clear preview on error
      } finally {
        setUploading(false);
        // Clean up the blob URL after a delay (let React render the real URL)
        setTimeout(() => URL.revokeObjectURL(localUrl), 1000);
      }
    }

    // The display URL: local preview during upload, real URL after
    const displayUrl = uploading && localPreview ? localPreview : url;

    return (
      <div className="space-y-2">
        {label && <Label className="text-sm font-medium">{label}</Label>}

        {displayUrl ? (
          /* ─── Preview shown (either local or uploaded) ─── */
          <div className="flex items-start gap-3">
            {type === "image" ? (
              <div className="relative">
                <img src={displayUrl} alt={label} className="w-28 h-28 rounded-lg object-cover border-2 border-slate-200" />
                {uploading && (
                  <div className="absolute inset-0 bg-black/40 rounded-lg flex items-center justify-center">
                    <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  </div>
                )}
                {!uploading && (
                  <button
                    onClick={() => { onClear(); setLocalPreview(""); }}
                    className="absolute -top-2 -right-2 w-7 h-7 bg-red-500 text-white rounded-full flex items-center justify-center text-sm font-bold hover:bg-red-600 shadow"
                    title="Remove"
                  >✕</button>
                )}
              </div>
            ) : (
              <div className="relative flex-1">
                <audio controls src={displayUrl} className="w-full" />
                {uploading && (
                  <div className="absolute inset-0 bg-black/20 rounded flex items-center justify-center">
                    <div className="bg-white px-3 py-1 rounded-full text-xs font-medium flex items-center gap-2">
                      <div className="w-3 h-3 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                      Uploading…
                    </div>
                  </div>
                )}
                {!uploading && (
                  <button
                    onClick={() => { onClear(); setLocalPreview(""); }}
                    className="absolute -top-2 -right-2 w-7 h-7 bg-red-500 text-white rounded-full flex items-center justify-center text-sm font-bold hover:bg-red-600 shadow"
                    title="Remove"
                  >✕</button>
                )}
              </div>
            )}
          </div>
        ) : (
          /* ─── Upload zone (drag-drop + click) ─── */
          <div
            onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
            onDragLeave={() => setDragOver(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDragOver(false);
              const f = e.dataTransfer.files?.[0];
              if (f) handleFile(f);
            }}
            className={`flex flex-col items-center justify-center w-full h-28 border-2 border-dashed rounded-lg cursor-pointer transition-all ${
              dragOver ? "border-primary bg-primary/10 scale-[1.02]" : "border-slate-300 hover:border-primary hover:bg-slate-50"
            }`}
          >
            <input type="file" accept={accept} className="hidden" onChange={(e) => {
              const f = e.target.files?.[0]; if (!f) return;
              handleFile(f);
              e.target.value = "";
            }} />
            <Upload className="w-7 h-7 text-slate-400 mb-1" />
            <span className="text-xs text-slate-500 font-medium">
              {dragOver ? "Drop here!" : `Drag & drop or click to upload ${type}`}
            </span>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-5 p-1">
      {/* ─── Question number + label ──────────────────────────────────────── */}
      <div className="flex items-center gap-3 pb-3 border-b">
        <div className="w-12 h-12 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-lg font-bold">
          {question.blockNumber}
        </div>
        <div>
          <p className="font-bold text-base">{blockLabel}</p>
          <p className="text-xs text-muted-foreground">{isAudioBlock ? "Audio question" : "Text question"}</p>
        </div>
      </div>

      {/* ─── Two-column layout for PC ─────────────────────────────────────── */}
      <div className="grid grid-cols-2 gap-6">

        {/* ─── LEFT COLUMN: Description + Question ─────────────────────────── */}
        <div className="space-y-5">

          {/* Description Type */}
          <div className="space-y-2">
            <Label className="text-sm font-semibold">Description Type</Label>
            <div className="flex gap-1">
              {(["none", "text", "image", "audio"] as const).map((t) => (
                <Button
                  key={t}
                  variant={question.descType === t ? "default" : "outline"}
                  size="sm"
                  className="flex-1"
                  onClick={() => onChange({ ...question, descType: t })}
                >
                  {t === "none" ? "None" : t.charAt(0).toUpperCase() + t.slice(1)}
                </Button>
              ))}
            </div>
          </div>

          {/* Description content */}
          {question.descType === "text" && (
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Description Text</Label>
              <Textarea rows={2} value={question.descText} onChange={(e) => onChange({ ...question, descText: e.target.value })} placeholder="Enter description…" />
            </div>
          )}
          {question.descType === "image" && (
            <MediaUpload
              label="Description Image"
              accept="image/*"
              url={question.descImageUrl}
              onUpload={(url) => onChange({ ...question, descImageUrl: url })}
              onClear={() => onChange({ ...question, descImageUrl: "" })}
              type="image"
            />
          )}
          {question.descType === "audio" && (
            <MediaUpload
              label="Description Audio"
              accept="audio/*"
              url={question.descAudioUrl}
              onUpload={(url) => onChange({ ...question, descAudioUrl: url })}
              onClear={() => onChange({ ...question, descAudioUrl: "" })}
              type="audio"
            />
          )}

          {/* Question text */}
          <div className="space-y-1">
            <Label className="text-sm font-semibold">Question <span className="text-muted-foreground font-normal text-xs">(optional — leave empty if using media below)</span></Label>
            <Textarea rows={3} value={question.stem} onChange={(e) => onChange({ ...question, stem: e.target.value })} placeholder="What is the question?" className="text-base" />
          </div>

          {/* Question Media Type */}
          <div className="space-y-2">
            <Label className="text-sm font-semibold">Question Media (shows in exam)</Label>
            <div className="flex gap-1">
              {(["none", "text", "image", "audio"] as const).map((t) => (
                <Button
                  key={t}
                  variant={question.mediaType === t ? "default" : "outline"}
                  size="sm"
                  className="flex-1"
                  onClick={() => onChange({ ...question, mediaType: t })}
                  disabled={isAudioBlock && t === "none"}
                >
                  {t === "none" ? "None" : t.charAt(0).toUpperCase() + t.slice(1)}
                </Button>
              ))}
            </div>
          </div>

          {/* Media content */}
          {question.mediaType === "text" && (
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Media Text</Label>
              <Textarea rows={2} value={question.mediaText} onChange={(e) => onChange({ ...question, mediaText: e.target.value })} placeholder="Media text…" />
            </div>
          )}
          {question.mediaType === "image" && (
            <MediaUpload
              label="Question Image"
              accept="image/*"
              url={question.mediaImageUrl}
              onUpload={(url) => onChange({ ...question, mediaImageUrl: url })}
              onClear={() => onChange({ ...question, mediaImageUrl: "" })}
              type="image"
            />
          )}
          {question.mediaType === "audio" && (
            <MediaUpload
              label="Question Audio"
              accept="audio/*"
              url={question.mediaAudioUrl}
              onUpload={(url) => onChange({ ...question, mediaAudioUrl: url })}
              onClear={() => onChange({ ...question, mediaAudioUrl: "" })}
              type="audio"
            />
          )}
        </div>

        {/* ─── RIGHT COLUMN: Answer type + options ────────────────────────── */}
        <div className="space-y-5">

          {/* Answer Type */}
          <div className="space-y-2">
            <Label className="text-sm font-semibold">Answer Type</Label>
            <div className="grid grid-cols-2 gap-1">
              {(["text", "image", "audio", "choose"] as const).map((t) => (
                <Button
                  key={t}
                  variant={question.answerType === t ? "default" : "outline"}
                  size="sm"
                  onClick={() => onChange({ ...question, answerType: t })}
                >
                  {t === "text" ? "Text Options" : t === "image" ? "Image Options" : t === "audio" ? "Audio Options" : "Choose Correct"}
                </Button>
              ))}
            </div>
          </div>

          {/* Options based on answer type */}
          {(question.answerType === "text" || question.answerType === "choose") && (
            <div className="space-y-3 p-4 border rounded-lg bg-slate-50">
              <Label className="text-sm font-semibold">4 Options — click circle to mark correct</Label>
              <div className="space-y-2">
                {[0, 1, 2, 3].map((i) => (
                  <div key={i} className="flex items-center gap-3">
                    <button
                      onClick={() => onChange({ ...question, correctOption: i })}
                      className={`w-8 h-8 rounded-full border-2 flex items-center justify-center text-sm font-bold flex-shrink-0 ${
                        question.correctOption === i ? "bg-green-500 text-white border-green-500" : "border-slate-300 text-slate-400"
                      }`}
                      title="Mark as correct"
                    >
                      {question.correctOption === i ? "✓" : String.fromCharCode(65 + i)}
                    </button>
                    <Input
                      value={(question.options || [])[i] || ""}
                      onChange={(e) => {
                        const opts = [...(question.options || ["", "", "", ""])];
                        opts[i] = e.target.value;
                        onChange({ ...question, options: opts });
                      }}
                      placeholder={`Option ${String.fromCharCode(65 + i)}`}
                      className="flex-1"
                    />
                    <Input
                      value={(question.optionBlanks || [])[i] || ""}
                      onChange={(e) => {
                        const blanks = [...(question.optionBlanks || ["", "", "", ""])];
                        blanks[i] = e.target.value;
                        onChange({ ...question, optionBlanks: blanks });
                      }}
                      placeholder="underline word"
                      className="w-32 text-xs"
                      title="Type a word from the option to underline it in the app"
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {question.answerType === "image" && (
            <div className="space-y-3 p-4 border rounded-lg bg-slate-50">
              <Label className="text-sm font-semibold">4 Image Options — click circle to mark correct</Label>
              <div className="grid grid-cols-2 gap-3">
                {[0, 1, 2, 3].map((i) => (
                  <div key={i} className="space-y-2 p-3 border rounded bg-white">
                    <div className="flex items-center gap-2 mb-1">
                      <button
                        onClick={() => onChange({ ...question, correctOption: i })}
                        className={`w-7 h-7 rounded-full border-2 flex items-center justify-center text-xs font-bold flex-shrink-0 ${
                          question.correctOption === i ? "bg-green-500 text-white border-green-500" : "border-slate-300 text-slate-400"
                        }`}
                      >
                        {question.correctOption === i ? "✓" : String.fromCharCode(65 + i)}
                      </button>
                      <span className="text-xs font-medium">Option {String.fromCharCode(65 + i)}</span>
                    </div>
                    <MediaUpload
                      label=""
                      accept="image/*"
                      url={(question.optionImages || [])[i] || ""}
                      onUpload={(url) => {
                        const imgs = [...(question.optionImages || ["", "", "", ""])]; imgs[i] = url;
                        onChange({ ...question, optionImages: imgs });
                      }}
                      onClear={() => {
                        const imgs = [...(question.optionImages || ["", "", "", ""])]; imgs[i] = "";
                        onChange({ ...question, optionImages: imgs });
                      }}
                      type="image"
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {question.answerType === "audio" && (
            <div className="space-y-3 p-4 border rounded-lg bg-slate-50">
              <Label className="text-sm font-semibold">4 Audio Options — click circle to mark correct</Label>
              <div className="space-y-3">
                {[0, 1, 2, 3].map((i) => (
                  <div key={i} className="p-3 border rounded bg-white">
                    <div className="flex items-center gap-2 mb-2">
                      <button
                        onClick={() => onChange({ ...question, correctOption: i })}
                        className={`w-7 h-7 rounded-full border-2 flex items-center justify-center text-xs font-bold flex-shrink-0 ${
                          question.correctOption === i ? "bg-green-500 text-white border-green-500" : "border-slate-300 text-slate-400"
                        }`}
                      >
                        {question.correctOption === i ? "✓" : String.fromCharCode(65 + i)}
                      </button>
                      <span className="text-xs font-medium">Audio {String.fromCharCode(65 + i)}</span>
                    </div>
                    <MediaUpload
                      label=""
                      accept="audio/*"
                      url={(question.optionAudios || [])[i] || ""}
                      onUpload={(url) => {
                        const auds = [...(question.optionAudios || ["", "", "", ""])]; auds[i] = url;
                        onChange({ ...question, optionAudios: auds });
                      }}
                      onClear={() => {
                        const auds = [...(question.optionAudios || ["", "", "", ""])]; auds[i] = "";
                        onChange({ ...question, optionAudios: auds });
                      }}
                      type="audio"
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Answer Description */}
          <div className="space-y-1">
            <Label className="text-sm font-semibold">Answer Description (optional)</Label>
            <Textarea rows={2} value={question.explanation} onChange={(e) => onChange({ ...question, explanation: e.target.value })} placeholder="Explanation shown after answering…" />
          </div>

          {/* Audio Play Count — shows for question audio AND option audios */}
          {(question.mediaType === "audio" || question.answerType === "audio") && (
            <div className="flex items-center gap-3 p-3 rounded-lg border bg-blue-50">
              <div className="flex-1">
                <Label className="text-sm font-semibold">Audio Play Count</Label>
                <p className="text-xs text-muted-foreground mt-0.5">
                  How many times the audio plays when the student taps play. Applies to both question audio and option audios.
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button type="button" variant="outline" size="sm" onClick={() => onChange({ ...question, audioLoop: Math.max(1, (question.audioLoop || 1) - 1) })}>−</Button>
                <span className="text-lg font-bold w-12 text-center">{question.audioLoop || 1}</span>
                <Button type="button" variant="outline" size="sm" onClick={() => onChange({ ...question, audioLoop: Math.min(100, (question.audioLoop || 1) + 1) })}>+</Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
