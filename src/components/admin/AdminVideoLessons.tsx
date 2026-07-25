"use client";

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Video, Plus, Trash2, Pencil, Play, Eye, Youtube } from "lucide-react";
import { toast } from "sonner";

interface VideoLesson {
  id: string;
  title: string;
  slug: string;
  description?: string;
  youtubeUrl: string;
  youtubeId: string;
  thumbnailUrl?: string;
  durationMin: number;
  level?: string;
  category?: string;
  isPublished: boolean;
  views: number;
}

export function AdminVideoLessons() {
  const [videos, setVideos] = useState<VideoLesson[]>([]);
  const [open, setOpen] = useState(false);
  const [preview, setPreview] = useState<VideoLesson | null>(null);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState<any>({
    title: "", description: "", youtubeUrl: "", videoUrl: "", videoSource: "youtube", durationMin: 10,
    level: "Beginner", category: "Grammar", isPublished: true,
  });

  function load() {
    fetch("/api/admin/video-lessons").then(r => r.json()).then(d => setVideos(d.videos || [])).catch(() => {});
  }
  useEffect(load, []);

  async function save() {
    if (!form.title.trim()) { toast.error("Title required"); return; }
    const isUpload = form.youtubeUrl === "UPLOAD";
    if (!isUpload && !form.youtubeUrl.trim()) { toast.error("YouTube URL or upload a video required"); return; }
    if (isUpload && !form.videoUrl) { toast.error("Please upload a video file"); return; }
    setBusy(true);
    const slug = form.title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    const payload = {
      title: form.title,
      slug,
      description: form.description,
      youtubeUrl: isUpload ? "" : form.youtubeUrl,
      videoUrl: isUpload ? form.videoUrl : "",
      videoSource: isUpload ? "upload" : "youtube",
      durationMin: Number(form.durationMin),
      level: form.level,
      category: form.category,
      isPublished: form.isPublished,
    };
    const res = await fetch("/api/admin/video-lessons", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Video lesson created");
    setOpen(false);
    setForm({ title: "", description: "", youtubeUrl: "", videoUrl: "", videoSource: "youtube", durationMin: 10, level: "Beginner", category: "Grammar", isPublished: true });
    load();
  }

  async function del(id: string) {
    if (!confirm("Delete this video lesson?")) return;
    await fetch(`/api/admin/video-lessons/${id}`, { method: "DELETE" });
    toast.success("Deleted");
    load();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Video Lessons ({videos.length})</h1>
          <p className="text-sm text-muted-foreground">Add YouTube video links — students watch in-app, no redirects.</p>
        </div>
        <Button onClick={() => setOpen(true)}><Plus className="mr-1 h-4 w-4" /> Add Video</Button>
      </div>

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
        {videos.length === 0 ? (
          <Card className="col-span-full"><CardContent className="p-8 text-center text-muted-foreground">
            <Video className="h-10 w-10 mx-auto mb-2 opacity-40" />
            <p>No video lessons yet. Add a YouTube link to get started.</p>
          </CardContent></Card>
        ) : videos.map(v => (
          <Card key={v.id} className="hover:shadow-md transition overflow-hidden">
            <div className="relative cursor-pointer" onClick={() => setPreview(v)}>
              {v.thumbnailUrl ? (
                <img src={v.thumbnailUrl} alt={v.title} className="w-full h-32 object-cover" />
              ) : (
                <div className="w-full h-32 bg-slate-200 grid place-items-center"><Youtube className="h-8 w-8 text-slate-400" /></div>
              )}
              <div className="absolute inset-0 bg-black/30 grid place-items-center opacity-0 hover:opacity-100 transition">
                <Play className="h-8 w-8 text-white" fill="white" />
              </div>
            </div>
            <CardContent className="p-3">
              <div className="font-semibold text-sm truncate">{v.title}</div>
              <div className="flex items-center gap-2 mt-1">
                {v.level && <Badge variant="secondary" className="text-xs">{v.level}</Badge>}
                {v.category && <Badge variant="outline" className="text-xs">{v.category}</Badge>}
              </div>
              <div className="flex items-center justify-between mt-2 text-xs text-muted-foreground">
                <span>{v.durationMin} min</span>
                <span><Eye className="inline h-3 w-3" /> {v.views}</span>
              </div>
              <div className="flex gap-1 mt-2">
                <Button size="sm" variant="ghost" onClick={() => setPreview(v)}><Play className="h-3 w-3" /></Button>
                <Button size="sm" variant="ghost" onClick={() => del(v.id)}><Trash2 className="h-3 w-3 text-rose-500" /></Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Add dialog */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader><DialogTitle>Add Video Lesson</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <div><Label>Title</Label><Input value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder="Korean Grammar — Present Tense" /></div>
            <div>
              <Label>Video Source</Label>
              <div className="flex gap-2 mb-2">
                <Button type="button" variant={form.youtubeUrl ? "default" : "outline"} size="sm" onClick={() => setForm(f => ({ ...f, youtubeUrl: "" }))}>YouTube URL</Button>
                <Button type="button" variant={!form.youtubeUrl ? "default" : "outline"} size="sm" onClick={() => setForm(f => ({ ...f, youtubeUrl: "UPLOAD" }))}>Upload from Device</Button>
              </div>
              {form.youtubeUrl === "UPLOAD" ? (
                <div>
                  <label className="cursor-pointer">
                    <input
                      type="file"
                      accept="video/*"
                      className="hidden"
                      onChange={async (e) => {
                        const f = e.target.files?.[0];
                        if (!f) return;
                        setBusy(true);
                        try {
                          const fd = new FormData();
                          fd.append("file", f);
                          fd.append("folder", "videos");
                          const res = await fetch("/api/admin/upload", { method: "POST", body: fd });
                          if (!res.ok) { const d = await res.json(); toast.error(d.error || "Upload failed"); return; }
                          const d = await res.json();
                          setForm(prev => ({ ...prev, youtubeUrl: d.url, videoSource: "upload", videoUrl: d.url }));
                          toast.success("Video uploaded");
                        } catch { toast.error("Upload failed"); }
                        finally { setBusy(false); }
                      }}
                    />
                    <span className="inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium h-9 px-3 bg-primary text-primary-foreground cursor-pointer hover:bg-primary/90">
                      📁 Choose Video File
                    </span>
                  </label>
                  {form.videoUrl && <p className="mt-1 text-xs text-green-600">✓ Video file ready</p>}
                </div>
              ) : (
                <Input value={form.youtubeUrl} onChange={e => setForm(f => ({ ...f, youtubeUrl: e.target.value }))} placeholder="https://youtube.com/watch?v=..." />
              )}
            </div>
            <div><Label>Description</Label><Textarea rows={2} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} /></div>
            <div className="grid grid-cols-3 gap-3">
              <div><Label>Duration (min)</Label><Input type="number" value={form.durationMin} onChange={e => setForm(f => ({ ...f, durationMin: Number(e.target.value) }))} /></div>
              <div><Label>Level</Label>
                <Select value={form.level} onValueChange={v => setForm(f => ({ ...f, level: v }))}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Beginner">Beginner</SelectItem>
                    <SelectItem value="Intermediate">Intermediate</SelectItem>
                    <SelectItem value="Advanced">Advanced</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div><Label>Category</Label>
                <Select value={form.category} onValueChange={v => setForm(f => ({ ...f, category: v }))}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Grammar">Grammar</SelectItem>
                    <SelectItem value="Conversation">Conversation</SelectItem>
                    <SelectItem value="TOPIK Prep">TOPIK Prep</SelectItem>
                    <SelectItem value="Pronunciation">Pronunciation</SelectItem>
                    <SelectItem value="Vocabulary">Vocabulary</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <Button onClick={save} disabled={busy} className="w-full">{busy ? "Saving…" : "Create Video Lesson"}</Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Preview modal — plays YouTube embed */}
      {preview && (
        <Dialog open={!!preview} onOpenChange={() => setPreview(null)}>
          <DialogContent className="max-w-3xl">
            <DialogHeader><DialogTitle>{preview.title}</DialogTitle></DialogHeader>
            <div className="aspect-video rounded-lg overflow-hidden">
              <iframe
                src={`https://www.youtube.com/embed/${preview.youtubeId}?rel=0`}
                title={preview.title}
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                className="w-full h-full"
              />
            </div>
            {preview.description && <p className="text-sm text-muted-foreground">{preview.description}</p>}
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}
