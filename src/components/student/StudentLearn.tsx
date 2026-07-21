"use client";

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ChevronRight, BookOpen, PlayCircle, FileText, File } from "lucide-react";
import type { Subject, Chapter, Lesson } from "@/types";
import { StudentLessonView } from "./StudentLessonView";

export function StudentLearn() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [activeSubject, setActiveSubject] = useState<Subject | null>(null);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [activeChapter, setActiveChapter] = useState<Chapter | null>(null);
  const [activeLesson, setActiveLesson] = useState<Lesson | null>(null);

  useEffect(() => {
    fetch("/api/student/subjects")
      .then((r) => r.json())
      .then((d) => setSubjects(d.subjects || []))
      .catch(() => null);
  }, []);

  function openSubject(s: Subject) {
    setActiveSubject(s);
    fetch(`/api/student/subjects/${s.id}/chapters`)
      .then((r) => r.json())
      .then((d) => setChapters(d.chapters || []));
  }

  function openChapter(c: Chapter) {
    setActiveChapter(c);
    if (!c.lessons) {
      fetch(`/api/student/chapters/${c.id}/lessons`)
        .then((r) => r.json())
        .then((d) => {
          const updated = chapters.map((x) => (x.id === c.id ? { ...x, lessons: d.lessons } : x));
          setChapters(updated);
          setActiveChapter({ ...c, lessons: d.lessons });
        });
    }
  }

  if (activeLesson) {
    return (
      <StudentLessonView
        lesson={activeLesson}
        onBack={() => setActiveLesson(null)}
      />
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">
          {activeChapter ? activeChapter.title : activeSubject ? activeSubject.name : "Learn"}
        </h2>
        {(activeSubject || activeChapter) && (
          <Button variant="ghost" size="sm" onClick={() => {
            if (activeChapter) { setActiveChapter(null); }
            else if (activeSubject) { setActiveSubject(null); setChapters([]); }
          }}>
            Back
          </Button>
        )}
      </div>

      {/* Subjects */}
      {!activeSubject && (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {subjects.length === 0 ? (
            <p className="text-sm text-muted-foreground col-span-full">No subjects available yet. Check back soon.</p>
          ) : subjects.map((s) => (
            <Card key={s.id} className="hover:shadow-md transition cursor-pointer" onClick={() => openSubject(s)}>
              <CardContent className="p-5 flex items-center gap-4">
                <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-emerald-400 to-teal-500 grid place-items-center text-white shrink-0">
                  <BookOpen className="h-6 w-6" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="font-semibold truncate">{s.name}</div>
                  <div className="text-xs text-muted-foreground line-clamp-2">{s.description}</div>
                </div>
                <ChevronRight className="h-5 w-5 text-muted-foreground" />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Chapters */}
      {activeSubject && !activeChapter && (
        <div className="space-y-2">
          {chapters.length === 0 ? (
            <p className="text-sm text-muted-foreground">No chapters published yet.</p>
          ) : chapters.map((c, i) => (
            <Card key={c.id} className="hover:shadow-md transition cursor-pointer" onClick={() => openChapter(c)}>
              <CardContent className="p-4 flex items-center gap-3">
                <div className="h-8 w-8 rounded-lg bg-muted text-muted-foreground grid place-items-center text-sm font-semibold">
                  {i + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="font-medium truncate">{c.title}</div>
                  <div className="text-xs text-muted-foreground line-clamp-1">{c.description}</div>
                </div>
                <ChevronRight className="h-5 w-5 text-muted-foreground" />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Lessons */}
      {activeChapter && (
        <div className="space-y-2">
          {activeChapter.lessons?.length === 0 && (
            <p className="text-sm text-muted-foreground">No lessons in this chapter yet.</p>
          )}
          {activeChapter.lessons?.map((l, i) => {
            const Icon = l.type === "VIDEO" ? PlayCircle : l.type === "PDF" ? File : FileText;
            return (
              <Card key={l.id} className="hover:shadow-md transition cursor-pointer" onClick={() => setActiveLesson(l)}>
                <CardContent className="p-4 flex items-center gap-3">
                  <div className="h-8 w-8 rounded-lg bg-emerald-50 text-emerald-600 grid place-items-center">
                    <Icon className="h-4 w-4" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-medium truncate">{i + 1}. {l.title}</div>
                    <div className="text-xs text-muted-foreground">{l.durationMin} min · {l.type.toLowerCase()}</div>
                  </div>
                  <ChevronRight className="h-5 w-5 text-muted-foreground" />
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
