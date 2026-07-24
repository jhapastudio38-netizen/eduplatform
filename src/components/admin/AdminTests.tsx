"use client";

import { useEffect, useState, useRef } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { FileText, Plus, Trash2, Clock, Power, PowerOff, Calendar, AlertCircle, Upload, X, Sparkles, ChevronRight, ChevronLeft, Image as ImageIcon, Headphones, CheckCircle2, Loader2 } from "lucide-react";
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
  _count?: { items: number };
}

interface Question {
  id?: string;
  type: string;
  difficulty: string;
  stem: string;
  options: string[];
  correctAnswer: string;
  explanation?: string;
  imageUrl?: string;
  audioUrl?: string;
  audioLoop?: number;
  audioLoopDelay?: number;
}

export function AdminTests() {
  const [tests, setTests] = useState<Test[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);

  function load() {
    setLoading(true);
    fetch("/api/admin/tests")
      .then((r) => r.json())
      .then((d) => setTests(d.tests || []))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function toggleActive(test: Test) {
    await fetch(`/api/admin/tests/${test.id}/toggle-active`, { method: "POST" });
    toast.success(test.isActive ? "Test deactivated" : "Test activated");
    load();
  }

  async function deleteTest(test: Test) {
    if (!confirm(`Delete "${test.title}"? This removes all questions.`)) return;
    await fetch(`/api/admin/tests/${test.id}`, { method: "DELETE" });
    toast.success("Test deleted");
    load();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Exams & Tests ({tests.length})</h1>
          <p className="text-sm text-muted-foreground mt-1">Create exams with unlimited questions, timer, schedule, and AI generation.</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="mr-1 h-4 w-4" /> Create Exam
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          <span className="ml-2 text-muted-foreground">Loading exams...</span>
        </div>
      ) : tests.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <FileText className="h-12 w-12 mx-auto mb-3 text-muted-foreground opacity-40" />
            <p className="text-muted-foreground">No exams yet. Create your first exam with unlimited questions.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2">
          {tests.map((t) => (
            <Card key={t.id} className={!t.isActive ? "opacity-60" : ""}>
              <CardContent className="p-4">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="font-semibold truncate">{t.title}</h3>
                      <Badge variant={t.isExam ? "destructive" : "secondary"}>
                        {t.isExam ? "EXAM" : "PRACTICE"}
                      </Badge>
                      {t._count && t._count.items > 0 && (
                        <Badge variant="outline">{t._count.items} questions</Badge>
                      )}
                    </div>
                    {t.description && (
                      <p className="text-sm text-muted-foreground truncate">{t.description}</p>
                    )}
                    <div className="flex items-center gap-3 mt-2 text-xs text-muted-foreground">
                      <span className="flex items-center gap-1"><Clock className="h-3 w-3" /> {t.durationMin} min</span>
                      <span className="flex items-center gap-1"><CheckCircle2 className="h-3 w-3" /> Pass: {t.passScore}%</span>
                      {t.negativeMarking > 0 && <span className="text-rose-500">−{t.negativeMarking}/wrong</span>}
                    </div>
                  </div>
                  <div className="flex gap-1 shrink-0">
                    <Button size="sm" variant="ghost" onClick={() => toggleActive(t)} title={t.isActive ? "Deactivate" : "Activate"}>
                      {t.isActive ? <PowerOff className="h-4 w-4" /> : <Power className="h-4 w-4" />}
                    </Button>
                    <Button size="sm" variant="ghost" className="text-rose-500" onClick={() => deleteTest(t)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <MultiStepExamCreator open={createOpen} onOpenChange={setCreateOpen} onSaved={load} />
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// MULTI-STEP EXAM CREATOR
// Step 1: Exam details (title, description, timer, etc.)
// Step 2: Question builder (unlimited questions with per-question media)
// ═══════════════════════════════════════════════════════════════════════════

function MultiStepExamCreator({ open, onOpenChange, onSaved }: {
  open: boolean; onOpenChange: (v: boolean) => void; onSaved: () => void;
}) {
  const [step, setStep] = useState(1);
  const [examId, setExamId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Step 1 state
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [durationMin, setDurationMin] = useState(30);
  const [isExam, setIsExam] = useState(false);
  const [examType, setExamType] = useState("REGULAR");
  const [passScore, setPassScore] = useState(50);
  const [negativeMarking, setNegativeMarking] = useState(0);
  const [maxAttempts, setMaxAttempts] = useState(1);
  const [shuffleQuestions, setShuffleQuestions] = useState(false);
  const [showResultImmediately, setShowResultImmediately] = useState(true);

  // Step 2 state
  const [questions, setQuestions] = useState<Question[]>([]);
  const [aiPrompt, setAiPrompt] = useState("");
  const [aiQuestionType, setAiQuestionType] = useState("SINGLE_CHOICE");
  const [aiCount, setAiCount] = useState(5);
  const [aiGenerating, setAiGenerating] = useState(false);

  function reset() {
    setStep(1);
    setExamId(null);
    setTitle(""); setDescription(""); setDurationMin(30); setIsExam(false);
    setExamType("REGULAR"); setPassScore(50); setNegativeMarking(0);
    setMaxAttempts(1); setShuffleQuestions(false); setShowResultImmediately(true);
    setQuestions([]); setAiPrompt("");
  }

  async function createExamAndGoToStep2() {
    if (!title.trim()) { toast.error("Title required"); return; }
    setBusy(true);
    try {
      const res = await fetch("/api/admin/tests", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title, description, durationMin, isExam, examType,
          passScore, negativeMarking, maxAttempts, shuffleQuestions,
          showResultImmediately, isPublished: true,
        }),
      });
      const data = await res.json();
      if (!res.ok) { toast.error(data.error || "Failed"); return; }
      setExamId(data.test.id);
      setStep(2);
      toast.success("Exam created! Now add questions.");
    } finally { setBusy(false); }
  }

  async function generateAIQuestions() {
    if (!aiPrompt.trim()) { toast.error("Enter a prompt for AI"); return; }
    setAiGenerating(true);
    try {
      const res = await fetch("/api/admin/ai/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          topic: aiPrompt,
          type: aiQuestionType,
          count: aiCount, difficulty: 'EASY',
        }),
      });
      const data = await res.json();
      if (!res.ok) { toast.error(data.error || "AI generation failed"); return; }
      const generated = data.questions || [];
      setQuestions([...questions, ...generated]);
      toast.success(`AI generated ${generated.length} questions!`);
      setAiPrompt("");
    } catch {
      toast.error("AI generation failed. Try again.");
    } finally { setAiGenerating(false); }
  }

  function addBlankQuestion() {
    setQuestions([...questions, {
      type: "SINGLE_CHOICE", difficulty: "EASY", stem: "",
      options: ["", "", "", ""], correctAnswer: "", explanation: "",
    }]);
  }

  function updateQuestion(index: number, updates: Partial<Question>) {
    const next = [...questions];
    next[index] = { ...next[index], ...updates };
    setQuestions(next);
  }

  function removeQuestion(index: number) {
    setQuestions(questions.filter((_, i) => i !== index));
  }

  async function uploadMedia(index: number, type: "image" | "audio", file: File) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("folder", type === "image" ? "questions" : "audio");
    toast.info(`Uploading ${type}...`);
    try {
      const res = await fetch("/api/admin/upload", { method: "POST", body: formData });
      const data = await res.json();
      if (data.ok) {
        const url = data.url.startsWith("http") ? data.url : `https://my-project-five-sepia.vercel.app${data.url}`;
        if (type === "image") {
          updateQuestion(index, { imageUrl: url });
        } else {
          updateQuestion(index, { audioUrl: url });
        }
        toast.success(`${type === "image" ? "Image" : "Audio"} uploaded!`);
      } else {
        toast.error("Upload failed");
      }
    } catch {
      toast.error("Upload failed");
    }
  }

  async function saveAllQuestions() {
    if (questions.length === 0) { toast.error("Add at least 1 question"); return; }
    setBusy(true);
    let saved = 0;
    for (const q of questions) {
      if (!q.stem.trim()) continue;
      try {
        // Create question
        const qRes = await fetch("/api/admin/questions", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            type: q.type, difficulty: q.difficulty, stem: q.stem,
            options: q.options.filter(o => o.trim()),
            correctAnswer: q.correctAnswer,
            explanation: q.explanation,
            imageUrl: q.imageUrl, audioUrl: q.audioUrl,
            audioLoop: q.audioLoop || 0, audioLoopDelay: q.audioLoopDelay || 0,
          }),
        });
        const qData = await qRes.json();
        if (qData.question && examId) {
          // Link question to test
          await fetch(`/api/admin/tests/${examId}/questions`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ questionId: qData.question.id, points: 1 }),
          });
          saved++;
        }
      } catch {}
    }
    setBusy(false);
    toast.success(`Saved ${saved} questions!`);
    onOpenChange(false);
    reset();
    onSaved();
  }

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!v) reset(); onOpenChange(v); }}>
      <DialogContent className="max-w-4xl max-h-[92vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{step === 1 ? "Create Exam — Step 1: Details" : "Step 2: Add Questions"}</DialogTitle>
          <DialogDescription>
            {step === 1 ? "Enter exam details, then click Next to add questions." : `Add unlimited questions to "${title}". Upload images, audio, or use AI.`}
          </DialogDescription>
        </DialogHeader>

        {/* ─── STEP 1: EXAM DETAILS ─── */}
        {step === 1 && (
          <div className="space-y-4">
            <div>
              <Label>Exam Title *</Label>
              <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="TOPIK I Listening Practice" />
            </div>
            <div>
              <Label>Description</Label>
              <Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Practice test for TOPIK Level 1 listening" />
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <div>
                <Label>Duration (min)</Label>
                <Input type="number" value={durationMin} onChange={(e) => setDurationMin(parseInt(e.target.value) || 30)} />
              </div>
              <div>
                <Label>Pass Score (%)</Label>
                <Input type="number" value={passScore} onChange={(e) => setPassScore(parseInt(e.target.value) || 50)} />
              </div>
              <div>
                <Label>Negative Marking</Label>
                <Input type="number" step="0.25" value={negativeMarking} onChange={(e) => setNegativeMarking(parseFloat(e.target.value) || 0)} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>Exam Type</Label>
                <Select value={examType} onValueChange={setExamType}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="REGULAR">Regular Practice</SelectItem>
                    <SelectItem value="UBT">UBT Test</SelectItem>
                    <SelectItem value="TOPIK_I">TOPIK I</SelectItem>
                    <SelectItem value="TOPIK_II">TOPIK II</SelectItem>
                    <SelectItem value="DEMO">Demo</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Max Attempts (0=∞)</Label>
                <Input type="number" value={maxAttempts} onChange={(e) => setMaxAttempts(parseInt(e.target.value) || 0)} />
              </div>
            </div>
            <div className="flex gap-4 flex-wrap">
              <div className="flex items-center gap-2">
                <Switch checked={isExam} onCheckedChange={setIsExam} />
                <Label>Graded Exam</Label>
              </div>
              <div className="flex items-center gap-2">
                <Switch checked={shuffleQuestions} onCheckedChange={setShuffleQuestions} />
                <Label>Shuffle Questions</Label>
              </div>
              <div className="flex items-center gap-2">
                <Switch checked={showResultImmediately} onCheckedChange={setShowResultImmediately} />
                <Label>Show Results Immediately</Label>
              </div>
            </div>
            <div className="flex justify-end">
              <Button onClick={createExamAndGoToStep2} disabled={busy || !title.trim()}>
                {busy ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <ChevronRight className="mr-2 h-4 w-4" />}
                Next: Add Questions
              </Button>
            </div>
          </div>
        )}

        {/* ─── STEP 2: QUESTION BUILDER ─── */}
        {step === 2 && (
          <div className="space-y-4">
            {/* AI Generation Section */}
            <Card className="bg-purple-50 border-purple-200">
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <Sparkles className="h-4 w-4 text-purple-600" />
                  <span className="font-semibold text-sm text-purple-900">AI Question Generator</span>
                </div>
                <div className="flex gap-2 flex-wrap mb-2">
                  <Select value={aiQuestionType} onValueChange={setAiQuestionType}>
                    <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="SINGLE_CHOICE">Single Choice</SelectItem>
                      <SelectItem value="MULTIPLE_CHOICE">Multiple Choice</SelectItem>
                      <SelectItem value="TRUE_FALSE">True/False</SelectItem>
                      <SelectItem value="FILL_BLANK">Fill in Blank</SelectItem>
                    </SelectContent>
                  </Select>
                  <Input type="number" min={1} max={50} value={aiCount} onChange={(e) => setAiCount(parseInt(e.target.value) || 5)} className="w-24" />
                  <Input value={aiPrompt} onChange={(e) => setAiPrompt(e.target.value)} placeholder="e.g. Generate questions about Korean greetings for beginners" className="flex-1 min-w-[200px]" />
                  <Button onClick={generateAIQuestions} disabled={aiGenerating} className="bg-purple-600 hover:bg-purple-700">
                    {aiGenerating ? <Loader2 className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}
                    Generate
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* Questions List */}
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">{questions.length} question{questions.length !== 1 ? "s" : ""}</span>
              <Button size="sm" variant="outline" onClick={addBlankQuestion}>
                <Plus className="mr-1 h-4 w-4" /> Add Question
              </Button>
            </div>

            {questions.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                <FileText className="h-10 w-10 mx-auto mb-2 opacity-40" />
                <p>No questions yet. Use AI or add manually.</p>
              </div>
            ) : (
              <div className="space-y-3">
                {questions.map((q, i) => (
                  <QuestionEditor
                    key={i}
                    index={i}
                    question={q}
                    onUpdate={(updates) => updateQuestion(i, updates)}
                    onRemove={() => removeQuestion(i)}
                    onUploadMedia={(type, file) => uploadMedia(i, type, file)}
                  />
                ))}
              </div>
            )}

            {/* Footer */}
            <div className="flex justify-between pt-2 border-t">
              <Button variant="outline" onClick={() => setStep(1)}>
                <ChevronLeft className="mr-1 h-4 w-4" /> Back
              </Button>
              <Button onClick={saveAllQuestions} disabled={busy || questions.length === 0}>
                {busy ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <CheckCircle2 className="mr-2 h-4 w-4" />}
                Save All Questions ({questions.length})
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// QUESTION EDITOR — per-question control with image, audio, options
// ═══════════════════════════════════════════════════════════════════════════

