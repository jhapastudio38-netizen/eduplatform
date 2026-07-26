"use client";
import { useState, useEffect } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Radio, Plus, Trash2, Copy, RefreshCw } from "lucide-react";
import { toast } from "sonner";

interface LiveSession {
  id: string;
  title: string;
  description?: string | null;
  joinCode: string;
  meetingUrl: string;
  credentials?: string | null;
  hostName?: string | null;
  isActive: boolean;
  createdAt: string;
}

export function AdminLiveSessions() {
  const [sessions, setSessions] = useState<LiveSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);

  function load() {
    setLoading(true);
    fetch("/api/admin/live-sessions")
      .then((r) => r.json())
      .then((d) => setSessions(d.sessions || []))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  async function del(id: string) {
    if (!confirm("Delete this live session?")) return;
    await fetch(`/api/admin/live-sessions/${id}`, { method: "DELETE" });
    toast.success("Deleted");
    load();
  }

  async function toggleActive(s: LiveSession) {
    await fetch(`/api/admin/live-sessions/${s.id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ isActive: !s.isActive }),
    });
    load();
  }

  function genCode() {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    let code = "";
    for (let i = 0; i < 6; i++) code += chars[Math.floor(Math.random() * chars.length)];
    return code;
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Live Sessions</h1>
          <p className="text-sm text-muted-foreground">Create a join code — students enter it to get the meeting link</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <Plus className="w-4 h-4 mr-1" /> New Session
        </Button>
      </div>

      {loading ? (
        <p className="text-center py-8 text-muted-foreground">Loading…</p>
      ) : sessions.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Radio className="w-12 h-12 mx-auto mb-3 text-muted-foreground" />
            <p className="text-muted-foreground">No live sessions yet.</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-3">
          {sessions.map((s) => (
            <Card key={s.id}>
              <CardContent className="flex items-center gap-4 py-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h3 className="font-semibold truncate">{s.title}</h3>
                    <Badge variant={s.isActive ? "default" : "secondary"}>
                      {s.isActive ? "Live" : "Ended"}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-3 mt-1">
                    <button
                      onClick={() => { navigator.clipboard.writeText(s.joinCode); toast.success("Code copied"); }}
                      className="flex items-center gap-1 text-sm font-mono font-bold text-primary hover:underline"
                    >
                      {s.joinCode} <Copy className="w-3 h-3" />
                    </button>
                    <span className="text-sm text-muted-foreground truncate">{s.meetingUrl}</span>
                  </div>
                  {s.credentials && <p className="text-xs text-muted-foreground mt-1">{s.credentials}</p>}
                </div>
                <div className="flex items-center gap-2">
                  <Button variant="outline" size="sm" onClick={() => toggleActive(s)}>
                    {s.isActive ? "End" : "Reactivate"}
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => del(s.id)}>
                    <Trash2 className="w-4 h-4 text-red-500" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {createOpen && (
        <CreateSessionDialog
          open={createOpen}
          onOpenChange={setCreateOpen}
          onCreated={() => { setCreateOpen(false); load(); }}
          genCode={genCode}
        />
      )}
    </div>
  );
}

function CreateSessionDialog({ open, onOpenChange, onCreated, genCode }: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onCreated: () => void;
  genCode: () => string;
}) {
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({
    title: "",
    description: "",
    joinCode: "",
    meetingUrl: "",
    credentials: "",
    hostName: "",
  });

  useEffect(() => {
    if (open && !form.joinCode) {
      setForm(f => ({ ...f, joinCode: genCode() }));
    }
  }, [open]);

  async function create() {
    if (!form.title.trim()) { toast.error("Title required"); return; }
    if (!form.joinCode.trim()) { toast.error("Join code required"); return; }
    if (!form.meetingUrl.trim()) { toast.error("Meeting link required"); return; }
    setBusy(true);
    try {
      const res = await fetch("/api/admin/live-sessions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Failed"); return; }
      toast.success(`Session created — code: ${form.joinCode}`);
      onCreated();
    } finally { setBusy(false); }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader><DialogTitle>Create Live Session</DialogTitle></DialogHeader>
        <div className="space-y-3">
          <div>
            <Label>Session Title *</Label>
            <Input value={form.title} onChange={(e) => setForm(f => ({ ...f, title: e.target.value }))} placeholder="e.g. TOPIK Listening Practice" />
          </div>
          <div>
            <Label>Description (optional)</Label>
            <Textarea rows={2} value={form.description} onChange={(e) => setForm(f => ({ ...f, description: e.target.value }))} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>Join Code *</Label>
              <div className="flex gap-2">
                <Input value={form.joinCode} onChange={(e) => setForm(f => ({ ...f, joinCode: e.target.value.toUpperCase() }))} className="font-mono font-bold text-lg" maxLength={10} />
                <Button variant="outline" size="icon" onClick={() => setForm(f => ({ ...f, joinCode: genCode() }))} title="Generate new code">
                  <RefreshCw className="w-4 h-4" />
                </Button>
              </div>
              <p className="text-xs text-muted-foreground mt-1">Students enter this code to join</p>
            </div>
            <div>
              <Label>Host Name (optional)</Label>
              <Input value={form.hostName} onChange={(e) => setForm(f => ({ ...f, hostName: e.target.value }))} placeholder="Teacher name" />
            </div>
          </div>
          <div>
            <Label>Meeting Link *</Label>
            <Input value={form.meetingUrl} onChange={(e) => setForm(f => ({ ...f, meetingUrl: e.target.value }))} placeholder="https://zoom.us/j/... or https://meet.google.com/..." />
          </div>
          <div>
            <Label>Credentials (optional)</Label>
            <Textarea rows={2} value={form.credentials} onChange={(e) => setForm(f => ({ ...f, credentials: e.target.value }))} placeholder="Meeting ID: 123 456 7890, Password: 1234" />
          </div>
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={create} disabled={busy}>{busy ? "Creating…" : "Create Session"}</Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
