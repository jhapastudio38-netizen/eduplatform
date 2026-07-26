"use client";

/**
 * AdminBundles — manage Question Bank / Batch / Exam packages.
 *
 * A bundle is a curated collection of Tests. Admin/teacher creates a bundle,
 * adds existing tests to it, and publishes it. Students see published bundles
 * in the app as packages.
 *
 * UI:
 *   • Top-level grid of bundles filtered by kind (qbank / batch / exam / chapter)
 *   • Click a bundle → editor dialog showing its tests + a search box to add more
 *   • Create dialog with title / kind / description / price / cover image
 *   • Publish toggle on each bundle (refuses empty bundles)
 */

import { useEffect, useState } from "react";
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
} from "@/components/ui/dialog";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import {
  Package, Plus, Trash2, Search, X, CheckCircle2, Loader2, Layers,
} from "lucide-react";
import { toast } from "sonner";
import { FastImageUpload } from "./FastImageUpload";

type Kind = "qbank" | "batch" | "exam" | "chapter";

interface BundleTest {
  id: string;
  title: string;
  testCategory: string | null;
  examType: string;
  durationMin: number;
  isPublished: boolean;
  _count?: { items: number };
}

interface BundleItem {
  id: string;
  bundleId: string;
  testId: string;
  sortOrder: number;
  test: BundleTest;
}

interface Bundle {
  id: string;
  title: string;
  slug: string;
  description?: string | null;
  kind: Kind;
  coverUrl?: string | null;
  price: number;
  isPublished: boolean;
  batchId?: string | null;
  createdAt: string;
  _count?: { items: number };
  items?: BundleItem[];
}

const KIND_LABEL: Record<Kind, string> = {
  qbank: "Question Bank",
  batch: "Batch",
  exam: "Exam",
  chapter: "Chapter",
};

