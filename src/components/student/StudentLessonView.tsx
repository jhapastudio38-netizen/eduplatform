"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ArrowLeft, CheckCircle2, Video, FileText } from "lucide-react";
import type { Lesson } from "@/types";
import { Progress } from "@/components/ui/progress";
import { toast } from "sonner";
import ReactMarkdown from "react-markdown";

export function StudentLessonView({ lesson, onBack }: { lesson: Lesson; onBack: () => void }) {
  const [percent, setPercent] = useState(0);

  // Restore saved progress
  useEffect(() => {
    fetch(`/api/student/lessons/${lesson.id}/progress`)
      .then((r) => r.json())
      .then((d) => d.percent && setPercent(d.percent))
      .catch(() => null);
  }, [lesson.id]);

  async function markComplete() {
    setPercent(100);
    await fetch(`/api/student/lessons/${lesson.id}/progress`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ percent: 100, completed: true }),
    });
    toast.success("Lesson completed! +10 XP");
  }

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" onClick={onBack}>
        <ArrowLeft className="mr-2 h-4 w-4" /> Back to chapter
      </Button>

      <div>
        <h2 className="text-xl font-bold">{lesson.title}</h2>
        <div className="text-xs text-muted-foreground mt-1">{lesson.durationMin} min · {lesson.type}</div>
      </div>

      <Progress value={percent} className="h-1.5" />

      <Card>
        <CardContent className="p-5">
          {lesson.type === "VIDEO" && lesson.videoUrl ? (
            <div className="aspect-video rounded-lg overflow-hidden bg-black">
              <video src={lesson.videoUrl} controls className="w-full h-full" />
            </div>
          ) : lesson.type === "PDF" ? (
            <div className="flex flex-col items-center justify-center py-10 text-center">
              <FileText className="h-12 w-12 text-muted-foreground mb-3" />
              <p className="text-sm text-muted-foreground mb-3">PDF preview</p>
              {lesson.videoUrl && (
                <a href={lesson.videoUrl} target="_blank" rel="noreferrer">
                  <Button variant="outline" size="sm">Open PDF</Button>
                </a>
              )}
            </div>
          ) : (
            <article className="prose prose-sm max-w-none dark:prose-invert">
              <ReactMarkdown>{lesson.content || "No content for this lesson yet."}</ReactMarkdown>
            </article>
          )}
        </CardContent>
      </Card>

      <div className="flex items-center justify-between gap-2 sticky bottom-20 bg-background/95 backdrop-blur p-3 rounded-xl border">
        <div className="text-sm text-muted-foreground">
          {percent === 100 ? (
            <span className="flex items-center text-emerald-600"><CheckCircle2 className="h-4 w-4 mr-1" /> Completed</span>
          ) : (
            <span>{percent}% complete</span>
          )}
        </div>
        <Button onClick={markComplete} disabled={percent === 100}>
          {percent === 100 ? "Completed" : "Mark as complete"}
        </Button>
      </div>
    </div>
  );
}
