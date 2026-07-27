"use client";

import { useEffect, useState, useRef } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Eye, Plus, Trash2, Upload, Loader2, X } from "lucide-react";
import { toast } from "sonner";

interface EyeVisionTest {
  id: string;
  title: string;
  description?: string | null;
  imageUrl: string;
  correctAnswer: string;
  category?: string | null;
  sortOrder: number;
  isPublished: boolean;
  createdAt: string;
}

export function AdminEyeVision() {
  const [tests, setTests] = useState<EyeVisionTest[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);

  function load() {
    setLoading(true);
    fetch("/api/admin/eye-vision")
      .then((r) => r.json())
      .then((d) => setTests(d.tests || []))
      .catch(() => setTests([]))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function deleteTest(t: EyeVisionTest) {
    if (!confirm(`Delete "${t.title}"?`)) return;
    try {
      const res = await fetch(`/api/admin/eye-vision/${t.id}`, { method: "DELETE" });
      if (!res.ok) {
        const d = await res.json();
        toast.error(d.error || "Delete failed");
        return;
      }
      toast.success("Eye vision test deleted");
      load();
    } catch {
      toast.error("Delete failed");
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Eye Vision Tests ({tests.length})</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Upload an image and set the correct answer. Students see the image and type what they see — matching is case-insensitive.
          </p>
        </div>
        <Button size="lg" onClick={() => setCreateOpen(true)}>
          <Plus className="w-4 h-4 mr-1" /> New Eye Vision Test
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : tests.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Eye className="w-12 h-12 mx-auto mb-3 text-muted-foreground opacity-40" />
            <p className="text-muted-foreground">No eye vision tests yet. Click &ldquo;New Eye Vision Test&rdquo; to create one.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {tests.map((t) => (
            <Card key={t.id} className="overflow-hidden hover:shadow-md transition-shadow">
              <div className="aspect-video bg-slate-100 flex items-center justify-center overflow-hidden">
                {t.imageUrl ? (
                  <img
                    src={t.imageUrl}
                    alt={t.title}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <Eye className="w-10 h-10 text-slate-400" />
                )}
              </div>
              <CardContent className="p-4 space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-tight">{t.title}</h3>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="shrink-0 text-rose-500 hover:text-rose-600"
                    onClick={() => deleteTest(t)}
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
                {t.description && (
                  <p className="text-xs text-muted-foreground line-clamp-2">{t.description}</p>
                )}
                <div className="flex flex-wrap items-center gap-2 pt-1">
                  {t.category && <Badge variant="secondary">{t.category}</Badge>}
                  <Badge variant="outline" className="text-emerald-700 border-emerald-300 bg-emerald-50">
                    Answer: {t.correctAnswer}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {createOpen && (
        <CreateEyeVisionDialog
          open={createOpen}
          onOpenChange={setCreateOpen}
          onCreated={() => { setCreateOpen(false); load(); }}
        />
      )}
    </div>
  );
}

function CreateEyeVisionDialog({ open, onOpenChange, onCreated }: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onCreated: () => void;
}) {
  const [form, setForm] = useState({
    title: "",
    description: "",
    imageUrl: "",
    correctAnswer: "",
    category: "",
  });
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  async function uploadFile(file: File): Promise<string> {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("folder", "eye-vision");
    const res = await fetch("/api/admin/file-upload", { method: "POST", body: fd });
    const d = await res.json();
    if (!res.ok || !d.url) {
      throw new Error(d.error || "Upload failed");
    }
    return d.url;
  }

  async function onPickFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (!f) return;
    setUploading(true);
    try {
      const url = await uploadFile(f);
      setForm((p) => ({ ...p, imageUrl: url }));
      toast.success("Image uploaded");
    } catch (err: any) {
      toast.error(err.message || "Upload failed");
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = "";
    }
  }

  async function create() {
    if (!form.title.trim()) { toast.error("Title is required"); return; }
    if (!form.imageUrl) { toast.error("Please upload an image"); return; }
    if (!form.correctAnswer.trim()) { toast.error("Correct answer is required"); return; }
    setBusy(true);
    try {
      const res = await fetch("/api/admin/eye-vision", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          description: form.description.trim(),
          imageUrl: form.imageUrl,
          correctAnswer: form.correctAnswer.trim(),
          category: form.category.trim(),
          isPublished: true,
        }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Failed to create"); return; }
      toast.success("Eye vision test created");
      setForm({ title: "", description: "", imageUrl: "", correctAnswer: "", category: "" });
      onCreated();
    } catch {
      toast.error("Failed to create");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create Eye Vision Test</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          {/* Title */}
          <div>
            <Label className="text-sm font-semibold">Title *</Label>
            <Input
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              placeholder="e.g. Vision Chart Line 1"
              className="h-12 text-base"
            />
          </div>

          {/* Description */}
          <div>
            <Label className="text-sm font-semibold">Description (optional)</Label>
            <Textarea
              rows={2}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              placeholder="Brief description / instructions for students"
            />
          </div>

          {/* Image upload */}
          <div className="space-y-2">
            <Label className="text-sm font-semibold">Image *</Label>
            <p className="text-xs text-muted-foreground">
              Upload the image students should look at. The correct answer below is matched case-insensitively against what the student types.
            </p>
            {form.imageUrl ? (
              <div className="relative w-full max-w-md rounded-lg overflow-hidden border">
                <img src={form.imageUrl} alt="Eye vision" className="w-full max-h-72 object-contain bg-slate-50" />
                <Button
                  size="icon"
                  variant="destructive"
                  className="absolute top-2 right-2 h-8 w-8"
                  onClick={() => setForm((f) => ({ ...f, imageUrl: "" }))}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            ) : (
              <button
                type="button"
                onClick={() => fileRef.current?.click()}
                disabled={uploading}
                className="w-full max-w-md h-48 rounded-lg border-2 border-dashed border-slate-300 hover:border-primary hover:bg-slate-50 transition-colors grid place-items-center text-slate-500"
              >
                {uploading ? (
                  <div className="flex flex-col items-center gap-2">
                    <Loader2 className="h-6 w-6 animate-spin" />
                    <span className="text-sm">Uploading…</span>
                  </div>
                ) : (
                  <div className="flex flex-col items-center gap-2">
                    <Upload className="h-8 w-8" />
                    <span className="text-sm font-medium">Click to upload image</span>
                    <span className="text-xs">PNG, JPG, WebP</span>
                  </div>
                )}
              </button>
            )}
            <input
              ref={fileRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={onPickFile}
            />
            {form.imageUrl && (
              <div className="flex gap-2 items-center">
                <Input
                  value={form.imageUrl}
                  onChange={(e) => setForm((f) => ({ ...f, imageUrl: e.target.value }))}
                  placeholder="Or paste image URL…"
                />
                <Button variant="outline" size="lg" onClick={() => fileRef.current?.click()} disabled={uploading}>
                  <Upload className="w-4 h-4 mr-1" /> Replace
                </Button>
              </div>
            )}
          </div>

          {/* Correct answer */}
          <div>
            <Label className="text-sm font-semibold">Correct Answer *</Label>
            <Input
              value={form.correctAnswer}
              onChange={(e) => setForm((f) => ({ ...f, correctAnswer: e.target.value }))}
              placeholder="e.g. 가나다라 or 7 or AB"
              className="h-12 text-base"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Students must type this exactly (case-insensitive). Whitespace is trimmed.
            </p>
          </div>

          {/* Category */}
          <div>
            <Label className="text-sm font-semibold">Category (optional)</Label>
            <Input
              value={form.category}
              onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}
              placeholder="e.g. Letters, Numbers, Symbols"
            />
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2 border-t">
          <Button variant="outline" size="lg" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button size="lg" onClick={create} disabled={busy || uploading}>
            {busy ? "Creating…" : <><Plus className="w-4 h-4 mr-1" /> Create Test</>}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