export function AdminBundles({ initialKind = "qbank" }: { initialKind?: Kind }) {
  const [kind, setKind] = useState<Kind>(initialKind);
  const [bundles, setBundles] = useState<Bundle[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Bundle | null>(null);

  function load() {
    setLoading(true);
    fetch(`/api/admin/bundles?kind=${kind}`)
      .then((r) => r.json())
      .then((d) => setBundles(d.bundles || []))
      .catch(() => setBundles([]))
      .finally(() => setLoading(false));
  }
  useEffect(load, [kind]);

  async function deleteBundle(b: Bundle) {
    if (!confirm(`Delete bundle "${b.title}"? This does NOT delete the tests inside it.`)) return;
    const res = await fetch(`/api/admin/bundles/${b.id}`, { method: "DELETE" });
    if (!res.ok) {
      const d = await res.json();
      toast.error(d.error || "Delete failed");
      return;
    }
    toast.success("Bundle deleted");
    load();
  }

  async function togglePublish(b: Bundle) {
    const publish = !b.isPublished;
    const res = await fetch(`/api/admin/bundles/${b.id}/publish`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ publish }),
    });
    const d = await res.json();
    if (!res.ok) {
      toast.error(d.error || "Publish failed");
      return;
    }
    toast.success(publish ? "Bundle published — students can see it now" : "Bundle unpublished");
    load();
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Package className="w-6 h-6" />
            {KIND_LABEL[kind]} Packages ({bundles.length})
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Combine multiple {KIND_LABEL[kind].toLowerCase()} sets into a single package students can browse.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {/* Kind selector — lets the admin switch between qbank/batch/exam/chapter bundles */}
          <Select value={kind} onValueChange={(v) => setKind(v as Kind)}>
            <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="qbank">Question Bank</SelectItem>
              <SelectItem value="batch">Batch</SelectItem>
              <SelectItem value="exam">Exam</SelectItem>
              <SelectItem value="chapter">Chapter</SelectItem>
            </SelectContent>
          </Select>
          <Button size="lg" onClick={() => setCreateOpen(true)}>
            <Plus className="w-4 h-4 mr-1" /> New Package
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : bundles.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Package className="w-12 h-12 mx-auto mb-3 text-muted-foreground opacity-40" />
            <p className="text-muted-foreground">
              No {KIND_LABEL[kind].toLowerCase()} packages yet. Click &ldquo;New Package&rdquo; to create one.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {bundles.map((b) => (
            <Card key={b.id} className="overflow-hidden hover:shadow-md transition-shadow cursor-pointer" onClick={() => setEditing(b)}>
              <div className="aspect-video bg-slate-100 flex items-center justify-center overflow-hidden">
                {b.coverUrl ? (
                  <img src={b.coverUrl} alt={b.title} className="w-full h-full object-cover" />
                ) : (
                  <Package className="w-10 h-10 text-slate-400" />
                )}
              </div>
              <CardContent className="p-4 space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <h3 className="font-semibold leading-tight">{b.title}</h3>
                  {b.isPublished ? (
                    <Badge className="bg-green-500">Live</Badge>
                  ) : (
                    <Badge variant="secondary">Draft</Badge>
                  )}
                </div>
                {b.description && (
                  <p className="text-xs text-muted-foreground line-clamp-2">{b.description}</p>
                )}
                <div className="flex flex-wrap items-center gap-2 pt-1">
                  <Badge variant="outline">
                    <Layers className="w-3 h-3 mr-1" /> {b._count?.items ?? 0} sets
                  </Badge>
                  {b.price > 0 ? (
                    <Badge>₩{b.price.toLocaleString()}</Badge>
                  ) : (
                    <Badge variant="outline">Free</Badge>
                  )}
                </div>
                <div className="flex gap-2 pt-2 border-t" onClick={(e) => e.stopPropagation()}>
                  <Button
                    size="sm"
                    className="flex-1"
                    variant={b.isPublished ? "outline" : "default"}
                    onClick={() => togglePublish(b)}
                  >
                    {b.isPublished ? "Unpublish" : "Publish"}
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="text-rose-500"
                    onClick={() => deleteBundle(b)}
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {createOpen && (
        <CreateBundleDialog
          kind={kind}
          onOpenChange={setCreateOpen}
          onCreated={(b) => { setCreateOpen(false); setEditing(b); load(); }}
        />
      )}

      {editing && (
        <BundleEditor
          bundle={editing}
          onOpenChange={(v) => { if (!v) { setEditing(null); load(); } }}
        />
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// CREATE BUNDLE DIALOG
// ═══════════════════════════════════════════════════════════════════════════

function CreateBundleDialog({ kind, onOpenChange, onCreated }: {
  kind: Kind;
  onOpenChange: (v: boolean) => void;
  onCreated: (b: Bundle) => void;
}) {
  const [form, setForm] = useState({
    title: "",
    description: "",
    price: "0",
    coverUrl: "",
  });
  const [busy, setBusy] = useState(false);

  async function uploadFile(file: File): Promise<string> {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("folder", "bundle-covers");
    const res = await fetch("/api/admin/file-upload", { method: "POST", body: fd });
    const d = await res.json();
    if (!res.ok || !d.url) throw new Error(d.error || "Upload failed");
    return d.url;
  }

  async function create() {
    if (!form.title.trim()) { toast.error("Title is required"); return; }
    setBusy(true);
    try {
      const res = await fetch("/api/admin/bundles", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          description: form.description.trim(),
          kind,
          coverUrl: form.coverUrl || undefined,
          price: parseInt(form.price) || 0,
        }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Failed to create"); return; }
      toast.success("Package created — add tests now");
      onCreated(d.bundle);
    } catch {
      toast.error("Failed to create");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={true} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>New {KIND_LABEL[kind]} Package</DialogTitle>
          <DialogDescription>
            A package groups multiple {KIND_LABEL[kind].toLowerCase()} sets into one bundle students can browse.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <Label>Package Title *</Label>
            <Input
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              placeholder="e.g. TOPIK 1 Complete Question Bank"
              className="h-12 text-base"
            />
          </div>
          <div>
            <Label>Description</Label>
            <Textarea
              rows={2}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              placeholder="Brief description shown to students"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label>Price (₩)</Label>
              <Input
                type="number"
                value={form.price}
                onChange={(e) => setForm((f) => ({ ...f, price: e.target.value }))}
                min={0}
                placeholder="0 = free"
              />
            </div>
            <div>
              <Label>Kind</Label>
              <Input value={KIND_LABEL[kind]} disabled />
            </div>
          </div>
          <div>
            <Label>Cover Image</Label>
            <FastImageUpload
              url={form.coverUrl}
              onUpload={(url) => setForm((f) => ({ ...f, coverUrl: url }))}
              onClear={() => setForm((f) => ({ ...f, coverUrl: "" }))}
              folder="bundle-covers"
              previewClassName="w-32 h-24"
            />
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-2 border-t">
          <Button variant="outline" size="lg" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button size="lg" onClick={create} disabled={busy}>
            {busy ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Plus className="w-4 h-4 mr-1" />}
            {busy ? "Creating…" : "Create"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// BUNDLE EDITOR — add/remove tests
// ═══════════════════════════════════════════════════════════════════════════

function BundleEditor({ bundle, onOpenChange }: {
  bundle: Bundle;
  onOpenChange: (v: boolean) => void;
}) {
  const [items, setItems] = useState<BundleItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [searchResults, setSearchResults] = useState<BundleTest[]>([]);
  const [searching, setSearching] = useState(false);
  const [addingId, setAddingId] = useState<string | null>(null);

  function loadItems() {
    setLoading(true);
    fetch(`/api/admin/bundles/${bundle.id}/items`)
      .then((r) => r.json())
      .then((d) => setItems(d.items || []))
      .finally(() => setLoading(false));
  }
  useEffect(loadItems, [bundle.id]);

  // Debounced search across all tests (of any category)
  useEffect(() => {
    if (!search.trim()) { setSearchResults([]); return; }
    setSearching(true);
    const t = setTimeout(async () => {
      try {
        // Reuse the existing admin tests endpoint, filtered by query param `q`
        const res = await fetch(`/api/admin/tests?q=${encodeURIComponent(search)}`);
        const d = await res.json();
        setSearchResults(d.tests || []);
      } catch {
        setSearchResults([]);
      } finally {
        setSearching(false);
      }
    }, 300);
    return () => clearTimeout(t);
  }, [search]);

  async function addTest(testId: string) {
    setAddingId(testId);
    try {
      const res = await fetch(`/api/admin/bundles/${bundle.id}/items`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ testId }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Add failed"); return; }
      toast.success("Added to package");
      loadItems();
      setSearch("");
      setSearchResults([]);
    } finally {
      setAddingId(null);
    }
  }

  async function removeTest(item: BundleItem) {
    if (!confirm(`Remove "${item.test.title}" from this package?`)) return;
    const res = await fetch(`/api/admin/bundles/${bundle.id}/items/${item.testId}`, { method: "DELETE" });
    if (!res.ok) {
      const d = await res.json();
      toast.error(d.error || "Remove failed");
      return;
    }
    toast.success("Removed");
    loadItems();
  }

  const addedIds = new Set(items.map((i) => i.testId));

  return (
    <Dialog open={true} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 flex-wrap">
            <Package className="w-5 h-5" />
            <span>{bundle.title}</span>
            <Badge variant="outline">{KIND_LABEL[bundle.kind as Kind]}</Badge>
            {bundle.isPublished && <Badge className="bg-green-500">Live</Badge>}
          </DialogTitle>
          <DialogDescription>
            Add tests to this package. Students see all tests in the order shown below.
          </DialogDescription>
        </DialogHeader>

        {/* Search to add tests */}
        <div className="space-y-2">
          <Label className="text-sm font-semibold">Add tests to this package</Label>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              className="pl-9"
              placeholder="Search tests by name…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            {searching && <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 animate-spin text-muted-foreground" />}
          </div>

          {/* Search results */}
          {search.trim() && (
            <div className="border rounded-lg max-h-48 overflow-y-auto divide-y">
              {searchResults.length === 0 ? (
                <p className="p-3 text-xs text-muted-foreground text-center">No tests match &ldquo;{search}&rdquo;</p>
              ) : (
                searchResults.map((t) => {
                  const already = addedIds.has(t.id);
                  return (
                    <div key={t.id} className="flex items-center gap-3 p-2 hover:bg-muted/30">
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium truncate">{t.title}</div>
                        <div className="text-xs text-muted-foreground">
                          {t.testCategory || t.examType} · {t.durationMin} min
                          {t._count?.items ? ` · ${t._count.items} Qs` : ""}
                        </div>
                      </div>
                      {already ? (
                        <Badge variant="outline" className="text-emerald-700 border-emerald-300">
                          <CheckCircle2 className="w-3 h-3 mr-1" /> Added
                        </Badge>
                      ) : (
                        <Button
                          size="sm"
                          onClick={() => addTest(t.id)}
                          disabled={addingId === t.id}
                        >
                          {addingId === t.id ? <Loader2 className="w-3 h-3 animate-spin" /> : <Plus className="w-3 h-3 mr-1" />}
                          Add
                        </Button>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          )}
        </div>

        {/* Current items */}
        <div className="flex-1 overflow-y-auto">
          <Label className="text-sm font-semibold">Tests in this package ({items.length})</Label>
          {loading ? (
            <div className="flex justify-center py-6">
              <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
            </div>
          ) : items.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-6">
              No tests yet. Search above to add some.
            </p>
          ) : (
            <div className="space-y-2 mt-2">
              {items.map((item, idx) => (
                <div key={item.id} className="flex items-center gap-3 p-3 border rounded-lg bg-slate-50">
                  <div className="w-8 h-8 rounded-full bg-primary text-primary-foreground flex items-center justify-center text-sm font-bold shrink-0">
                    {idx + 1}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium truncate">{item.test.title}</div>
                    <div className="text-xs text-muted-foreground">
                      {item.test.testCategory || item.test.examType} · {item.test.durationMin} min
                      {item.test._count?.items ? ` · ${item.test._count.items} questions` : ""}
                    </div>
                  </div>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="text-rose-500"
                    onClick={() => removeTest(item)}
                  >
                    <X className="w-4 h-4" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-3 border-t">
          <Button variant="outline" onClick={() => onOpenChange(false)}>Done</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