function QuestionEditor({ index, question, onUpdate, onRemove, onUploadMedia }: {
  index: number;
  question: Question;
  onUpdate: (updates: Partial<Question>) => void;
  onRemove: () => void;
  onUploadMedia: (type: "image" | "audio", file: File) => void;
}) {
  const imgRef = useRef<HTMLInputElement>(null);
  const audioRef = useRef<HTMLInputElement>(null);

  return (
    <Card className="border-slate-200">
      <CardContent className="p-4 space-y-3">
        {/* Header */}
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold text-slate-500">Q{index + 1}</span>
          <Button size="sm" variant="ghost" className="text-rose-500 h-7" onClick={onRemove}>
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>

        {/* Type + Difficulty */}
        <div className="grid grid-cols-2 gap-2">
          <Select value={question.type} onValueChange={(v) => onUpdate({ type: v })}>
            <SelectTrigger className="h-8 text-xs"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="SINGLE_CHOICE">Single Choice</SelectItem>
              <SelectItem value="MULTIPLE_CHOICE">Multiple Choice</SelectItem>
              <SelectItem value="TRUE_FALSE">True/False</SelectItem>
              <SelectItem value="FILL_BLANK">Fill in Blank</SelectItem>
              <SelectItem value="ONE_WORD">One Word</SelectItem>
            </SelectContent>
          </Select>
          <Select value={question.difficulty} onValueChange={(v) => onUpdate({ difficulty: v })}>
            <SelectTrigger className="h-8 text-xs"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="EASY">Easy</SelectItem>
              <SelectItem value="MEDIUM">Medium</SelectItem>
              <SelectItem value="HARD">Hard</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Question text */}
        <Textarea
          rows={2}
          value={question.stem}
          onChange={(e) => onUpdate({ stem: e.target.value })}
          placeholder="Enter question text..."
          className="text-sm"
        />

        {/* Image upload + preview */}
        {question.imageUrl && (
          <div className="relative rounded-lg overflow-hidden border">
            <img src={question.imageUrl} alt="Question" className="w-full max-h-40 object-cover" />
            <Button size="sm" variant="destructive" className="absolute top-2 right-2 h-6 w-6 p-0" onClick={() => onUpdate({ imageUrl: "" })}>
              <X className="h-3 w-3" />
            </Button>
          </div>
        )}
        <input ref={imgRef} type="file" accept="image/*" className="hidden" onChange={(e) => { const f = e.target.files?.[0]; if (f) onUploadMedia("image", f); e.target.value = ""; }} />
        <input ref={audioRef} type="file" accept="audio/*" className="hidden" onChange={(e) => { const f = e.target.files?.[0]; if (f) onUploadMedia("audio", f); e.target.value = ""; }} />

        {/* Media buttons */}
        <div className="flex gap-2">
          <Button size="sm" variant="outline" onClick={() => imgRef.current?.click()}>
            <ImageIcon className="mr-1 h-3 w-3" /> {question.imageUrl ? "Change Image" : "Upload Image"}
          </Button>
          <Button size="sm" variant="outline" onClick={() => audioRef.current?.click()}>
            <Headphones className="mr-1 h-3 w-3" /> {question.audioUrl ? "Change Audio" : "Upload Audio"}
          </Button>
        </div>

        {/* Audio preview + loop settings */}
        {question.audioUrl && (
          <div className="space-y-1">
            <audio controls src={question.audioUrl} className="w-full h-8" />
            <div className="flex gap-2 items-center">
              <Label className="text-xs">Loop:</Label>
              <Input type="number" className="h-7 w-16 text-xs" value={question.audioLoop || 0} onChange={(e) => onUpdate({ audioLoop: parseInt(e.target.value) || 0 })} min={-1} max={20} />
              <Label className="text-xs">Delay (s):</Label>
              <Input type="number" className="h-7 w-16 text-xs" value={question.audioLoopDelay || 0} onChange={(e) => onUpdate({ audioLoopDelay: parseInt(e.target.value) || 0 })} min={0} max={60} />
              <Button size="sm" variant="ghost" className="text-rose-500 h-7" onClick={() => onUpdate({ audioUrl: "", audioLoop: 0, audioLoopDelay: 0 })}>
                <X className="h-3 w-3" />
              </Button>
            </div>
          </div>
        )}

        {/* Options (for MCQ) */}
        {(question.type === "SINGLE_CHOICE" || question.type === "MULTIPLE_CHOICE" || question.type === "TRUE_FALSE") && (
          <div className="space-y-1">
            <Label className="text-xs">Options (click ✓ to mark correct)</Label>
            {question.options.map((opt, oi) => (
              <div key={oi} className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => onUpdate({ correctAnswer: JSON.stringify(opt) })}
                  className={`shrink-0 w-6 h-6 rounded-full border-2 flex items-center justify-center text-xs ${
                    JSON.parse(question.correctAnswer || '""') === opt
                      ? "bg-green-500 border-green-500 text-white"
                      : "bg-white border-slate-300"
                  }`}
                >
                  {JSON.parse(question.correctAnswer || '""') === opt && "✓"}
                </button>
                <Input
                  className="h-8 text-xs flex-1"
                  value={opt}
                  onChange={(e) => {
                    const next = [...question.options]; next[oi] = e.target.value;
                    onUpdate({ options: next });
                  }}
                  placeholder={`Option ${String.fromCharCode(65 + oi)}`}
                />
                {question.options.length > 2 && (
                  <Button size="sm" variant="ghost" className="h-7 px-1 text-rose-500" onClick={() => onUpdate({ options: question.options.filter((_, j) => j !== oi) })}>
                    <X className="h-3 w-3" />
                  </Button>
                )}
              </div>
            ))}
            <Button size="sm" variant="ghost" className="h-7 text-xs" onClick={() => onUpdate({ options: [...question.options, ""] })}>
              <Plus className="h-3 w-3 mr-1" /> Add option
            </Button>
          </div>
        )}

        {/* Fill blank / One word answer */}
        {(question.type === "FILL_BLANK" || question.type === "ONE_WORD") && (
          <div>
            <Label className="text-xs">Correct Answer</Label>
            <Input className="h-8 text-xs" value={question.correctAnswer ? JSON.parse(question.correctAnswer) : ""} onChange={(e) => onUpdate({ correctAnswer: JSON.stringify(e.target.value) })} placeholder="Answer" />
          </div>
        )}

        {/* Explanation */}
        <Input
          className="h-8 text-xs"
          value={question.explanation || ""}
          onChange={(e) => onUpdate({ explanation: e.target.value })}
          placeholder="Explanation (optional)"
        />
      </CardContent>
    </Card>
  );
}
