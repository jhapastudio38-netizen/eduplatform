"use client";

import { useEffect, useRef, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Radio, Square, Copy, Check, Users, MessageCircle } from "lucide-react";
import { toast } from "sonner";
import { io, Socket } from "socket.io-client";

export function TeacherLive() {
  const [live, setLive] = useState<{ id: string; roomCode: string; title: string } | null>(null);
  const [title, setTitle] = useState("");
  const [desc, setDesc] = useState("");
  const [subject, setSubject] = useState("");
  const [attendees, setAttendees] = useState(0);
  const [chat, setChat] = useState<{ from: string; msg: string }[]>([]);
  const [msg, setMsg] = useState("");
  const [copied, setCopied] = useState(false);
  const sockRef = useRef<Socket | null>(null);

  async function startLive() {
    if (!title.trim()) {
      toast.error("Give your class a title");
      return;
    }
    const res = await fetch("/api/teacher/classes/start", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ title, description: desc, subject, durationMin: 60 }),
    });
    const d = await res.json();
    if (!res.ok) { toast.error(d.error || "Failed to start"); return; }
    setLive(d.liveClass);
    toast.success("You are now live! Share the room code with students.");
    // Connect to live WS
    const sock = io("/?XTransformPort=3003");
    sock.on("connect", () => {
      sock.emit("join", { role: "teacher", room: d.liveClass.roomCode });
    });
    sock.on("attendee_count", (n: number) => setAttendees(n));
    sock.on("chat", (m: { from: string; msg: string }) => {
      setChat((c) => [...c, m]);
    });
    sockRef.current = sock;
  }

  async function endLive() {
    if (!live) return;
    await fetch(`/api/teacher/classes/${live.id}/end`, { method: "POST" });
    sockRef.current?.disconnect();
    setLive(null);
    setAttendees(0);
    setChat([]);
    toast.success("Class ended.");
  }

  function sendMsg() {
    if (!msg.trim() || !sockRef.current) return;
    sockRef.current.emit("chat", { from: "Teacher", msg });
    setChat((c) => [...c, { from: "Teacher", msg }]);
    setMsg("");
  }

  function copyCode() {
    if (!live) return;
    navigator.clipboard.writeText(live.roomCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  if (live) {
    return (
      <div className="space-y-4 max-w-3xl mx-auto">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="inline-flex h-3 w-3 rounded-full bg-rose-500 animate-pulse" />
            <span className="font-semibold">Live: {live.title}</span>
          </div>
          <Button variant="destructive" size="sm" onClick={endLive}>
            <Square className="mr-1 h-3 w-3" /> End
          </Button>
        </div>

        <div className="grid sm:grid-cols-3 gap-3">
          <Card>
            <CardContent className="p-4">
              <div className="text-xs text-muted-foreground">Room code</div>
              <div className="flex items-center justify-between mt-1">
                <span className="text-lg font-mono font-bold">{live.roomCode}</span>
                <Button size="icon" variant="ghost" onClick={copyCode}>
                  {copied ? <Check className="h-4 w-4 text-emerald-600" /> : <Copy className="h-4 w-4" />}
                </Button>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-xs text-muted-foreground">Attendees</div>
              <div className="flex items-center gap-2 mt-1">
                <Users className="h-5 w-5 text-emerald-600" />
                <span className="text-lg font-bold">{attendees}</span>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="p-4">
              <div className="text-xs text-muted-foreground">Status</div>
              <div className="text-lg font-bold text-rose-600">● LIVE</div>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <MessageCircle className="h-4 w-4" /> Live chat
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-64 overflow-y-auto bg-muted/30 rounded-lg p-3 space-y-2 mb-3">
              {chat.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center pt-8">
                  Messages from students will appear here.
                </p>
              ) : chat.map((c, i) => (
                <div key={i} className={`text-sm ${c.from === "Teacher" ? "text-right" : ""}`}>
                  <span className="font-semibold mr-1">{c.from}:</span>
                  <span>{c.msg}</span>
                </div>
              ))}
            </div>
            <div className="flex gap-2">
              <Input
                placeholder="Message students…"
                value={msg}
                onChange={(e) => setMsg(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && sendMsg()}
              />
              <Button onClick={sendMsg}>Send</Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto">
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Radio className="h-4 w-4" /> Start a live class
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div>
            <Label>Class title</Label>
            <Input placeholder="Algebra 101 — Linear equations" value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div>
            <Label>Subject</Label>
            <Input placeholder="Mathematics" value={subject} onChange={(e) => setSubject(e.target.value)} />
          </div>
          <div>
            <Label>Description</Label>
            <Textarea rows={3} placeholder="What will you cover?" value={desc} onChange={(e) => setDesc(e.target.value)} />
          </div>
          <Button className="w-full" onClick={startLive}>
            <Radio className="mr-2 h-4 w-4" /> Go live now
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
