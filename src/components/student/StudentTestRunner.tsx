"use client";

import { useEffect, useState, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { ArrowLeft, ArrowRight, Send, Clock, CheckCircle2, XCircle } from "lucide-react";
import type { Test } from "@/types";
import { toast } from "sonner";

type Answers = Record<string, string | string[]>;

export function StudentTestRunner({ test, onExit }: { test: Test; onExit: () => void }) {
  const [fullTest, setFullTest] = useState<Test | null>(null);
  const [loading, setLoading] = useState(true);
  const items = fullTest?.items ?? [];
  const [current, setCurrent] = useState(0);
  const [answers, setAnswers] = useState<Answers>({});
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<{ score: number; max: number; graded: boolean } | null>(null);
  const [secondsLeft, setSecondsLeft] = useState(test.durationMin * 60);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Fetch full test (with questions) when the runner mounts
  useEffect(() => {
    let cancelled = false;
    fetch(`/api/student/tests/${test.id}`)
      .then((r) => r.json())
      .then((d) => {
        if (cancelled) return;
        if (d.test) setFullTest(d.test);
        setLoading(false);
      })
      .catch(() => setLoading(false));
    return () => { cancelled = true; };
  }, [test.id]);

  useEffect(() => {
    if (!fullTest) return;
    timerRef.current = setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) {
          if (timerRef.current) clearInterval(timerRef.current);
          submit(true);
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, []);

  const mm = Math.floor(secondsLeft / 60).toString().padStart(2, "0");
  const ss = (secondsLeft % 60).toString().padStart(2, "0");

  function setAns(qId: string, val: string | string[]) {
    setAnswers((a) => ({ ...a, [qId]: val }));
  }

  async function submit(auto = false) {
    if (timerRef.current) clearInterval(timerRef.current);
    setSubmitting(true);
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
      toast[auto ? "info" : "success"](auto ? "Time's up — auto-submitted" : "Test submitted!");
    } finally {
      setSubmitting(false);
    }
  }

  if (result) {
    const pct = result.max > 0 ? Math.round((result.score / result.max) * 100) : 0;
    const passed = pct >= test.passScore;
    return (
      <div className="max-w-lg mx-auto text-center py-8">
        <div className={`mx-auto h-24 w-24 rounded-full grid place-items-center mb-4 ${passed ? "bg-emerald-50" : "bg-rose-50"}`}>
          {passed ? <CheckCircle2 className="h-12 w-12 text-emerald-600" /> : <XCircle className="h-12 w-12 text-rose-600" />}
        </div>
        <h2 className="text-2xl font-bold">{passed ? "Passed!" : "Try again"}</h2>
        <p className="text-muted-foreground mt-1">You scored</p>
        <div className="text-5xl font-bold my-2">{pct}%</div>
        <p className="text-sm text-muted-foreground">{result.score} / {result.max} points · Pass mark: {test.passScore}%</p>
        <Button className="mt-6" onClick={onExit}>Back to tests</Button>
      </div>
    );
  }

  const item = items[current];
  const q = item?.question;
  if (loading) {
    return (
      <div className="max-w-2xl mx-auto text-center py-12">
        <div className="h-8 w-8 rounded-full border-2 border-primary border-t-transparent animate-spin mx-auto" />
        <p className="text-sm text-muted-foreground mt-3">Loading test…</p>
      </div>
    );
  }
  if (!q) return <p className="text-muted-foreground">This test has no questions.</p>;

  const answered = Object.keys(answers).length;
  const progress = Math.round((answered / items.length) * 100);

  return (
    <div className="max-w-2xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" onClick={onExit}>
          <ArrowLeft className="mr-2 h-4 w-4" /> Exit
        </Button>
        <div className="flex items-center gap-2 text-sm font-mono bg-amber-50 text-amber-700 px-3 py-1 rounded-full">
          <Clock className="h-3 w-3" /> {mm}:{ss}
        </div>
      </div>

      <div>
        <div className="flex justify-between text-xs text-muted-foreground mb-1">
          <span>Question {current + 1} of {items.length}</span>
          <span>{answered} answered</span>
        </div>
        <Progress value={progress} className="h-1.5" />
      </div>

      <Card>
        <CardContent className="p-5">
          <div className="text-xs text-muted-foreground mb-2 uppercase tracking-wide">
            {q.type.replace(/_/g, " ")} · {q.difficulty.toLowerCase()} · {item.points} pts
          </div>
          <p className="font-medium mb-4 whitespace-pre-wrap">{q.stem}</p>

          {/* Render input based on type */}
          {q.type === "SINGLE_CHOICE" || q.type === "TRUE_FALSE" ? (
            <RadioGroup
              value={(answers[q.id] as string) || ""}
              onValueChange={(v) => setAns(q.id, v)}
              className="space-y-2"
            >
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
                    <Checkbox
                      checked={sel}
                      onCheckedChange={(c) => {
                        const arr = (answers[q.id] as string[]) || [];
                        setAns(q.id, c ? [...arr, opt] : arr.filter((x) => x !== opt));
                      }}
                    />
                    <span className="text-sm">{opt}</span>
                  </label>
                );
              })}
            </div>
          ) : q.type === "ONE_WORD" ? (
            <Input
              placeholder="Type your answer…"
              value={(answers[q.id] as string) || ""}
              onChange={(e) => setAns(q.id, e.target.value)}
            />
          ) : q.type === "FILL_BLANK" ? (
            <Input
              placeholder="Fill in the blank…"
              value={(answers[q.id] as string) || ""}
              onChange={(e) => setAns(q.id, e.target.value)}
            />
          ) : (
            <Textarea
              placeholder="Write your answer…"
              rows={5}
              value={(answers[q.id] as string) || ""}
              onChange={(e) => setAns(q.id, e.target.value)}
            />
          )}
        </CardContent>
      </Card>

      <div className="flex items-center justify-between gap-2">
        <Button variant="outline" disabled={current === 0} onClick={() => setCurrent((c) => c - 1)}>
          <ArrowLeft className="mr-2 h-4 w-4" /> Previous
        </Button>
        {current < items.length - 1 ? (
          <Button onClick={() => setCurrent((c) => c + 1)}>
            Next <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        ) : (
          <Button onClick={() => submit(false)} disabled={submitting}>
            <Send className="mr-2 h-4 w-4" /> {submitting ? "Submitting…" : "Submit test"}
          </Button>
        )}
      </div>
    </div>
  );
}
