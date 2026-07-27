"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { FileText, Plus, Trash2, Clock, Upload, X, ChevronRight, ChevronLeft, Image as ImageIcon, Headphones, CheckCircle2, Copy, ClipboardPaste, Save, CloudOff, Cloud, RotateCcw } from "lucide-react";
import { toast } from "sonner";

// ─── Draft persistence (localStorage) ────────────────────────────────────────
// Auto-saves in-progress question drafts keyed by testId so a misclick,
// accidental close, or page refresh doesn't lose work. Drafts are cleared
// after a successful "Push to App".

const DRAFT_PREFIX = "dk_draft_test_";

function loadDraft(testId: string): Record<string, QuestionData> | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(DRAFT_PREFIX + testId);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object") return null;
    // Sanity-check: only keep entries that look like questions
    const out: Record<string, QuestionData> = {};
    for (const [k, v] of Object.entries(parsed)) {
      if (v && typeof v === "object" && "blockType" in (v as any)) {
        out[k] = v as QuestionData;
      }
    }
    return out;
  } catch {
    return null;
  }
}

function saveDraft(testId: string, questions: Record<string, QuestionData>) {
  if (typeof window === "undefined") return;
  try {
    // Only persist entries that have a stem (skip pure-empty placeholders)
    const filtered: Record<string, QuestionData> = {};
    for (const [k, q] of Object.entries(questions)) {
      if (q && (q.stem?.trim() || q.descText?.trim() || q.mediaImageUrl || q.mediaAudioUrl || q.descImageUrl || (q.options && q.options.some((o) => o?.trim())))) {
        filtered[k] = q;
      }
    }
    localStorage.setItem(DRAFT_PREFIX + testId, JSON.stringify(filtered));
  } catch {
    // localStorage might be full or disabled — fail silently
  }
}

function clearDraft(testId: string) {
  if (typeof window === "undefined") return;
  try {
    localStorage.removeItem(DRAFT_PREFIX + testId);
  } catch {
    // ignore
  }
}

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
  // Per-block enable flags — admin can hide the audio or text section
  textBlockEnabled?: boolean | null;
  audioBlockEnabled?: boolean | null;
  _count?: { items: number };
}

interface QuestionData {
  id?: string;
  testItemId?: string;
  blockType: "text" | "audio";
  blockNumber: number;
  setNumber?: number;
  title: string; // per-question title shown to students at top of question
  isFree: boolean; // free (demo) questions show at the top of QBank/Batch
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
  optionBlanks: string[];
  correctOption: number;
  explanation: string;
}

