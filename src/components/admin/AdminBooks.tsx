"use client";

import { useEffect, useState, useRef } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Plus, Edit2, Trash2, BookOpen, Upload, Loader2, X } from "lucide-react";
import { toast } from "sonner";

interface Book {
  id: string;
  title: string;
  slug: string;
  description?: string;
  author?: string;
  coverUrl?: string | null;
  pdfUrl?: string | null;
  pageCount?: number | null;
  category?: string | null;
  level?: string | null;
  isPublished: boolean;
  downloads: number;
}

export function AdminBooks() {
  const [books, setBooks] = useState<Book[]>([]);
  const [loading, setLoading] = useState(true);
  const [editOpen, setEditOpen] = useState(false);
  const [editingBook, setEditingBook] = useState<Book | null>(null);

  function load() {
    setLoading(true);
    fetch("/api/admin/books")
      .then((r) => r.json())
      .then((d) => setBooks(d.books || []))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function deleteBook(book: Book) {
    if (!confirm(`Delete "${book.title}"?`)) return;
    await fetch(`/api/admin/books/${book.id}`, { method: "DELETE" });
    toast.success("Book deleted");
    load();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Books ({books.length})</h1>
          <p className="text-sm text-muted-foreground mt-1">Upload PDF books with covers. Files stored in R2.</p>
        </div>
        <Button onClick={() => { setEditingBook(null); setEditOpen(true); }}>
          <Plus className="mr-1 h-4 w-4" /> Add Book
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : books.length === 0 ? (
        <Card><CardContent className="py-12 text-center">
          <BookOpen className="h-12 w-12 mx-auto mb-3 text-muted-foreground opacity-40" />
          <p className="text-muted-foreground">No books yet. Upload your first PDF book.</p>
        </CardContent></Card>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {books.map((b) => (
            <Card key={b.id}>
              <CardContent className="p-4">
                <div className="flex gap-3">
                  <div className="w-16 h-20 bg-primary rounded-md flex items-center justify-center shrink-0">
                    {b.coverUrl ? <img src={b.coverUrl} alt={b.title} className="w-full h-full object-cover rounded-md" /> : <BookOpen className="h-6 w-6 text-white" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-semibold text-sm truncate">{b.title}</h3>
                    {b.author && <p className="text-xs text-muted-foreground">by {b.author}</p>}
                    <div className="flex gap-1 mt-1">
                      {b.category && <Badge variant="outline" className="text-xs">{b.category}</Badge>}
                      {b.level && <Badge variant="outline" className="text-xs">{b.level}</Badge>}
                    </div>
                    {b.pdfUrl && <Badge className="text-xs mt-1 bg-green-100 text-green-700">PDF Available</Badge>}
                  </div>
                </div>
                <div className="flex gap-1 mt-3 justify-end">
                  <Button size="sm" variant="ghost" onClick={() => { setEditingBook(b); setEditOpen(true); }}><Edit2 className="h-4 w-4" /></Button>
                  <Button size="sm" variant="ghost" className="text-rose-500" onClick={() => deleteBook(b)}><Trash2 className="h-4 w-4" /></Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <BookEditDialog open={editOpen} onOpenChange={setEditOpen} book={editingBook} onSaved={load} />
    </div>
  );
}

function BookEditDialog({ open, onOpenChange, book, onSaved }: {
  open: boolean; onOpenChange: (v: boolean) => void; book: Book | null; onSaved: () => void;
}) {
  const [title, setTitle] = useState("");
  const [author, setAuthor] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("Beginner");
  const [level, setLevel] = useState("TOPIK 1");
  const [coverUrl, setCoverUrl] = useState("");
  const [pdfUrl, setPdfUrl] = useState("");
  const [pageCount, setPageCount] = useState(0);
  const [isPublished, setIsPublished] = useState(true);
  const [busy, setBusy] = useState(false);
  const [uploadingCover, setUploadingCover] = useState(false);
  const [uploadingPdf, setUploadingPdf] = useState(false);
  const coverRef = useRef<HTMLInputElement>(null);
  const pdfRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (book) {
      setTitle(book.title); setAuthor(book.author || ""); setDescription(book.description || "");
      setCategory(book.category || "Beginner"); setLevel(book.level || "TOPIK 1");
      setCoverUrl(book.coverUrl || ""); setPdfUrl(book.pdfUrl || "");
      setPageCount(book.pageCount || 0); setIsPublished(book.isPublished);
    } else {
      setTitle(""); setAuthor(""); setDescription(""); setCategory("Beginner");
      setLevel("TOPIK 1"); setCoverUrl(""); setPdfUrl(""); setPageCount(0); setIsPublished(true);
    }
  }, [book, open]);

  async function uploadFile(file: File, folder: string, type: "cover" | "pdf") {
    if (type === "cover") setUploadingCover(true); else setUploadingPdf(true);
    try {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("folder", folder);
      const res = await fetch("/api/admin/upload", { method: "POST", body: formData });
      const data = await res.json();
      if (data.ok) {
        const url = data.url.startsWith("http") ? data.url : `https://my-project-five-sepia.vercel.app${data.url}`;
        if (type === "cover") setCoverUrl(url); else setPdfUrl(url);
        toast.success(`${type === "cover" ? "Cover" : "PDF"} uploaded to R2!`);
      } else { toast.error("Upload failed"); }
    } catch { toast.error("Upload failed"); }
    finally { if (type === "cover") setUploadingCover(false); else setUploadingPdf(false); }
  }

  async function save() {
    if (!title.trim()) { toast.error("Title required"); return; }
    setBusy(true);
    const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    try {
      if (book) {
        const res = await fetch(`/api/admin/books/${book.id}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ title, author, description, category, level, coverUrl, pdfUrl, pageCount, isPublished }),
        });
        if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
        toast.success("Book updated");
      } else {
        const res = await fetch("/api/admin/books", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ title, slug, author, description, category, level, coverUrl, pdfUrl, pageCount, isPublished }),
        });
        if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
        toast.success("Book created");
      }
      onOpenChange(false); onSaved();
    } finally { setBusy(false); }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{book ? "Edit Book" : "Add New Book"}</DialogTitle>
          <DialogDescription>Upload PDF and cover image directly from your computer.</DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div><Label>Title *</Label><Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Korean Grammar in Use" /></div>
          <div><Label>Author</Label><Input value={author} onChange={(e) => setAuthor(e.target.value)} placeholder="Author name" /></div>
          <div><Label>Description</Label><Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} /></div>
          <div className="grid grid-cols-2 gap-3">
            <div><Label>Category</Label>
              <Select value={category} onValueChange={setCategory}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent>
                <SelectItem value="Beginner">Beginner</SelectItem><SelectItem value="Intermediate">Intermediate</SelectItem>
                <SelectItem value="Advanced">Advanced</SelectItem><SelectItem value="TOPIK">TOPIK</SelectItem>
              </SelectContent></Select>
            </div>
            <div><Label>Level</Label>
              <Select value={level} onValueChange={setLevel}><SelectTrigger><SelectValue /></SelectTrigger><SelectContent>
                <SelectItem value="TOPIK 1">TOPIK 1</SelectItem><SelectItem value="TOPIK 2">TOPIK 2</SelectItem>
                <SelectItem value="TOPIK 3">TOPIK 3</SelectItem><SelectItem value="TOPIK 4">TOPIK 4</SelectItem>
                <SelectItem value="TOPIK 5">TOPIK 5</SelectItem><SelectItem value="TOPIK 6">TOPIK 6</SelectItem>
              </SelectContent></Select>
            </div>
          </div>

          {/* Cover Image Upload */}
          <div className="space-y-2">
            <Label>Cover Image</Label>
            {coverUrl && <div className="relative w-full h-32 rounded-lg overflow-hidden border">
              <img src={coverUrl} alt="Cover" className="w-full h-full object-cover" />
              <Button size="sm" variant="destructive" className="absolute top-2 right-2 h-6 w-6 p-0" onClick={() => setCoverUrl("")}><X className="h-3 w-3" /></Button>
            </div>}
            <input ref={coverRef} type="file" accept="image/*" className="hidden" onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadFile(f, "covers", "cover"); e.target.value = ""; }} />
            <Button type="button" variant="outline" size="sm" onClick={() => coverRef.current?.click()} disabled={uploadingCover}>
              {uploadingCover ? <Loader2 className="mr-1 h-3 w-3 animate-spin" /> : <Upload className="mr-1 h-3 w-3" />}
              {uploadingCover ? "Uploading..." : "Upload Cover Image"}
            </Button>
          </div>

          {/* PDF Upload */}
          <div className="space-y-2">
            <Label>PDF Book File</Label>
            {pdfUrl && <div className="flex items-center gap-2 p-2 bg-green-50 rounded-lg border border-green-200">
              <Badge className="bg-green-500">PDF Ready</Badge>
              <span className="text-xs text-green-700 truncate">{pdfUrl}</span>
              <Button size="sm" variant="ghost" className="h-6 w-6 p-0 text-rose-500" onClick={() => setPdfUrl("")}><X className="h-3 w-3" /></Button>
            </div>}
            <input ref={pdfRef} type="file" accept=".pdf,application/pdf" className="hidden" onChange={(e) => { const f = e.target.files?.[0]; if (f) uploadFile(f, "books", "pdf"); e.target.value = ""; }} />
            <Button type="button" variant="outline" size="sm" onClick={() => pdfRef.current?.click()} disabled={uploadingPdf}>
              {uploadingPdf ? <Loader2 className="mr-1 h-3 w-3 animate-spin" /> : <Upload className="mr-1 h-3 w-3" />}
              {uploadingPdf ? "Uploading PDF..." : "Upload PDF from Computer"}
            </Button>
          </div>

          <div><Label>Page Count</Label><Input type="number" value={pageCount} onChange={(e) => setPageCount(parseInt(e.target.value) || 0)} /></div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={save} disabled={busy}>{busy ? <Loader2 className="h-4 w-4 animate-spin" /> : null} {book ? "Update" : "Create"} Book</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
