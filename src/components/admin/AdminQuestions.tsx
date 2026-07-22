"use client";

import { useEffect, useState, useRef } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Plus, Upload, FileQuestion, Trash2, Sparkles, FileText } from "lucide-react";
import type { Question, QuestionType, Difficulty, Chapter } from "@/types";
import { toast } from "sonner";

export function AdminQuestions() {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [filter, setFilter] = useState<string>("all");
  const fileRef = useRef<HTMLInputElement>(null);

  function load() {
    fetch("/api/admin/questions")
      .then((r) => r.json())
      .then((d) => setQuestions(d.questions || []));
    fetch("/api/admin/chapters")
      .then((r) => r.json())
      .then((d) => setChapters(d.chapters || []));
  }
  useEffect(load, []);

  const filtered = filter === "all" ? questions : questions.filter((q) => q.chapterId === filter);

  async function onUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append("file", file);
    formData.append("chapterId", filter === "all" ? "" : filter);
    toast.info("Parsing file…");
    const res = await fetch("/api/admin/questions/upload", { method: "POST", body: formData });
    const d = await res.json();
    if (!res.ok) { toast.error(d.error || "Upload failed"); return; }
    toast.success(`Imported ${d.imported} question(s)`);
    load();
    if (fileRef.current) fileRef.current.value = "";
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-2xl font-bold">Questions ({filtered.length})</h1>
          <p className="text-sm text-muted-foreground">Add manually, upload a file, or generate with AI.</p>
        </div>
        <div className="flex gap-2">
          <input ref={fileRef} type="file" accept=".json,.csv,.txt,.md" onChange={onUpload} className="hidden" />
          <Button variant="outline" onClick={() => fileRef.current?.click()}>
            <Upload className="mr-1 h-4 w-4" /> Upload file
          </Button>
          <QuestionDialog chapters={chapters} onSaved={load} />
        </div>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">All questions</CardTitle>
            <Select value={filter} onValueChange={setFilter}>
              <SelectTrigger className="w-56"><SelectValue placeholder="Filter by chapter" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All chapters</SelectItem>
                {chapters.map((c) => <SelectItem key={c.id} value={c.id}>{c.title}</SelectItem>)}
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent className="space-y-2 max-h-[60vh] overflow-y-auto">
          {filtered.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-6">
              No questions yet. Add one manually, upload a JSON/CSV/TXT/MD file, or use AI to generate.
            </p>
          ) : filtered.map((q) => (
            <div key={q.id} className="p-3 border rounded-lg">
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <Badge variant="outline" className="text-xs">{q.type.replace(/_/g, " ")}</Badge>
                    <Badge variant="secondary" className="text-xs">{q.difficulty.toLowerCase()}</Badge>
                    {q.aiGenerated && <Badge className="text-xs bg-purple-100 text-purple-700 hover:bg-purple-100"><Sparkles className="h-3 w-3 mr-1" /> AI</Badge>}
                  </div>
                  <div className="text-sm font-medium line-clamp-2">{q.stem}</div>
                  {q.options && q.options.length > 0 && (
                    <ul className="text-xs text-muted-foreground mt-1 space-y-0.5">
                      {q.options.slice(0, 4).map((o, i) => (<li key={i}>{String.fromCharCode(65 + i)}. {o}</li>))}
                    </ul>
                  )}
                </div>
                <Button size="icon" variant="ghost" onClick={async () => {
                  await fetch(`/api/admin/questions/${q.id}`, { method: "DELETE" });
                  load();
                }}>
                  <Trash2 className="h-4 w-4 text-rose-500" />
                </Button>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}

function QuestionDialog({ chapters, onSaved }: { chapters: Chapter[]; onSaved: () => void }) {
  const [open, setOpen] = useState(false);
  const [type, setType] = useState<QuestionType>("SINGLE_CHOICE");
  const [difficulty, setDifficulty] = useState<Difficulty>("MEDIUM");
  const [chapterId, setChapterId] = useState<string>("");
  const [stem, setStem] = useState("");
  const [options, setOptions] = useState<string[]>(["", "", "", ""]);
  const [correct, setCorrect] = useState("");
  const [explanation, setExplanation] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [audioUrl, setAudioUrl] = useState("");
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!stem.trim()) { toast.error("Question stem required"); return; }
    setBusy(true);
    const body: Record<string, unknown> = { type, difficulty, chapterId: chapterId || undefined, stem, explanation, imageUrl: imageUrl || undefined, audioUrl: audioUrl || undefined };
    if (type === "SINGLE_CHOICE" || type === "MULTIPLE_CHOICE" || type === "TRUE_FALSE") {
      body.options = options.filter((o) => o.trim());
      body.correctAnswer = JSON.stringify(
        type === "MULTIPLE_CHOICE" ? correct.split(",").map((s) => s.trim()).filter(Boolean) : correct,
      );
    } else if (type === "ONE_WORD" || type === "FILL_BLANK") {
      body.correctAnswer = JSON.stringify(correct);
    } else {
      body.correctAnswer = JSON.stringify(correct);
    }
    const res = await fetch("/api/admin/questions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Question added");
    setOpen(false);
    setStem(""); setOptions(["", "", "", ""]); setCorrect(""); setExplanation(""); setImageUrl(""); setAudioUrl("");
    onSaved();
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button><Plus className="mr-1 h-4 w-4" /> Add question</Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader><DialogTitle>New question</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div className="grid grid-cols-3 gap-3">
            <div>
              <Label>Type</Label>
              <Select value={type} onValueChange={(v) => setType(v as QuestionType)}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="SINGLE_CHOICE">Single choice</SelectItem>
                  <SelectItem value="MULTIPLE_CHOICE">Multiple choice</SelectItem>
                  <SelectItem value="TRUE_FALSE">True / False</SelectItem>
                  <SelectItem value="ONE_WORD">One word</SelectItem>
                  <SelectItem value="SHORT_ANSWER">Short answer</SelectItem>
                  <SelectItem value="LONG_ANSWER">Long answer</SelectItem>
                  <SelectItem value="FILL_BLANK">Fill in the blank</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label>Difficulty</Label>
              <Select value={difficulty} onValueChange={(v) => setDifficulty(v as Difficulty)}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="EASY">Easy</SelectItem>
                  <SelectItem value="MEDIUM">Medium</SelectItem>
                  <SelectItem value="HARD">Hard</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label>Chapter</Label>
              <Select value={chapterId} onValueChange={setChapterId}>
                <SelectTrigger><SelectValue placeholder="Optional" /></SelectTrigger>
                <SelectContent>
                  {chapters.map((c) => <SelectItem key={c.id} value={c.id}>{c.title}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div>
            <Label>Question</Label>
            <Textarea rows={3} value={stem} onChange={(e) => setStem(e.target.value)} placeholder="What is the capital of France?" />
          </div>
          {(type === "SINGLE_CHOICE" || type === "MULTIPLE_CHOICE" || type === "TRUE_FALSE") && (
            <div>
              <Label>Options (one per line)</Label>
              {options.map((o, i) => (
                <Input key={i} className="mb-1" value={o} onChange={(e) => {
                  const next = [...options]; next[i] = e.target.value; setOptions(next);
                }} placeholder={`Option ${String.fromCharCode(65 + i)}`} />
              ))}
              <Button size="sm" variant="outline" className="mt-1" onClick={() => setOptions([...options, ""])}>+ Add option</Button>
              <div className="mt-2">
                <Label>Correct answer</Label>
                <Input value={correct} onChange={(e) => setCorrect(e.target.value)}
                  placeholder={type === "MULTIPLE_CHOICE" ? "Option A, Option C" : "Option A"} />
              </div>
            </div>
          )}
          {(type === "ONE_WORD" || type === "FILL_BLANK") && (
            <div>
              <Label>Correct answer</Label>
              <Input value={correct} onChange={(e) => setCorrect(e.target.value)} placeholder="Paris" />
            </div>
          )}
          <div>
            <Label>Explanation (optional)</Label>
            <Textarea rows={2} value={explanation} onChange={(e) => setExplanation(e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Image URL (optional — for visual questions)</Label>
              <Input value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} placeholder="https://..." />
            </div>
            <div>
              <Label>Audio URL (optional — for listening questions)</Label>
              <Input value={audioUrl} onChange={(e) => setAudioUrl(e.target.value)} placeholder="https://..." />
            </div>
          </div>
          <Button onClick={save} disabled={busy}>{busy ? "Saving…" : "Create question"}</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
