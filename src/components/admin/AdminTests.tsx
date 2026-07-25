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

export function AdminTests() {
  const [tests, setTests] = useState<Test[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [editingTest, setEditingTest] = useState<Test | null>(null);

  function load() {
    setLoading(true);
    fetch("/api/admin/tests")
      .then((r) => r.json())
      .then((d) => setTests(d.tests || []))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

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
          <h1 className="text-2xl font-bold">Exams ({tests.length})</h1>
          <p className="text-sm text-muted-foreground">Block-based exam builder — 20 text + 20 audio questions</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="w-4 h-4 mr-1" /> New Exam
        </Button>
      </div>

      {loading ? (
        <p className="text-center py-8 text-muted-foreground">Loading…</p>
      ) : tests.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <FileText className="w-12 h-12 mx-auto mb-3 text-muted-foreground" />
            <p className="text-muted-foreground">No exams yet. Click "New Exam" to create one.</p>
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
                  <Badge variant={t.isActive ? "default" : "secondary"}>
                    {t.isActive ? "Active" : "Inactive"}
                  </Badge>
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
          onOpenChange={setCreateOpen}
          onCreated={(t) => { setCreateOpen(false); setEditingTest(t); load(); }}
        />
      )}

      {editingTest && (
        <ExamEditor
          test={editingTest}
          onClose={() => { setEditingTest(null); load(); }}
        />
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// CREATE EXAM DIALOG — Step 1: exam details
// ═══════════════════════════════════════════════════════════════════════════

function CreateExamDialog({ open, onOpenChange, onCreated }: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onCreated: (t: Test) => void;
}) {
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
    textBlockCount: 20,
    audioBlockCount: 20,
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
      const res = await fetch("/api/admin/tests", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ...form,
          price: form.price ? parseFloat(form.price) : undefined,
          isExam: true,
          isPublished: true,
        }),
      });
      if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
      const d = await res.json();
      toast.success("Exam created — now add questions");
      onCreated(d.test);
    } finally { setBusy(false); }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader><DialogTitle>Create New Exam</DialogTitle></DialogHeader>
        <div className="space-y-3">
          {/* Exam Name */}
          <div>
            <Label>Exam Name *</Label>
            <Input value={form.title} onChange={(e) => setForm(f => ({ ...f, title: e.target.value }))} placeholder="e.g. UBT Mock Test 1" />
          </div>

          {/* Exam Details */}
          <div>
            <Label>Exam Details</Label>
            <Textarea rows={2} value={form.description} onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))} placeholder="Brief description of the exam" />
          </div>

          {/* Exam Time + Price */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Exam Time (minutes) *</Label>
              <Input type="number" value={form.durationMin} onChange={(e) => setForm(f => ({ ...f, durationMin: parseInt(e.target.value) || 60 }))} min={1} />
            </div>
            <div>
              <Label>Exam Price (optional)</Label>
              <Input type="number" value={form.price} onChange={(e) => setForm(f => ({ ...f, price: e.target.value }))} placeholder="0 = free" min={0} />
            </div>
          </div>

          {/* Exam Type + Category */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Exam Type *</Label>
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
              <Label>Category (shows in app)</Label>
              <Input value={form.category} onChange={(e) => setForm(f => ({ ...f, category: e.target.value }))} placeholder="e.g. Beginner, TOPIK 1" />
            </div>
          </div>

          {/* Featured Image */}
          <div>
            <Label>Featured Image</Label>
            <div className="flex gap-2">
              <Input value={form.featuredImage} onChange={(e) => setForm(f => ({ ...f, featuredImage: e.target.value }))} placeholder="Upload or paste URL…" className="flex-1" />
              <label className="cursor-pointer">
                <input type="file" accept="image/*" className="hidden" onChange={async (e) => {
                  const f = e.target.files?.[0]; if (!f) return;
                  try { const url = await uploadFile(f, "exam-featured"); setForm(p => ({ ...p, featuredImage: url })); toast.success("Image uploaded"); }
                  catch (err: any) { toast.error(err.message); }
                }} />
                <span className="inline-flex items-center h-9 px-3 rounded-md bg-primary text-primary-foreground text-sm cursor-pointer hover:bg-primary/90">📁 Upload</span>
              </label>
            </div>
            {form.featuredImage && <img src={form.featuredImage} alt="Featured" className="mt-2 max-h-32 rounded border" />}
          </div>

          {/* Audio Settings */}
          <div className="p-3 border rounded-lg bg-slate-50 space-y-3">
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

          {/* Block Counts */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Text Questions Count</Label>
              <Input type="number" value={form.textBlockCount} onChange={(e) => setForm(f => ({ ...f, textBlockCount: parseInt(e.target.value) || 20 }))} min={1} max={100} />
            </div>
            <div>
              <Label>Audio Questions Count</Label>
              <Input type="number" value={form.audioBlockCount} onChange={(e) => setForm(f => ({ ...f, audioBlockCount: parseInt(e.target.value) || 20 }))} min={1} max={100} />
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={create} disabled={busy}>{busy ? "Creating…" : "Create Exam"}</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// EXAM EDITOR — Step 2: block-based question editor
// ═══════════════════════════════════════════════════════════════════════════

function ExamEditor({ test, onClose }: { test: Test; onClose: () => void }) {
  const [activeBlock, setActiveBlock] = useState<"text" | "audio">("text");
  const [activeNumber, setActiveNumber] = useState(1);
  const [questions, setQuestions] = useState<Record<string, QuestionData>>({});
  const [loading, setLoading] = useState(true);
  const [clipboard, setClipboard] = useState<string>("");
  const [showPasteDialog, setShowPasteDialog] = useState(false);
  const [pasteCode, setPasteCode] = useState("");

  const textCount = test.textBlockCount || 20;
  const audioCount = test.audioBlockCount || 20;

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
      const res = await fetch(`/api/admin/tests/${test.id}/questions`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(q),
      });
      if (!res.ok) { const d = await res.json(); toast.error(d.error || "Save failed"); return; }
      toast.success(`Question ${q.blockNumber} saved`);
    } catch {
      toast.error("Save failed");
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

        {/* Block tabs */}
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

        {/* Block number selector — grid of numbered buttons */}
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

        {/* Action buttons — Done, Copy, Paste */}
        <div className="flex items-center justify-between border-t pt-3">
          <div className="flex gap-2">
            <Button onClick={saveQuestion} variant="default">
              <Save className="w-4 h-4 mr-1" /> Done
            </Button>
            <Button onClick={copyQuestion} variant="outline">
              <Copy className="w-4 h-4 mr-1" /> Copy Question
            </Button>
            <Button onClick={pasteQuestion} variant="outline">
              <ClipboardPaste className="w-4 h-4 mr-1" /> Paste Question
            </Button>
          </div>
          <Button variant="ghost" onClick={onClose}>Close</Button>
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