function emptyQuestion(blockType: "text" | "audio", blockNumber: number): QuestionData {
  return {
    blockType,
    blockNumber,
    setNumber: 1,
    title: "",
    isFree: false,
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
    optionBlanks: ["", "", "", ""],
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

  // ─── Make a Copy / Duplicate ──────────────────────────────────────────────
  // Two modes:
  //   • "Duplicate" — clone the whole test (all questions) into a target
  //     category. New test starts as a draft. Optionally add to a package.
  //   • "Copy Set" — only for question_bank tests. Copies one Set's questions
  //     into another existing test.
  const [duplicateTarget, setDuplicateTarget] = useState<Test | null>(null);

  async function doDuplicate(test: Test, newTitle: string, targetCategory: string, bundleId?: string) {
    try {
      const res = await fetch(`/api/admin/tests/${test.id}/copy`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          mode: "duplicate",
          targetCategory,
          newTitle: newTitle.trim() || `${test.title} (Copy)`,
        }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Duplicate failed"); return; }
      // Optionally add the new test to a package
      if (bundleId && d.test?.id) {
        try {
          await fetch(`/api/admin/bundles/${bundleId}/items`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ testId: d.test.id }),
          });
          toast.success(`Duplicated as "${d.test.title}" and added to package`);
        } catch {
          toast.success(`Duplicated as "${d.test.title}" (package add failed — try manually)`);
        }
      } else {
        toast.success(`Duplicated as "${d.test.title}"`);
      }
      setDuplicateTarget(null);
      load();
    } catch (e: any) {
      toast.error("Duplicate failed: " + (e?.message || "network error"));
    }
  }

  // Copy a single Set (only for question_bank tests) into another existing test
  async function copySet(test: Test) {
    const setNumberStr = prompt(`Which Set to copy? (1-10)`, "1");
    if (!setNumberStr) return;
    const setNumber = parseInt(setNumberStr, 10);
    if (isNaN(setNumber) || setNumber < 1) { toast.error("Invalid set number"); return; }
    const targetTestId = prompt(
      `Paste the destination test ID (you can copy it from the URL of any test you open in the admin panel):`,
      "",
    );
    if (!targetTestId?.trim()) { toast.error("Target test ID is required"); return; }
    try {
      const res = await fetch(`/api/admin/tests/${test.id}/copy`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          mode: "copySet",
          setNumber,
          targetTestId: targetTestId.trim(),
        }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Copy set failed"); return; }
      toast.success(`Copied ${d.copiedCount} questions from Set ${setNumber}`);
    } catch (e: any) {
      toast.error("Copy set failed: " + (e?.message || "network error"));
    }
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
                  {/* Block enable/disable toggles — only for exam & demo categories */}
                  {(testCategory === "exam" || testCategory === "demo") && (
                    <div className="flex items-center gap-1 mt-2" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={async () => {
                          const next = !(t.textBlockEnabled !== false);
                          try {
                            await fetch(`/api/admin/tests/${t.id}/toggle-block`, {
                              method: "POST",
                              headers: { "Content-Type": "application/json" },
                              body: JSON.stringify({ block: "text", enabled: next }),
                            });
                            toast.success(next ? "Text block enabled" : "Text block disabled — students won't see text questions");
                            load();
                          } catch {
                            toast.error("Failed to toggle");
                          }
                        }}
                        className={`text-[10px] px-2 py-0.5 rounded-full border transition-colors ${
                          t.textBlockEnabled !== false
                            ? "bg-emerald-50 text-emerald-700 border-emerald-300"
                            : "bg-slate-100 text-slate-500 border-slate-300 line-through"
                        }`}
                        title="Toggle text block visibility for students"
                      >
                        Text
                      </button>
                      <button
                        onClick={async () => {
                          const next = !(t.audioBlockEnabled !== false);
                          try {
                            await fetch(`/api/admin/tests/${t.id}/toggle-block`, {
                              method: "POST",
                              headers: { "Content-Type": "application/json" },
                              body: JSON.stringify({ block: "audio", enabled: next }),
                            });
                            toast.success(next ? "Audio block enabled" : "Audio block disabled — students won't see audio questions");
                            load();
                          } catch {
                            toast.error("Failed to toggle");
                          }
                        }}
                        className={`text-[10px] px-2 py-0.5 rounded-full border transition-colors ${
                          t.audioBlockEnabled !== false
                            ? "bg-amber-50 text-amber-700 border-amber-300"
                            : "bg-slate-100 text-slate-500 border-slate-300 line-through"
                        }`}
                        title="Toggle audio block visibility for students"
                      >
                        Audio
                      </button>
                    </div>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  {t.isPublished ? (
                    <Badge className="bg-green-500">🚀 Live</Badge>
                  ) : (
                    <Badge variant="secondary">📝 Draft</Badge>
                  )}
                  {/* Make a Copy — duplicate whole test into another category */}
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-blue-600 hover:text-blue-700"
                    title="Duplicate this test into another category"
                    onClick={(e) => { e.stopPropagation(); setDuplicateTarget(t); }}
                  >
                    <Copy className="w-4 h-4 mr-1" /> Duplicate
                  </Button>
                  {/* Copy Set — only for question_bank tests */}
                  {t.testCategory === "question_bank" && (
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-purple-600 hover:text-purple-700"
                      title="Copy one Set's questions into another test"
                      onClick={(e) => { e.stopPropagation(); copySet(t); }}
                    >
                      Copy Set
                    </Button>
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

      {duplicateTarget && (
        <DuplicateDialog
          test={duplicateTarget}
          onClose={() => setDuplicateTarget(null)}
          onDuplicate={(title, cat, bundleId) => doDuplicate(duplicateTarget, title, cat, bundleId)}
        />
      )}
    </div>
  );
}

// ─── Duplicate Dialog ────────────────────────────────────────────────────────
// Shows when admin clicks "Duplicate" on a test card. Lets the admin:
//   • Edit the new title (default: "{Original} (Copy)")
//   • Pick the target category (exam / demo / batch / chapter / question_bank)
//   • Optionally pick a package to add the duplicated test to
// Questions, blocks, and all other settings stay the same.
function DuplicateDialog({ test, onClose, onDuplicate }: {
  test: Test;
  onClose: () => void;
  onDuplicate: (newTitle: string, targetCategory: string, bundleId?: string) => void;
}) {
  const [newTitle, setNewTitle] = useState(`${test.title} (Copy)`);
  const [targetCategory, setTargetCategory] = useState(test.testCategory || "exam");
  const [bundles, setBundles] = useState<{ id: string; title: string; kind: string }[]>([]);
  const [selectedBundle, setSelectedBundle] = useState<string>("");
  const [busy, setBusy] = useState(false);

  // Load bundles so the admin can pick one to add the duplicate to
  useEffect(() => {
    fetch("/api/admin/bundles")
      .then((r) => r.json())
      .then((d) => setBundles(d.bundles || []))
      .catch(() => {});
  }, []);

  async function handleDuplicate() {
    setBusy(true);
    onDuplicate(newTitle, targetCategory, selectedBundle || undefined);
    setBusy(false);
  }

  return (
    <Dialog open={true} onOpenChange={(v) => { if (!v) onClose(); }}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Duplicate Test</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <Label className="text-sm font-semibold">New Title</Label>
            <Input
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              className="h-12 text-base"
              autoFocus
            />
            <p className="text-xs text-muted-foreground mt-1">
              Questions, blocks, audio settings, and all other content stay the same. Only the title changes.
            </p>
          </div>
          <div>
            <Label className="text-sm font-semibold">Target Category</Label>
            <Select value={targetCategory} onValueChange={setTargetCategory}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="exam">Exam</SelectItem>
                <SelectItem value="demo">Demo Exam</SelectItem>
                <SelectItem value="batch">Batch Exam</SelectItem>
                <SelectItem value="chapter">Chapter Exam</SelectItem>
                <SelectItem value="question_bank">Question Bank</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label className="text-sm font-semibold">Add to Package (optional)</Label>
            <Select value={selectedBundle} onValueChange={setSelectedBundle}>
              <SelectTrigger><SelectValue placeholder="— None —" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="">— None —</SelectItem>
                {bundles.map((b) => (
                  <SelectItem key={b.id} value={b.id}>
                    {b.title} ({b.kind})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground mt-1">
              If you pick a package, the duplicated test is automatically added to it.
            </p>
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-2 border-t">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button onClick={handleDuplicate} disabled={busy || !newTitle.trim()}>
            {busy ? "Duplicating…" : <><Copy className="w-4 h-4 mr-1" /> Duplicate</>}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
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
  const isQBank = testCategory === "question_bank";
  const [activeBlock, setActiveBlock] = useState<"text" | "audio">("text");
  const [activeNumber, setActiveNumber] = useState(1);
  // Set selector — only used for question_bank tests. Default Set 1.
  const [activeSet, setActiveSet] = useState(1);
  const [questions, setQuestions] = useState<Record<string, QuestionData>>({});
  const [loading, setLoading] = useState(true);
  const [clipboard, setClipboard] = useState<string>("");
  const [showPasteDialog, setShowPasteDialog] = useState(false);
  const [pasteCode, setPasteCode] = useState("");
  const [pushing, setPushing] = useState(false);
  const [isPublished, setIsPublished] = useState(test.isPublished);
  // Draft auto-save state
  const [draftSavedAt, setDraftSavedAt] = useState<number | null>(null);
  const [hasDraft, setHasDraft] = useState(false);
  const draftTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Track unsaved server-side question IDs so we can show "unsaved changes"
  const [dirty, setDirty] = useState(false);

  const textCount = test.textBlockCount || (isSimple ? 0 : 20);
  const audioCount = test.audioBlockCount || (isSimple ? 0 : 20);

  function key(blockType: string, blockNumber: number) {
    return `${blockType}-${blockNumber}`;
  }

  // Merge fetched server questions with any locally-saved draft.
  // Draft wins when present (it represents the most recent edit), but we
  // keep the server's `id` / `testItemId` so saving updates the same row
  // instead of creating a duplicate.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const serverMap: Record<string, QuestionData> = {};
      try {
        const r = await fetch(`/api/admin/tests/${test.id}/questions`);
        const d = await r.json();
        for (const q of d.questions || []) {
          serverMap[key(q.blockType, q.blockNumber)] = q;
        }
      } catch {
        // network error — fall through to draft-only
      }
      if (cancelled) return;

      const draft = loadDraft(test.id);
      if (draft) {
        const merged: Record<string, QuestionData> = { ...serverMap };
        for (const [k, dq] of Object.entries(draft)) {
          const sq = serverMap[k];
          merged[k] = sq
            ? { ...sq, ...dq, id: sq.id, testItemId: sq.testItemId }
            : dq;
        }
        setQuestions(merged);
        setHasDraft(true);
        toast.info("Restored unsaved draft from this browser.", { duration: 3500 });
      } else {
        setQuestions(serverMap);
      }
      setLoading(false);
    })();
    return () => { cancelled = true; };
  }, [test.id]);

  // Debounced auto-save to localStorage whenever questions change.
  // We also set a "dirty" flag so the editor can warn on close.
  const currentKey = key(activeBlock, activeNumber);
  const currentQuestion = questions[currentKey] || emptyQuestion(activeBlock, activeNumber);

  const scheduleDraftSave = useCallback((next: Record<string, QuestionData>) => {
    if (draftTimer.current) clearTimeout(draftTimer.current);
    draftTimer.current = setTimeout(() => {
      saveDraft(test.id, next);
      setDraftSavedAt(Date.now());
      setDirty(true);
    }, 600); // 600ms debounce — feels instant but avoids thrashing on every keystroke
  }, [test.id]);

  function updateQuestion(q: QuestionData) {
    setQuestions((prev) => {
      const next = { ...prev, [currentKey]: q };
      scheduleDraftSave(next);
      return next;
    });
  }

  // Force an immediate save (used after successful server-saves)
  function flushDraft() {
    if (draftTimer.current) clearTimeout(draftTimer.current);
    // Save whatever's in state right now
    setQuestions((prev) => {
      saveDraft(test.id, prev);
      return prev;
    });
  }

  async function saveQuestion() {
    const q = currentQuestion;
    if (!q.stem.trim()) { toast.error("Question text required"); return; }
    try {
      // Strip fields the API doesn't expect
      const payload = {
        blockType: q.blockType,
        blockNumber: q.blockNumber,
        setNumber: q.setNumber ?? activeSet,
        title: q.title || "",
        isFree: q.isFree || false,
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
        optionBlanks: q.optionBlanks || [],
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
      // Update local state with the saved question (preserves id/testItemId)
      setQuestions((prev) => ({
        ...prev,
        [currentKey]: { ...q, id: d.question?.id, testItemId: d.question?.testItemId ?? q.testItemId },
      }));
      // Persist the updated state to the draft so a refresh still works
      setTimeout(() => flushDraft(), 50);
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

  // ─── Copy ALL questions in this test ──────────────────────────────────────
  // Generates a single code that bundles every filled question. The admin
  // can paste the code into another test to bulk-import all questions at once.
  function copyAll() {
    const filled = Object.values(questions).filter(q => q.stem.trim());
    if (filled.length === 0) { toast.error("No questions to copy — add some first"); return; }
    const code = `DK-ALL-${test.id.slice(-4).toUpperCase()}-${Date.now().toString(36).toUpperCase()}`;
    const data = JSON.stringify({ code, allQuestions: filled, sourceTestId: test.id, sourceTitle: test.title });
    const allCopies = JSON.parse(localStorage.getItem("dk_copies") || "{}");
    allCopies[code] = data;
    localStorage.setItem("dk_copies", JSON.stringify(allCopies));
    navigator.clipboard.writeText(code);
    toast.success(`Copied ${filled.length} questions! Code: ${code}`);
  }

  function pasteQuestion() {
    setShowPasteDialog(true);
  }

  // ─── Paste All questions from a Copy-All code ─────────────────────────────
  // Reads the code from localStorage, parses the bundled questions, and
  // imports them into the current test with new block numbers (so they don't
  // overwrite existing questions). The setNumber is preserved from source.
  function doPasteAll(code: string) {
    const allCopies = JSON.parse(localStorage.getItem("dk_copies") || "{}");
    const data = allCopies[code.trim()];
    if (!data) { toast.error("Invalid code"); return; }
    const parsed = JSON.parse(data);
    if (!parsed.allQuestions || !Array.isArray(parsed.allQuestions)) {
      toast.error("This code is for a single question, not a bulk copy. Use Paste instead.");
      return;
    }
    const incoming: QuestionData[] = parsed.allQuestions;
    // Find the max block number currently in use so we append rather than overwrite
    const used = new Set(Object.values(questions).map(q => q.blockNumber));
    let nextNum = 1;
    while (used.has(nextNum)) nextNum++;
    const newQuestions = { ...questions };
    for (const q of incoming) {
      const k = key(q.blockType, nextNum);
      newQuestions[k] = {
        ...q,
        blockNumber: nextNum,
        // Preserve setNumber from source, default to current activeSet
        setNumber: q.setNumber ?? activeSet,
      };
      nextNum++;
    }
    setQuestions(newQuestions);
    scheduleDraftSave(newQuestions);
    toast.success(`Pasted ${incoming.length} questions — click Save on each to persist`);
    setShowPasteDialog(false);
    setPasteCode("");
  }

  async function pushToApp() {
    const filledQuestions = Object.values(questions).filter(q => q.stem.trim());
    if (filledQuestions.length === 0) {
      toast.error("Cannot push: add at least one question first");
      return;
    }

    setPushing(true);
    try {
      // Auto-save ALL filled questions — check each response
      let saveErrors = 0;
      for (const q of filledQuestions) {
        const payload = {
          blockType: q.blockType,
          blockNumber: q.blockNumber,
          setNumber: q.setNumber ?? activeSet,
          title: q.title || "",
          isFree: q.isFree || false,
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
        optionBlanks: q.optionBlanks || [],
          optionAudios: q.optionAudios || [],
          correctOption: q.correctOption,
          explanation: q.explanation || "",
        };
        const saveRes = await fetch(`/api/admin/tests/${test.id}/questions`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        });
        if (!saveRes.ok) {
          const errData = await saveRes.json().catch(() => ({}));
          console.error(`Question ${q.blockNumber} save failed:`, errData);
          saveErrors++;
        }
      }

      if (saveErrors > 0) {
        toast.error(`${saveErrors} question(s) failed to save. Check console for details.`);
        setPushing(false);
        return;
      }

      // Now publish
      const res = await fetch(`/api/admin/tests/${test.id}/publish`, { method: "POST" });
      const d = await res.json();
      if (!res.ok) {
        toast.error(d.error || `Push failed (HTTP ${res.status})`);
        return;
      }
      setIsPublished(true);
      // Clear the local draft — everything is now safely on the server
      clearDraft(test.id);
      setHasDraft(false);
      setDirty(false);
      toast.success(d.message || "Pushed to app — students can now see this exam");
    } catch (e: any) {
      toast.error("Push failed: " + (e.message || "network error"));
    } finally {
      setPushing(false);
    }
  }

  function discardDraft() {
    if (!hasDraft) return;
    if (!confirm("Discard local draft? Only unsaved changes from this browser will be removed — server-side questions stay intact.")) return;
    clearDraft(test.id);
    setHasDraft(false);
    setDirty(false);
    // Reload from server
    setLoading(true);
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
    toast.success("Draft discarded");
  }

  function attemptClose() {
    if (dirty && !isPublished) {
      // Confirm — but reassure the user that the draft is auto-saved
      if (!confirm("You have unsaved changes on this browser. They will be auto-saved as a draft — you can come back later. Close anyway?")) {
        return;
      }
      // Final flush of the draft just in case the debounce hasn't fired
      flushDraft();
    }
    onClose();
  }

  function doPaste() {
    const allCopies = JSON.parse(localStorage.getItem("dk_copies") || "{}");
    const data = allCopies[pasteCode.trim()];
    if (!data) { toast.error("Invalid paste code"); return; }
    const parsed = JSON.parse(data);
    // If this is a bulk-copy code (allQuestions array), paste all into this test
    if (parsed.allQuestions && Array.isArray(parsed.allQuestions)) {
      doPasteAll(pasteCode.trim());
      return;
    }
    // Single-question paste
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
    // Persist the deletion in the draft
    saveDraft(test.id, next);
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
    <Dialog open={true} onOpenChange={(v) => { if (!v) attemptClose(); }}>
      <DialogContent className="sm:max-w-5xl max-h-[95vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 flex-wrap">
            <span>{test.title}</span>
            <Badge variant="outline">{test.examType}</Badge>
            {test.category && <Badge variant="secondary">{test.category}</Badge>}
            {/* Draft status pill */}
            {hasDraft && (
              <Badge
                variant="outline"
                className="ml-1 gap-1 text-amber-700 border-amber-300 bg-amber-50"
                title={draftSavedAt ? `Auto-saved ${new Date(draftSavedAt).toLocaleTimeString()}` : "Auto-saved in this browser"}
              >
                <Cloud className="w-3 h-3" /> Draft saved
                <button
                  className="ml-1 hover:text-amber-900"
                  onClick={discardDraft}
                  title="Discard local draft"
                >
                  <RotateCcw className="w-3 h-3" />
                </button>
              </Badge>
            )}
            {!hasDraft && dirty && (
              <Badge variant="outline" className="ml-1 gap-1 text-slate-600 border-slate-300">
                <CloudOff className="w-3 h-3" /> Synced
              </Badge>
            )}
          </DialogTitle>
        </DialogHeader>

        {/* Set selector — only for question_bank tests. Lets admin organize
            questions into Set 1, 2, 3, 4, 5 within a single QBank test. */}
        {isQBank && (
          <div className="flex items-center gap-2 p-3 bg-purple-50 border border-purple-200 rounded-lg">
            <span className="text-sm font-semibold text-purple-800">Set:</span>
            {[1, 2, 3, 4, 5].map((s) => (
              <button
                key={s}
                onClick={() => setActiveSet(s)}
                className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${
                  activeSet === s
                    ? "bg-purple-600 text-white"
                    : "bg-white text-purple-700 border border-purple-300 hover:bg-purple-100"
                }`}
              >
                Set {s}
              </button>
            ))}
            <span className="text-xs text-purple-600 ml-2">
              Questions you add now go into Set {activeSet}. Use "Copy Set" on the test card to copy this set into another test.
            </span>
          </div>
        )}

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
            <Button onClick={copyAll} variant="outline" size="lg" className="text-purple-600 hover:text-purple-700">
              <Copy className="w-4 h-4 mr-1" /> Copy All
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

          {/* Question title (optional — shown at top of each question in exam UI) */}
          <div className="space-y-1">
            <Label className="text-sm font-semibold">Question Title <span className="text-muted-foreground font-normal">(optional — shown at the top of this question in the exam)</span></Label>
            <Input
              value={question.title || ""}
              onChange={(e) => onChange({ ...question, title: e.target.value })}
              placeholder="e.g. Question 1 — Vocabulary, or 어휘 (Vocabulary)"
              className="text-base"
              maxLength={200}
            />
          </div>

          {/* Free / Paid toggle — free questions show at the top of QBank + Batch packages */}
          <div className="flex items-center gap-3 p-3 rounded-lg border bg-slate-50">
            <input
              type="checkbox"
              id="isFree"
              checked={question.isFree || false}
              onChange={(e) => onChange({ ...question, isFree: e.target.checked })}
              className="w-5 h-5 rounded accent-emerald-600 cursor-pointer"
            />
            <div className="flex-1">
              <Label htmlFor="isFree" className="text-sm font-semibold cursor-pointer flex items-center gap-2">
                Free / Demo Question
                {(question.isFree) && (
                  <Badge className="bg-emerald-500 text-white text-[10px]">FREE</Badge>
                )}
              </Label>
              <p className="text-xs text-muted-foreground mt-0.5">
                Free questions appear at the TOP of Question Bank + Batch packages so students can try them before paying.
              </p>
            </div>
          </div>

          {/* Question text */}
          <div className="space-y-1">
            <Label className="text-sm font-semibold">Question <span className="text-red-500">*</span></Label>
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
                  <div key={i} className="space-y-1">
                    <div className="flex items-center gap-3">
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
                    {/* Underline bar — admin types a word that gets underlined in the option */}
                    <div className="flex items-center gap-2 pl-11">
                      <span className="text-xs text-slate-400 whitespace-nowrap">underline:</span>
                      <Input
                        value={question.optionBlanks[i] || ""}
                        onChange={(e) => {
                          const blanks = [...question.optionBlanks];
                          blanks[i] = e.target.value;
                          onChange({ ...question, optionBlanks: blanks });
                        }}
                        placeholder="word to underline (optional)"
                        className="h-7 text-xs flex-1 border-dashed border-slate-300"
                      />
                    </div>
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
                      url={question.optionImages[i] || ""}
                      onUpload={(url) => {
                        const imgs = [...question.optionImages]; imgs[i] = url;
                        onChange({ ...question, optionImages: imgs });
                      }}
                      onClear={() => {
                        const imgs = [...question.optionImages]; imgs[i] = "";
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
                      url={question.optionAudios[i] || ""}
                      onUpload={(url) => {
                        const auds = [...question.optionAudios]; auds[i] = url;
                        onChange({ ...question, optionAudios: auds });
                      }}
                      onClear={() => {
                        const auds = [...question.optionAudios]; auds[i] = "";
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
        </div>
      </div>
    </div>
  );
}
