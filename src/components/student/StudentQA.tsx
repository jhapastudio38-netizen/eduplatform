"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Plus, MessageSquare, ChevronRight } from "lucide-react";
import type { QAQuestion } from "@/types";
import { toast } from "sonner";
import { formatDistanceToNow } from "date-fns";

export function StudentQA() {
  const [questions, setQuestions] = useState<QAQuestion[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [tags, setTags] = useState("");
  const [busy, setBusy] = useState(false);
  const [active, setActive] = useState<QAQuestion | null>(null);

  function load() {
    fetch("/api/student/qa")
      .then((r) => r.json())
      .then((d) => setQuestions(d.questions || []))
      .catch(() => null);
  }
  useEffect(load, []);

  async function submit() {
    if (!title.trim() || !body.trim()) {
      toast.error("Title and body are required");
      return;
    }
    setBusy(true);
    try {
      const res = await fetch("/api/student/qa", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title,
          body,
          tags: tags.split(",").map((t) => t.trim()).filter(Boolean),
        }),
      });
      const d = await res.json();
      if (!res.ok) {
        toast.error(d.error || "Failed to post");
        return;
      }
      toast.success("Question posted!");
      setTitle(""); setBody(""); setTags("");
      setShowForm(false);
      load();
    } finally {
      setBusy(false);
    }
  }

  if (active) {
    return <QAADetail question={active} onBack={() => { setActive(null); load(); }} />;
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">Questions & Answers</h2>
        <Button size="sm" onClick={() => setShowForm((s) => !s)}>
          <Plus className="mr-1 h-4 w-4" /> Ask
        </Button>
      </div>

      {showForm && (
        <Card>
          <CardContent className="p-4 space-y-3">
            <div>
              <Label htmlFor="t">Title</Label>
              <Input id="t" placeholder="What's your question?" value={title} onChange={(e) => setTitle(e.target.value)} />
            </div>
            <div>
              <Label htmlFor="b">Details</Label>
              <Textarea id="b" rows={4} placeholder="Add more context…" value={body} onChange={(e) => setBody(e.target.value)} />
            </div>
            <div>
              <Label htmlFor="tags">Tags (comma-separated)</Label>
              <Input id="tags" placeholder="algebra, equations" value={tags} onChange={(e) => setTags(e.target.value)} />
            </div>
            <div className="flex gap-2">
              <Button onClick={submit} disabled={busy}>{busy ? "Posting…" : "Post question"}</Button>
              <Button variant="outline" onClick={() => setShowForm(false)}>Cancel</Button>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="space-y-2">
        {questions.length === 0 ? (
          <Card><CardContent className="p-6 text-center text-sm text-muted-foreground">
            <MessageSquare className="h-8 w-8 mx-auto mb-2 opacity-40" />
            No questions yet. Be the first to ask!
          </CardContent></Card>
        ) : questions.map((q) => (
          <Card key={q.id} className="hover:shadow-md transition cursor-pointer" onClick={() => setActive(q)}>
            <CardContent className="p-4 flex items-start gap-3">
              <div className="h-9 w-9 rounded-lg bg-rose-50 text-rose-600 grid place-items-center shrink-0">
                <MessageSquare className="h-4 w-4" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="font-medium line-clamp-1">{q.title}</div>
                <div className="text-xs text-muted-foreground line-clamp-2 mt-0.5">{q.body}</div>
                <div className="text-xs text-muted-foreground mt-1">
                  {q.answers?.length || 0} answer(s) · {formatDistanceToNow(new Date(q.createdAt), { addSuffix: true })}
                </div>
              </div>
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

function QAADetail({ question, onBack }: { question: QAQuestion; onBack: () => void }) {
  const [answer, setAnswer] = useState("");
  const [busy, setBusy] = useState(false);
  const [answers, setAnswers] = useState(question.answers || []);

  async function postAnswer() {
    if (!answer.trim()) return;
    setBusy(true);
    try {
      const res = await fetch(`/api/student/qa/${question.id}/answers`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ body: answer }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Failed"); return; }
      setAnswers((a) => [...a, d.answer]);
      setAnswer("");
      toast.success("Answer posted");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-4 max-w-2xl mx-auto">
      <Button variant="ghost" size="sm" onClick={onBack}>← Back to questions</Button>
      <Card>
        <CardContent className="p-5">
          <h2 className="text-lg font-bold">{question.title}</h2>
          <p className="text-sm text-muted-foreground mt-2 whitespace-pre-wrap">{question.body}</p>
          {question.tags?.length ? (
            <div className="flex flex-wrap gap-1 mt-3">
              {question.tags.map((t) => <span key={t} className="text-xs bg-muted px-2 py-0.5 rounded">#{t}</span>)}
            </div>
          ) : null}
        </CardContent>
      </Card>

      <div>
        <h3 className="font-semibold mb-2">{answers.length} Answer(s)</h3>
        <div className="space-y-2">
          {answers.map((a) => (
            <Card key={a.id}><CardContent className="p-4">
              <p className="text-sm whitespace-pre-wrap">{a.body}</p>
              <div className="text-xs text-muted-foreground mt-2">
                {formatDistanceToNow(new Date(a.createdAt), { addSuffix: true })}
              </div>
            </CardContent></Card>
          ))}
        </div>
      </div>

      <Card>
        <CardContent className="p-4 space-y-2">
          <Label>Your answer</Label>
          <Textarea rows={4} value={answer} onChange={(e) => setAnswer(e.target.value)} placeholder="Share your knowledge…" />
          <Button onClick={postAnswer} disabled={busy}>{busy ? "Posting…" : "Post answer"}</Button>
        </CardContent>
      </Card>
    </div>
  );
}
