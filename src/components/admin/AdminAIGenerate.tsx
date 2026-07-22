"use client";

import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Sparkles, Wand2, Loader2, CheckCircle2 } from "lucide-react";
import type { QuestionType, Difficulty, Chapter, Question } from "@/types";
import { toast } from "sonner";

export function AdminAIGenerate() {
  const [topic, setTopic] = useState("");
  const [count, setCount] = useState(5);
  const [type, setType] = useState<QuestionType>("SINGLE_CHOICE");
  const [difficulty, setDifficulty] = useState<Difficulty>("MEDIUM");
  const [chapterId, setChapterId] = useState<string>("");
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [generating, setGenerating] = useState(false);
  const [generated, setGenerated] = useState<Question[]>([]);
  const [saving, setSaving] = useState(false);

  // Lazy-load chapters
  if (chapters.length === 0) {
    fetch("/api/admin/chapters").then((r) => r.json()).then((d) => setChapters(d.chapters || [])).catch(() => null);
  }

  async function generate() {
    if (!topic.trim()) { toast.error("Enter a topic first"); return; }
    setGenerating(true);
    setGenerated([]);
    try {
      const res = await fetch("/api/admin/ai/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ topic, count, type, difficulty, chapterId: chapterId || undefined }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Generation failed"); return; }
      setGenerated(d.questions || []);
      toast.success(`Generated ${d.questions?.length || 0} questions`);
    } finally {
      setGenerating(false);
    }
  }

  async function saveAll() {
    setSaving(true);
    const res = await fetch("/api/admin/ai/save", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ questions: generated, chapterId: chapterId || undefined }),
    });
    setSaving(false);
    if (!res.ok) { toast.error("Save failed"); return; }
    toast.success(`Saved ${generated.length} questions to bank`);
    setGenerated([]);
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Sparkles className="h-6 w-6 text-purple-500" /> AI question generator
        </h1>
        <p className="text-sm text-muted-foreground mt-1">
          Enter a topic and let AI generate ready-to-use questions in seconds. Powered by Groq.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Generator</CardTitle>
          <CardDescription>Describe what you want to test. Be specific for better results.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div>
            <Label>Topic / Prompt</Label>
            <Textarea
              rows={3}
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              placeholder="e.g. Photosynthesis — light and dark reactions, factors affecting rate, importance for ecosystem"
            />
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div>
              <Label>Count</Label>
              <Input type="number" min={1} max={20} value={count} onChange={(e) => setCount(Math.min(20, Math.max(1, Number(e.target.value))))} />
            </div>
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
                  <SelectItem value="FILL_BLANK">Fill blank</SelectItem>
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
          <Button onClick={generate} disabled={generating}>
            {generating ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Generating…</> : <><Wand2 className="mr-2 h-4 w-4" /> Generate questions</>}
          </Button>
        </CardContent>
      </Card>

      {generated.length > 0 && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Generated ({generated.length})</CardTitle>
              <Button onClick={saveAll} disabled={saving}>
                {saving ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Saving…</> : <><CheckCircle2 className="mr-2 h-4 w-4" /> Save all to bank</>}
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-2 max-h-[60vh] overflow-y-auto">
            {generated.map((q, i) => (
              <div key={i} className="p-3 border rounded-lg">
                <div className="flex items-center gap-2 mb-1">
                  <Badge variant="outline" className="text-xs">{q.type.replace(/_/g, " ")}</Badge>
                  <Badge variant="secondary" className="text-xs">{q.difficulty.toLowerCase()}</Badge>
                  <Badge className="text-xs bg-purple-100 text-purple-700 hover:bg-purple-100"><Sparkles className="h-3 w-3 mr-1" /> AI</Badge>
                </div>
                <div className="text-sm font-medium">{q.stem}</div>
                {q.options && q.options.length > 0 && (
                  <ul className="text-xs text-muted-foreground mt-1">
                    {q.options.map((o, j) => (<li key={j}>{String.fromCharCode(65 + j)}. {o}</li>))}
                  </ul>
                )}
                {q.correctAnswer && (
                  <div className="text-xs text-emerald-600 mt-1">Answer: {q.correctAnswer}</div>
                )}
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
