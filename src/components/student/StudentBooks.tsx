"use client";

/**
 * Student Books — Korean learning digital library.
 * Shows book covers, author names, categories, levels.
 * Students can read PDFs in the built-in viewer.
 */

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { BookOpen, Search, FileText, Download, User, BookMarked, Filter, X } from "lucide-react";

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
  downloads: number;
}

export function StudentBooks() {
  const [books, setBooks] = useState<Book[]>([]);
  const [search, setSearch] = useState("");
  const [filterLevel, setFilterLevel] = useState("all");
  const [filterCategory, setFilterCategory] = useState("all");
  const [reading, setReading] = useState<Book | null>(null);

  useEffect(() => {
    fetch("/api/student/books")
      .then(r => r.json())
      .then(d => setBooks(d.books || []))
      .catch(() => {});
  }, []);

  const categories = [...new Set(books.map(b => b.category).filter(Boolean))] as string[];
  const levels = ["Beginner", "Intermediate", "Advanced", "TOPIK 1", "TOPIK 2", "TOPIK 3", "TOPIK 4", "TOPIK 5", "TOPIK 6"];

  const filtered = books.filter(b => {
    if (search && !b.title.toLowerCase().includes(search.toLowerCase()) && !(b.author || "").toLowerCase().includes(search.toLowerCase())) return false;
    if (filterLevel !== "all" && b.level !== filterLevel) return false;
    if (filterCategory !== "all" && b.category !== filterCategory) return false;
    return true;
  });

  // Generate a gradient cover if no coverUrl
  function coverGradient(book: Book) {
    const colors = [
      "from-emerald-400 to-teal-600",
      "from-blue-400 to-indigo-600",
      "from-amber-400 to-orange-600",
      "from-rose-400 to-pink-600",
      "from-purple-400 to-violet-600",
      "from-cyan-400 to-blue-600",
    ];
    const hash = book.title.charCodeAt(0) % colors.length;
    return colors[hash];
  }

  if (reading) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={() => setReading(null)}>
          <X className="mr-1 h-4 w-4" /> Back to library
        </Button>
        <div className="flex gap-4">
          <div className={`w-24 h-32 rounded-lg bg-gradient-to-br ${coverGradient(reading)} grid place-items-center shrink-0 shadow-lg`}>
            <BookOpen className="h-8 w-8 text-white" />
          </div>
          <div className="flex-1">
            <h2 className="text-xl font-bold">{reading.title}</h2>
            {reading.author && <p className="text-sm text-muted-foreground flex items-center gap-1 mt-1"><User className="h-3 w-3" /> {reading.author}</p>}
            {reading.description && <p className="text-sm text-muted-foreground mt-2">{reading.description}</p>}
            <div className="flex items-center gap-2 mt-3">
              {reading.category && <Badge variant="secondary">{reading.category}</Badge>}
              {reading.level && <Badge variant="outline">{reading.level}</Badge>}
              {reading.pageCount && <span className="text-xs text-muted-foreground">{reading.pageCount} pages</span>}
            </div>
          </div>
        </div>
        {reading.pdfUrl ? (
          <div className="rounded-lg overflow-hidden border">
            <iframe src={reading.pdfUrl} className="w-full h-[600px]" title={reading.title} />
          </div>
        ) : (
          <Card><CardContent className="p-8 text-center text-muted-foreground">
            <FileText className="h-10 w-10 mx-auto mb-2 opacity-40" />
            <p>No PDF available for this book yet.</p>
          </CardContent></Card>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">Korean Learning Books</h2>
        <Badge variant="secondary">{filtered.length} books</Badge>
      </div>

      {/* Search + filters */}
      <div className="flex flex-wrap gap-2">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input placeholder="Search by title or author..." value={search} onChange={e => setSearch(e.target.value)} className="pl-9" />
        </div>
        <select className="px-3 py-2 rounded-md border bg-background text-sm" value={filterLevel} onChange={e => setFilterLevel(e.target.value)}>
          <option value="all">All Levels</option>
          {levels.map(l => <option key={l} value={l}>{l}</option>)}
        </select>
        {categories.length > 0 && (
          <select className="px-3 py-2 rounded-md border bg-background text-sm" value={filterCategory} onChange={e => setFilterCategory(e.target.value)}>
            <option value="all">All Categories</option>
            {categories.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        )}
      </div>

      {/* Book grid — showcase style with covers */}
      {filtered.length === 0 ? (
        <Card><CardContent className="p-8 text-center text-muted-foreground">
          <BookMarked className="h-10 w-10 mx-auto mb-2 opacity-40" />
          <p>No books available yet. Check back soon!</p>
        </CardContent></Card>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {filtered.map(book => (
            <Card key={book.id} className="hover:shadow-lg transition cursor-pointer overflow-hidden group" onClick={() => setReading(book)}>
              {/* Book cover */}
              <div className="relative h-48 overflow-hidden">
                {book.coverUrl ? (
                  <img src={book.coverUrl} alt={book.title} className="w-full h-full object-cover group-hover:scale-105 transition" />
                ) : (
                  <div className={`w-full h-full bg-gradient-to-br ${coverGradient(book)} grid place-items-center`}>
                    <BookOpen className="h-12 w-12 text-white/80" />
                  </div>
                )}
                {/* Level badge overlay */}
                {book.level && (
                  <div className="absolute top-2 right-2">
                    <Badge className="text-xs bg-black/60 text-white hover:bg-black/60">{book.level}</Badge>
                  </div>
                )}
              </div>
              {/* Book info */}
              <CardContent className="p-3">
                <div className="font-semibold text-sm line-clamp-2 mb-1">{book.title}</div>
                {book.author && (
                  <div className="text-xs text-muted-foreground flex items-center gap-1 mb-2">
                    <User className="h-3 w-3" /> {book.author}
                  </div>
                )}
                <div className="flex items-center justify-between">
                  {book.category && <Badge variant="secondary" className="text-xs">{book.category}</Badge>}
                  {book.pageCount && <span className="text-xs text-muted-foreground">{book.pageCount}p</span>}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
