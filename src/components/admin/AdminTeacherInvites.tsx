"use client";

/**
 * AdminTeacherInvites — admin generates invite codes that teachers can use to
 * self-sign up at /teacher?invite=<code>. The page shows:
 *   • A "Generate Invite" dialog (preset name/email + expiry)
 *   • A list of all invites with their status (pending / consumed / expired)
 *   • A "Copy signup link" button that copies https://<host>/teacher?invite=CODE
 *   • Delete (revoke) button for unused invites
 */

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import { Ticket, Plus, Copy, Trash2, Loader2, Check, Clock, XCircle } from "lucide-react";
import { toast } from "sonner";
import { useAuthStore } from "@/stores/auth";

interface Invite {
  id: string;
  code: string;
  presetName: string | null;
  presetEmail: string | null;
  createdBy: string;
  consumedBy: string | null;
  consumedAt: string | null;
  expiresAt: string;
  isRevoked: boolean;
  createdAt: string;
}

export function AdminTeacherInvites() {
  const { user } = useAuthStore();
  const isAdmin = user?.role === "ADMIN";
  const [invites, setInvites] = useState<Invite[]>([]);
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [justCopied, setJustCopied] = useState<string | null>(null);

  function load() {
    setLoading(true);
    fetch("/api/admin/teacher-invites")
      .then((r) => r.json())
      .then((d) => setInvites(d.invites || []))
      .catch(() => setInvites([]))
      .finally(() => setLoading(false));
  }
  useEffect(load, []);

  function copySignupLink(code: string) {
    const link = `${window.location.origin}/teacher?invite=${encodeURIComponent(code)}`;
    navigator.clipboard.writeText(link);
    setJustCopied(code);
    toast.success("Signup link copied — share it with the teacher");
    setTimeout(() => setJustCopied(null), 2000);
  }

  async function deleteInvite(id: string) {
    if (!confirm("Delete this invite code? It can no longer be used.")) return;
    const res = await fetch(`/api/admin/teacher-invites?id=${encodeURIComponent(id)}`, { method: "DELETE" });
    if (!res.ok) {
      toast.error("Delete failed");
      return;
    }
    toast.success("Invite deleted");
    load();
  }

  function status(i: Invite): { label: string; cls: string; icon: any } {
    if (i.consumedBy) return { label: "Used", cls: "bg-emerald-50 text-emerald-700 border-emerald-300", icon: Check };
    if (i.isRevoked) return { label: "Revoked", cls: "bg-rose-50 text-rose-700 border-rose-300", icon: XCircle };
    if (new Date(i.expiresAt) < new Date()) return { label: "Expired", cls: "bg-slate-100 text-slate-500", icon: Clock };
    return { label: "Pending", cls: "bg-amber-50 text-amber-700 border-amber-300", icon: Clock };
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Ticket className="w-6 h-6" />
            Teacher Invite Codes ({invites.length})
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Generate single-use codes that let teachers self-sign up at{" "}
            <code className="text-xs bg-muted px-1.5 py-0.5 rounded">/teacher</code>.
            Share the signup link with the teacher — they fill in their own password.
          </p>
        </div>
        {isAdmin && (
          <Button size="lg" onClick={() => setCreateOpen(true)}>
            <Plus className="w-4 h-4 mr-1" /> Generate Invite
          </Button>
        )}
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : invites.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Ticket className="w-12 h-12 mx-auto mb-3 text-muted-foreground opacity-40" />
            <p className="text-muted-foreground">
              No invite codes yet. Click &ldquo;Generate Invite&rdquo; to create one for a new teacher.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2">
          {invites.map((i) => {
            const s = status(i);
            const Icon = s.icon;
            return (
              <Card key={i.id}>
                <CardContent className="flex flex-wrap items-center gap-3 p-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <code className="font-mono text-base font-bold tracking-wider bg-muted px-2 py-1 rounded">{i.code}</code>
                      <Badge variant="outline" className={s.cls}>
                        <Icon className="w-3 h-3 mr-1" /> {s.label}
                      </Badge>
                    </div>
                    <div className="text-xs text-muted-foreground mt-1 flex flex-wrap gap-x-3 gap-y-0.5">
                      {i.presetName && <span>For: {i.presetName}</span>}
                      {i.presetEmail && <span>Email: {i.presetEmail}</span>}
                      <span>Expires: {new Date(i.expiresAt).toLocaleString()}</span>
                      {i.consumedAt && <span>Used: {new Date(i.consumedAt).toLocaleString()}</span>}
                    </div>
                  </div>
                  <div className="flex gap-2 shrink-0">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => copySignupLink(i.code)}
                      disabled={Boolean(i.consumedBy) || i.isRevoked}
                    >
                      {justCopied === i.code ? (
                        <><Check className="w-3 h-3 mr-1" /> Copied</>
                      ) : (
                        <><Copy className="w-3 h-3 mr-1" /> Copy link</>
                      )}
                    </Button>
                    {isAdmin && !i.consumedBy && (
                      <Button size="sm" variant="ghost" className="text-rose-500" onClick={() => deleteInvite(i.id)}>
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {createOpen && (
        <CreateInviteDialog
          onOpenChange={setCreateOpen}
          onCreated={() => { setCreateOpen(false); load(); }}
        />
      )}
    </div>
  );
}

function CreateInviteDialog({ onOpenChange, onCreated }: {
  onOpenChange: (v: boolean) => void;
  onCreated: () => void;
}) {
  const [form, setForm] = useState({
    presetName: "",
    presetEmail: "",
    expiresInDays: "14",
  });
  const [busy, setBusy] = useState(false);

  async function create() {
    setBusy(true);
    try {
      const res = await fetch("/api/admin/teacher-invites", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          presetName: form.presetName.trim() || undefined,
          presetEmail: form.presetEmail.trim() || undefined,
          expiresInDays: parseInt(form.expiresInDays) || 14,
        }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Failed to create invite"); return; }
      toast.success("Invite generated — share the link with the teacher");
      onCreated();
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={true} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Generate Teacher Invite</DialogTitle>
          <DialogDescription>
            Optionally pre-fill the teacher's name and email — they'll be locked to that email when signing up.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <Label>Preset name (optional)</Label>
            <Input
              value={form.presetName}
              onChange={(e) => setForm((f) => ({ ...f, presetName: e.target.value }))}
              placeholder="Teacher's full name"
            />
          </div>
          <div>
            <Label>Preset email (optional)</Label>
            <Input
              type="email"
              value={form.presetEmail}
              onChange={(e) => setForm((f) => ({ ...f, presetEmail: e.target.value }))}
              placeholder="teacher@dreamkorea.com"
            />
            <p className="text-xs text-muted-foreground mt-1">
              If set, only this email can use the invite.
            </p>
          </div>
          <div>
            <Label>Expires in</Label>
            <Select value={form.expiresInDays} onValueChange={(v) => setForm((f) => ({ ...f, expiresInDays: v }))}>
              <SelectTrigger><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectItem value="1">1 day</SelectItem>
                <SelectItem value="7">7 days</SelectItem>
                <SelectItem value="14">14 days (default)</SelectItem>
                <SelectItem value="30">30 days</SelectItem>
                <SelectItem value="90">90 days</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={create} disabled={busy}>
            {busy ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Plus className="w-4 h-4 mr-1" />}
            {busy ? "Generating…" : "Generate"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
