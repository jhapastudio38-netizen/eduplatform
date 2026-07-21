"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { BookOpen, Plus, Trash2, Pencil, Upload, FileText, Download } from "lucide-react";
import { toast } from "sonner";

interface Book {
  id: string;
  title: string;
  slug: string;
  description?: string;
  author?: string;
  coverUrl?: string;
  pdfUrl?: string;
  pageCount?: number;
  category?: string;
  level?: string;
  isPublished: boolean;
  downloads: number;
  createdAt: string;
}

export function AdminBooks() {
  const [books, setBooks] = useState<Book[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Book | null>(null);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({
    title: "", description: "", author: "", category: "", level: "",
    pdfUrl: "", pageCount: 1, isPublished: true,
  });

  function load() {
    fetch("/api/admin/books")
      .then(r => r.json())
      .then(d => setBooks(d.books || []))
      .catch(() => {});
  }
  useEffect(load, []);

  function openCreate() {
    setEditing(null);
    setForm({ title: "", description: "", author: "", category: "", level: "", pdfUrl: "", pageCount: 1, isPublished: true });
    setOpen(true);
  }

  function openEdit(book: Book) {
    setEditing(book);
    setForm({
      title: book.title, description: book.description || "", author: book.author || "",
      category: book.category || "", level: book.level || "", pdfUrl: book.pdfUrl || "",
      pageCount: book.pageCount || 1, isPublished: book.isPublished,
    });
    setOpen(true);
  }

  async function save() {
    if (!form.title.trim()) { toast.error("Title required"); return; }
    setBusy(true);
    const slug = form.title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
    const body = { ...form, slug, pageCount: Number(form.pageCount) };
    const url = editing ? `/api/admin/books/${editing.id}` : "/api/admin/books";
    const method = editing ? "PUT" : "POST";
    const res = await fetch(url, { method, headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success(editing ? "Book updated" : "Book created");
    setOpen(false);
    load();
  }

  async function del(id: string) {
    if (!confirm("Delete this book?")) return;
    await fetch(`/api/admin/books/${id}`, { method: "DELETE" });
    toast.success("Deleted");
    load();
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Books ({books.length})</h1>
          <p className="text-sm text-muted-foreground">Digital library — add Korean learning textbooks with PDF viewer.</p>
        </div>
        <Button onClick={openCreate}><Plus className="mr-1 h-4 w-4" /> Add Book</Button>
      </div>

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
        {books.length === 0 ? (
          <Card className="col-span-full">
            <CardContent className="p-8 text-center text-muted-foreground">
              <BookOpen className="h-10 w-10 mx-auto mb-2 opacity-40" />
              <p>No books yet. Add your first Korean learning textbook.</p>
            </CardContent>
          </Card>
        ) : books.map(book => (
          <Card key={book.id} className="hover:shadow-md transition">
            <CardContent className="p-4">
              <div className="flex items-start gap-3 mb-3">
                <div className="h-12 w-10 rounded bg-gradient-to-br from-emerald-400 to-teal-500 grid place-items-center shrink-0">
                  <BookOpen className="h-5 w-5 text-white" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="font-semibold truncate">{book.title}</div>
                  {book.author && <div className="text-xs text-muted-foreground">{book.author}</div>}
                </div>
              </div>
              <p className="text-xs text-muted-foreground line-clamp-2 mb-3">{book.description}</p>
              <div className="flex items-center gap-2 mb-3">
                {book.category && <Badge variant="secondary" className="text-xs">{book.category}</Badge>}
                {book.level && <Badge variant="outline" className="text-xs">{book.level}</Badge>}
                {book.isPublished ? <Badge className="text-xs bg-emerald-100 text-emerald-700">Published</Badge> : <Badge variant="outline" className="text-xs">Draft</Badge>}
              </div>
              <div className="flex items-center justify-between text-xs text-muted-foreground mb-3">
                <span>{book.pageCount || 0} pages</span>
                <span>{book.downloads} downloads</span>
              </div>
              {book.pdfUrl && (
                <a href={book.pdfUrl} target="_blank" rel="noreferrer" className="block">
                  <Button variant="outline" size="sm" className="w-full">
                    <FileText className="mr-1 h-3 w-3" /> View PDF
                  </Button>
                </a>
              )}
              <div className="flex gap-1 mt-2">
                <Button size="sm" variant="ghost" onClick={() => openEdit(book)}><Pencil className="h-3 w-3" /></Button>
                <Button size="sm" variant="ghost" onClick={() => del(book.id)}><Trash2 className="h-3 w-3 text-rose-500" /></Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader><DialogTitle>{editing ? "Edit Book" : "Add Book"}</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <div><Label>Title</Label><Input value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder="TOPIK I Complete Guide" /></div>
            <div><Label>Author</Label><Input value={form.author} onChange={e => setForm(f => ({ ...f, author: e.target.value }))} placeholder="Park Min-jun" /></div>
            <div><Label>Description</Label><Textarea rows={3} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} /></div>
            <div className="grid grid-cols-2 gap-3">
              <div><Label>Category</Label><Input value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value }))} placeholder="TOPIK / Grammar / Conversation" /></div>
              <div><Label>Level</Label>
                <Select value={form.level} onValueChange={v => setForm(f => ({ ...f, level: v }))}>
                  <SelectTrigger><SelectValue placeholder="Select level" /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Beginner">Beginner</SelectItem>
                    <SelectItem value="Intermediate">Intermediate</SelectItem>
                    <SelectItem value="Advanced">Advanced</SelectItem>
                    <SelectItem value="TOPIK 1">TOPIK 1</SelectItem>
                    <SelectItem value="TOPIK 2">TOPIK 2</SelectItem>
                    <SelectItem value="TOPIK 3">TOPIK 3</SelectItem>
                    <SelectItem value="TOPIK 4">TOPIK 4</SelectItem>
                    <SelectItem value="TOPIK 5">TOPIK 5</SelectItem>
                    <SelectItem value="TOPIK 6">TOPIK 6</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div><Label>PDF URL (S3 or direct link)</Label><Input value={form.pdfUrl} onChange={e => setForm(f => ({ ...f, pdfUrl: e.target.value }))} placeholder="https://s3.amazonaws.com/..." /></div>
            <div><Label>Page Count</Label><Input type="number" value={form.pageCount} onChange={e => setForm(f => ({ ...f, pageCount: Number(e.target.value) }))} /></div>
            <Button onClick={save} disabled={busy} className="w-full">{busy ? "Saving…" : editing ? "Update Book" : "Create Book"}</Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
