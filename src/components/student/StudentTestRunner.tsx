"use client";

/**
 * Student Test Runner — Korean exam experience.
 * Features:
 *   - Question navigation grid (jump to any question)
 *   - Skip question (come back later)
 *   - Go back to previous question
 *   - Timer with auto-submit on expiry
 *   - Image + audio display for media questions
 *   - Marked/unmarked status indicators
 *   - Confirmation before submit
 */

import { useEffect, useState, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  ArrowLeft, ArrowRight, Send, Clock, CheckCircle2, XCircle,
  Flag, Grid3x3, AlertCircle, ChevronLeft, ChevronRight
} from "lucide-react";
import type { Test } from "@/types";
import { toast } from "sonner";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription
} from "@/components/ui/dialog";

type Answers = Record<string, string | string[]>;

export function StudentTestRunner({ test, onExit }: { test: Test; onExit: () => void }) {
  const [fullTest, setFullTest] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const items = fullTest?.items ?? [];
  const [current, setCurrent] = useState(0);
  const [answers, setAnswers] = useState<Answers>({});
  const [marked, setMarked] = useState<Set<string>>(new Set());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ score: number; max: number; graded: boolean } | null>(null);
  const [secondsLeft, setSecondsLeft] = useState((fullTest?.durationMin || test.durationMin) * 60);
  const [showNav, setShowNav] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/student/tests/${test.id}`)
      .then(r => r.json())
      .then(d => {
        if (cancelled) return;
        if (d.test) {
          setFullTest(d.test);
          setSecondsLeft(d.test.durationMin * 60);
        }
        setLoading(false);
      })
      .catch(() => setLoading(false));
    return () => { cancelled = true; };
  }, [test.id]);

  useEffect(() => {
    if (!fullTest) return;
    timerRef.current = setInterval(() => {
      setSecondsLeft(s => {
        if (s <= 1) {
          if (timerRef.current) clearInterval(timerRef.current);
          submit(true);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [fullTest]);

  function setAns(qId: string, val: string | string[]) {
    setAnswers(a => ({ ...a, [qId]: val }));
  }

  function toggleMark(qId: string) {
    setMarked(prev => {
      const next = new Set(prev);
      if (next.has(qId)) next.delete(qId);
      else next.add(qId);
      return next;
    });
  }

  async function submit(auto = false) {
    if (timerRef.current) clearInterval(timerRef.current);
    setSubmitting(true);
    setShowConfirm(false);
    try {
      const res = await fetch(`/api/student/tests/${test.id}/submit`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ answers }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast.error(data.error || "Failed to submit");
        return;
      }
      setResult({ score: data.score, max: data.maxScore, graded: data.graded });
      toast[auto ? "info" : "success"](auto ? "Time's up — exam auto-submitted" : "Exam submitted!");
    } finally {
      setSubmitting(false);
    }
  }

  const mm = Math.floor(secondsLeft / 60).toString().padStart(2, "0");
  const ss = (secondsLeft % 60).toString().padStart(2, "0");
  const isLowTime = secondsLeft < 60;

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin mx-auto" />
        <p className="text-sm text-muted-foreground mt-3">Loading exam…</p>
      </div>
    );
  }

  if (result) {
    const pct = result.max > 0 ? Math.round((result.score / result.max) * 100) : 0;
    const passed = pct >= (fullTest?.passScore || test.passScore || 40);
    return (
      <div className="max-w-lg mx-auto text-center py-8">
        <div className={`mx-auto h-24 w-24 rounded-full grid place-items-center mb-4 ${passed ? "bg-emerald-50" : "bg-rose-50"}`}>
          {passed ? <CheckCircle2 className="h-12 w-12 text-emerald-600" /> : <XCircle className="h-12 w-12 text-rose-600" />}
        </div>
        <h2 className="text-2xl font-bold">{passed ? "Passed!" : "Try again"}</h2>
        <p className="text-muted-foreground mt-1">You scored</p>
        <div className="text-5xl font-bold my-2">{pct}%</div>
        <p className="text-sm text-muted-foreground">{result.score} / {result.max} points</p>
        {!result.graded && <p className="text-xs text-muted-foreground mt-2">Subjective answers will be graded by your teacher.</p>}
        <Button className="mt-6" onClick={onExit}>Back to exams</Button>
      </div>
    );
  }

  const item = items[current];
  const q = item?.question;
  if (!q) return <p className="text-muted-foreground">This exam has no questions.</p>;

  const answeredCount = Object.keys(answers).length;
  const progress = Math.round((answeredCount / items.length) * 100);

  return (
    <div className="max-w-3xl mx-auto space-y-3">
      {/* Top bar */}
      <div className="flex items-center justify-between gap-2">
        <Button variant="ghost" size="sm" onClick={() => { if (confirm("Exit exam? Your progress will be lost.")) onExit(); }}>
          <ArrowLeft className="mr-1 h-4 w-4" /> Exit
        </Button>
        <div className={`flex items-center gap-2 text-sm font-mono px-3 py-1 rounded-full ${isLowTime ? "bg-rose-100 text-rose-700 animate-pulse" : "bg-amber-50 text-amber-700"}`}>
          <Clock className="h-3 w-3" /> {mm}:{ss}
        </div>
        <Button variant="ghost" size="sm" onClick={() => setShowNav(true)}>
          <Grid3x3 className="h-4 w-4" />
        </Button>
      </div>

      {/* Progress bar */}
      <div>
        <div className="flex justify-between text-xs text-muted-foreground mb-1">
          <span>Question {current + 1} of {items.length}</span>
          <span>{answeredCount} answered · {marked.size} marked</span>
        </div>
        <Progress value={progress} className="h-1.5" />
      </div>

      {/* Question card */}
      <Card>
        <CardContent className="p-5">
          <div className="flex items-center justify-between mb-3">
            <div className="text-xs text-muted-foreground uppercase tracking-wide">
              {q.type.replace(/_/g, " ")} · {q.difficulty.toLowerCase()} · {item.points} pts
            </div>
            <Button size="sm" variant={marked.has(q.id) ? "default" : "ghost"} onClick={() => toggleMark(q.id)}>
              <Flag className="h-3 w-3 mr-1" fill={marked.has(q.id) ? "currentColor" : "none"} />
              {marked.has(q.id) ? "Marked" : "Mark"}
            </Button>
          </div>

          <p className="font-medium mb-4 whitespace-pre-wrap text-base">{q.stem}</p>

          {/* Image */}
          {q.imageUrl && (
            <div className="mb-4 rounded-lg overflow-hidden border">
              <img src={q.imageUrl} alt="Question" className="max-w-full max-h-80 mx-auto" />
            </div>
          )}

          {/* Audio */}
          {q.audioUrl && (
            <div className="mb-4">
              <audio controls className="w-full">
                <source src={q.audioUrl} />
              </audio>
            </div>
          )}

          {/* Answer inputs */}
          {q.type === "SINGLE_CHOICE" || q.type === "TRUE_FALSE" ? (
            <RadioGroup value={(answers[q.id] as string) || ""} onValueChange={(v) => setAns(q.id, v)} className="space-y-2">
              {(q.options || []).map((opt, i) => (
                <label key={i} className="flex items-center gap-3 p-3 rounded-lg border cursor-pointer hover:bg-muted/40 transition">
                  <RadioGroupItem value={opt} id={`${q.id}-${i}`} />
                  <span className="text-sm">{opt}</span>
                </label>
              ))}
            </RadioGroup>
          ) : q.type === "MULTIPLE_CHOICE" ? (
            <div className="space-y-2">
              {(q.options || []).map((opt, i) => {
                const sel = ((answers[q.id] as string[]) || []).includes(opt);
                return (
                  <label key={i} className="flex items-center gap-3 p-3 rounded-lg border cursor-pointer hover:bg-muted/40 transition">
                    <Checkbox checked={sel} onCheckedChange={(c) => {
                      const arr = (answers[q.id] as string[]) || [];
                      setAns(q.id, c ? [...arr, opt] : arr.filter(x => x !== opt));
                    }} />
                    <span className="text-sm">{opt}</span>
                  </label>
                );
              })}
            </div>
          ) : q.type === "ONE_WORD" || q.type === "FILL_BLANK" ? (
            <Input placeholder="Type your answer…" value={(answers[q.id] as string) || ""} onChange={(e) => setAns(q.id, e.target.value)} />
          ) : (
            <Textarea placeholder="Write your answer…" rows={5} value={(answers[q.id] as string) || ""} onChange={(e) => setAns(q.id, e.target.value)} />
          )}
        </CardContent>
      </Card>

      {/* Navigation */}
      <div className="flex items-center justify-between gap-2">
        <Button variant="outline" disabled={current === 0} onClick={() => setCurrent(c => c - 1)}>
          <ChevronLeft className="mr-1 h-4 w-4" /> Previous
        </Button>

        <div className="flex gap-1">
          {/* Quick jump dots */}
          {items.slice(0, 10).map((it, i) => (
            <button
              key={i}
              onClick={() => setCurrent(i)}
              className={`h-2 w-2 rounded-full transition ${
                i === current ? "bg-primary scale-125" :
                answers[it.questionId] ? "bg-emerald-400" :
                marked.has(it.questionId) ? "bg-amber-400" : "bg-muted"
              }`}
            />
          ))}
          {items.length > 10 && <span className="text-xs text-muted-foreground ml-1">+{items.length - 10}</span>}
        </div>

        {current < items.length - 1 ? (
          <div className="flex gap-2">
            <Button variant="ghost" onClick={() => setCurrent(c => c + 1)}>Skip</Button>
            <Button onClick={() => setCurrent(c => c + 1)}>Next <ChevronRight className="ml-1 h-4 w-4" /></Button>
          </div>
        ) : (
          <Button onClick={() => setShowConfirm(true)} disabled={submitting}>
            <Send className="mr-1 h-4 w-4" /> Submit
          </Button>
        )}
      </div>

      {/* Question navigation grid modal */}
      <Dialog open={showNav} onOpenChange={setShowNav}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Question Navigator</DialogTitle>
            <DialogDescription>Jump to any question. Green = answered, Amber = marked, Gray = unanswered.</DialogDescription>
          </DialogHeader>
          <div className="grid grid-cols-6 gap-2">
            {items.map((it, i) => (
              <button
                key={i}
                onClick={() => { setCurrent(i); setShowNav(false); }}
                className={`h-10 rounded-lg font-medium text-sm transition ${
                  i === current ? "bg-primary text-white" :
                  answers[it.questionId] ? "bg-emerald-100 text-emerald-700 hover:bg-emerald-200" :
                  marked.has(it.questionId) ? "bg-amber-100 text-amber-700 hover:bg-amber-200" :
                  "bg-muted hover:bg-muted/80"
                }`}
              >
                {i + 1}
              </button>
            ))}
          </div>
          <div className="flex items-center justify-between text-xs text-muted-foreground pt-2">
            <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-emerald-400" /> Answered ({answeredCount})</span>
            <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-amber-400" /> Marked ({marked.size})</span>
            <span className="flex items-center gap-1"><span className="h-2 w-2 rounded-full bg-muted-foreground" /> Unanswered ({items.length - answeredCount})</span>
          </div>
        </DialogContent>
      </Dialog>

      {/* Submit confirmation */}
      <Dialog open={showConfirm} onOpenChange={setShowConfirm}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Submit exam?</DialogTitle>
            <DialogDescription>
              You have answered {answeredCount} of {items.length} questions.
              {answeredCount < items.length && ` ${items.length - answeredCount} questions are unanswered.`}
              This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <div className="flex gap-2">
            <Button variant="outline" className="flex-1" onClick={() => setShowConfirm(false)}>Cancel</Button>
            <Button className="flex-1" onClick={() => submit(false)} disabled={submitting}>
              {submitting ? "Submitting…" : "Submit Now"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
