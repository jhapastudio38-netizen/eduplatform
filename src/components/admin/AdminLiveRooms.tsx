"use client";

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Radio, Users, Plus, StopCircle, Copy, Check, Mic } from "lucide-react";
import { toast } from "sonner";

interface Room {
  id: string;
  title: string;
  description?: string;
  subject?: string;
  roomCode: string;
  isLive: boolean;
  maxStudents: number;
  audioOnly: boolean;
  startedAt?: string;
  _count?: { attendees: number };
}

export function AdminLiveRooms() {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [copied, setCopied] = useState<string | null>(null);
  const [form, setForm] = useState({ title: "", description: "", subject: "", maxStudents: 50, audioOnly: true });

  function load() {
    fetch("/api/admin/live-rooms").then(r => r.json()).then(d => setRooms(d.rooms || [])).catch(() => {});
  }
  useEffect(load, []);

  async function create() {
    if (!form.title.trim()) { toast.error("Title required"); return; }
    setBusy(true);
    const res = await fetch("/api/admin/live-rooms", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...form, maxStudents: Number(form.maxStudents), startNow: true }),
    });
    setBusy(false);
    if (!res.ok) { const d = await res.json(); toast.error(d.error || "Failed"); return; }
    toast.success("Live room created!");
    setOpen(false);
    setForm({ title: "", description: "", subject: "", maxStudents: 50, audioOnly: true });
    load();
  }

  async function endRoom(id: string) {
    if (!confirm("End this live room? Students will be disconnected.")) return;
    await fetch(`/api/admin/live-rooms/${id}/end`, { method: "POST" });
    toast.success("Room ended");
    load();
  }

  function copyCode(code: string) {
    navigator.clipboard.writeText(code);
    setCopied(code);
    setTimeout(() => setCopied(null), 1500);
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Live Rooms ({rooms.length})</h1>
          <p className="text-sm text-muted-foreground">Create real-time audio rooms — students join with a 6-char code.</p>
        </div>
        <Button onClick={() => setOpen(true)}><Plus className="mr-1 h-4 w-4" /> Create Room</Button>
      </div>

      <div className="space-y-2">
        {rooms.length === 0 ? (
          <Card><CardContent className="p-8 text-center text-muted-foreground">
            <Radio className="h-10 w-10 mx-auto mb-2 opacity-40" />
            <p>No live rooms. Create one to start a real-time class.</p>
          </CardContent></Card>
        ) : rooms.map(room => (
          <Card key={room.id} className={room.isLive ? "border-rose-300" : ""}>
            <CardContent className="p-4 flex items-center gap-4">
              <div className={`h-12 w-12 rounded-xl grid place-items-center shrink-0 ${room.isLive ? "bg-rose-100" : "bg-slate-100"}`}>
                <Radio className={`h-5 w-5 ${room.isLive ? "text-rose-600 animate-pulse" : "text-slate-400"}`} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="font-semibold truncate">{room.title}</div>
                {room.subject && <div className="text-xs text-muted-foreground">{room.subject}</div>}
                <div className="flex items-center gap-2 mt-1">
                  {room.isLive && <Badge className="text-xs bg-rose-100 text-rose-700">LIVE</Badge>}
                  {room.audioOnly && <Badge variant="outline" className="text-xs"><Mic className="h-3 w-3 mr-1" />Audio</Badge>}
                  <span className="text-xs text-muted-foreground flex items-center gap-1">
                    <Users className="h-3 w-3" /> {room._count?.attendees || 0}/{room.maxStudents}
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <div className="text-center">
                  <div className="text-xs text-muted-foreground">Room Code</div>
                  <div className="font-mono font-bold text-lg tracking-wider">{room.roomCode}</div>
                </div>
                <Button size="icon" variant="ghost" onClick={() => copyCode(room.roomCode)}>
                  {copied === room.roomCode ? <Check className="h-4 w-4 text-emerald-600" /> : <Copy className="h-4 w-4" />}
                </Button>
                {room.isLive && (
                  <Button size="sm" variant="destructive" onClick={() => endRoom(room.id)}>
                    <StopCircle className="h-3 w-3 mr-1" /> End
                  </Button>
                )}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Create Live Room</DialogTitle></DialogHeader>
          <div className="space-y-3">
            <div><Label>Room Title</Label><Input value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder="TOPIK II Speaking Practice" /></div>
            <div><Label>Description</Label><Textarea rows={2} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} /></div>
            <div><Label>Subject</Label><Input value={form.subject} onChange={e => setForm(f => ({ ...f, subject: e.target.value }))} placeholder="Korean Language" /></div>
            <div className="grid grid-cols-2 gap-3">
              <div><Label>Max Students</Label><Input type="number" value={form.maxStudents} onChange={e => setForm(f => ({ ...f, maxStudents: Number(e.target.value) }))} /></div>
              <div className="flex items-end pb-2">
                <label className="flex items-center gap-2 text-sm">
                  <input type="checkbox" checked={form.audioOnly} onChange={e => setForm(f => ({ ...f, audioOnly: e.target.checked }))} className="h-4 w-4" />
                  Audio only (no video)
                </label>
              </div>
            </div>
            <Button onClick={create} disabled={busy} className="w-full bg-rose-600 hover:bg-rose-700">
              {busy ? "Creating…" : "Start Live Room"}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
