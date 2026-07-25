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
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { BookMarked, Plus, Trash2, Upload, Loader2, X, FileText } from "lucide-react";
import { toast } from "sonner";

interface Book {
  id: string;
  title: string;
  slug?: string;
  description?: string | null;
  author?: string | null;
  coverUrl?: string | null;
  pdfUrl?: string | null;
  category?: string | null;
  level?: string | null;
  publishedDate?: string | null;
  isPublished: boolean;
  downloads?: number;
  createdAt?: string;
}

export function AdminBooks() {
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);

  function load() {
    setLoading(true);
    fetch("/api/admin/books")
      .then((r) => r.json())
      .then((d) => setBooks(d.books || []))
      .catch(() => setBooks([]))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function deleteBook(b: Book) {
    if (!confirm(`Delete "${b.title}"?`)) return;
    try {
      const res = await fetch(`/api/admin/books/${b.id}`, { method: "DELETE" });
      if (!res.ok) {
        const d = await res.json();
        toast.error(d.error || "Delete failed");
        return;
      }
      toast.success("Book deleted");
      load();
    } catch {
      toast.error("Delete failed");
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Books ({books.length})</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Upload PDF books with cover images, author info, and published dates.
          </p>
        </div>
        <Button size="lg" onClick={() => setCreateOpen(true)}>
          <Plus className="w-4 h-4 mr-1" /> Add Book
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : books.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <BookMarked className="h-12 w-12 mx-auto mb-3 text-muted-foreground opacity-40" />
            <p className="text-muted-foreground">No books yet. Click &ldquo;Add Book&rdquo; to upload your first PDF.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {books.map((b) => (
            <Card key={b.id} className="overflow-hidden hover:shadow-md transition-shadow">
              <div className="aspect-[3/4] bg-gradient-to-br from-emerald-100 to-teal-100 flex items-center justify-center overflow-hidden">
                {b.coverUrl ? (
                  <img src={b.coverUrl} alt={b.title} className="w-full h-full object-cover" />
                ) : (
                  <BookMarked className="h-12 w-12 text-emerald-700/60" />
                )}
              </div>
              <CardContent className="p-4 space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-tight line-clamp-2">{b.title}</h3>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="shrink-0 text-rose-500 hover:text-rose-600"
                    onClick={() => deleteBook(b)}
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
                {b.author && (
                  <p className="text-xs text-muted-foreground">by {b.author}</p>
                )}
                {b.description && (
                  <p className="text-xs text-muted-foreground line-clamp-2">{b.description}</p>
                )}
                <div className="flex flex-wrap items-center gap-1.5 pt-1">
                  {b.category && <Badge variant="secondary" className="text-xs">{b.category}</Badge>}
                  {b.level && <Badge variant="outline" className="text-xs">{b.level}</Badge>}
                  {b.publishedDate && (
                    <Badge variant="outline" className="text-xs">
                      <FileText className="w-3 h-3 mr-1" />
                      {new Date(b.publishedDate).toLocaleDateString()}
                    </Badge>
                  )}
                  {b.pdfUrl && (
                    <Badge className="text-xs bg-emerald-100 text-emerald-700">PDF</Badge>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {createOpen && (
        <CreateBookDialog
          open={createOpen}
          onOpenChange={setCreateOpen}
          onCreated={() => { setCreateOpen(false); load(); }}
        />
      )}
    </div>
  );
}

function CreateBookDialog({ open, onOpenChange, onCreated }: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onCreated: () => void;
}) {
  const [form, setForm] = useState({
    title: "",
    author: "",
    publishedDate: "",
    description: "",
    coverUrl: "",
    pdfUrl: "",
    category: "Beginner",
    level: "TOPIK 1",
  });
  const [busy, setBusy] = useState(false);
  const [uploadingCover, setUploadingCover] = useState(false);
  const [uploadingPdf, setUploadingPdf] = useState(false);
  const coverRef = useRef<HTMLInputElement>(null);
  const pdfRef = useRef<HTMLInputElement>(null);

  async function uploadFile(file: File, folder: string): Promise<string> {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("folder", folder);
    const res = await fetch("/api/admin/file-upload", { method: "POST", body: fd });
    const d = await res.json();
    if (!res.ok || !d.url) {
      throw new Error(d.error || "Upload failed");
    }
    return d.url;
  }

  async function pickCover(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (!f) return;
    setUploadingCover(true);
    try {
      const url = await uploadFile(f, "covers");
      setForm((p) => ({ ...p, coverUrl: url }));
      toast.success("Cover uploaded");
    } catch (err: any) {
      toast.error(err.message || "Upload failed");
    } finally {
      setUploadingCover(false);
      if (coverRef.current) coverRef.current.value = "";
    }
  }

  async function pickPdf(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (!f) return;
    setUploadingPdf(true);
    try {
      const url = await uploadFile(f, "books");
      setForm((p) => ({ ...p, pdfUrl: url }));
      toast.success("PDF uploaded");
    } catch (err: any) {
      toast.error(err.message || "Upload failed");
    } finally {
      setUploadingPdf(false);
      if (pdfRef.current) pdfRef.current.value = "";
    }
  }

  async function create() {
    if (!form.title.trim()) { toast.error("Book name is required"); return; }
    if (!form.pdfUrl) { toast.error("Please upload a PDF"); return; }
    setBusy(true);
    const slug = form.title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") + "-" + Date.now().toString(36);
    try {
      const res = await fetch("/api/admin/books", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          slug,
          author: form.author.trim() || undefined,
          description: form.description.trim() || undefined,
          coverUrl: form.coverUrl || undefined,
          pdfUrl: form.pdfUrl,
          publishedDate: form.publishedDate || undefined,
          category: form.category,
          level: form.level,
          isPublished: true,
        }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Failed to create book"); return; }
      toast.success("Book created");
      setForm({
        title: "", author: "", publishedDate: "", description: "",
        coverUrl: "", pdfUrl: "", category: "Beginner", level: "TOPIK 1",
      });
      onCreated();
    } catch {
      toast.error("Failed to create book");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Add New Book</DialogTitle>
          <DialogDescription>
            Upload a PDF file and a cover image directly from your computer.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Book Name */}
          <div>
            <Label className="text-sm font-semibold">Book Name *</Label>
            <Input
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              placeholder="e.g. Korean Grammar in Use"
              className="h-12 text-base"
            />
          </div>

          {/* Author + Published Date */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <Label className="text-sm font-semibold">Author</Label>
              <Input
                value={form.author}
                onChange={(e) => setForm((f) => ({ ...f, author: e.target.value }))}
                placeholder="Author name"
              />
            </div>
            <div>
              <Label className="text-sm font-semibold">Published Date</Label>
              <Input
                type="date"
                value={form.publishedDate}
                onChange={(e) => setForm((f) => ({ ...f, publishedDate: e.target.value }))}
              />
            </div>
          </div>

          {/* About Book */}
          <div>
            <Label className="text-sm font-semibold">About Book</Label>
            <Textarea
              rows={3}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              placeholder="Brief description / summary of the book"
            />
          </div>

          {/* Cover Image Upload */}
          <div className="space-y-2">
            <Label className="text-sm font-semibold">Cover Image</Label>
            {form.coverUrl ? (
              <div className="relative w-40 h-52 rounded-lg overflow-hidden border">
                <img src={form.coverUrl} alt="Cover" className="w-full h-full object-cover" />
                <Button
                  size="icon"
                  variant="destructive"
                  className="absolute top-1 right-1 h-7 w-7"
                  onClick={() => setForm((f) => ({ ...f, coverUrl: "" }))}
                >
                  <X className="h-3 w-3" />
                </Button>
              </div>
            ) : null}
            <input
              ref={coverRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={pickCover}
            />
            <Button
              type="button"
              variant="outline"
              size="lg"
              onClick={() => coverRef.current?.click()}
              disabled={uploadingCover}
            >
              {uploadingCover ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <Upload className="w-4 h-4 mr-2" />
              )}
              {uploadingCover ? "Uploading…" : "📁 Upload Cover Image"}
            </Button>
          </div>

          {/* PDF Upload */}
          <div className="space-y-2">
            <Label className="text-sm font-semibold">PDF Book File *</Label>
            {form.pdfUrl ? (
              <div className="flex items-center gap-2 p-3 bg-emerald-50 rounded-lg border border-emerald-200">
                <Badge className="bg-emerald-500">PDF Ready</Badge>
                <span className="text-xs text-emerald-700 truncate flex-1">{form.pdfUrl}</span>
                <Button
                  size="icon"
                  variant="ghost"
                  className="h-7 w-7 text-rose-500"
                  onClick={() => setForm((f) => ({ ...f, pdfUrl: "" }))}
                >
                  <X className="h-3 w-3" />
                </Button>
              </div>
            ) : null}
            <input
              ref={pdfRef}
              type="file"
              accept=".pdf,application/pdf"
              className="hidden"
              onChange={pickPdf}
            />
            <Button
              type="button"
              variant="outline"
              size="lg"
              onClick={() => pdfRef.current?.click()}
              disabled={uploadingPdf}
            >
              {uploadingPdf ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <Upload className="w-4 h-4 mr-2" />
              )}
              {uploadingPdf ? "Uploading PDF…" : "📁 Upload PDF from Computer"}
            </Button>
          </div>

          {/* Category + Level */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <Label className="text-sm font-semibold">Category</Label>
              <Input
                value={form.category}
                onChange={(e) => setForm((f) => ({ ...f, category: e.target.value }))}
                placeholder="Beginner / Intermediate / TOPIK…"
              />
            </div>
            <div>
              <Label className="text-sm font-semibold">Level</Label>
              <Input
                value={form.level}
                onChange={(e) => setForm((f) => ({ ...f, level: e.target.value }))}
                placeholder="TOPIK 1 — TOPIK 6"
              />
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" size="lg" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button size="lg" onClick={create} disabled={busy || uploadingPdf || uploadingCover}>
            {busy ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Plus className="w-4 h-4 mr-2" />}
            {busy ? "Creating…" : "Create Book"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
