"use client";

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
} from "@/components/ui/tabs";
import {
  GraduationCap, UserCog, Plus, Trash2, Ban, ShieldCheck, Search, Loader2, Eye, Crown, Clock, Award, TrendingUp,
} from "lucide-react";
import { toast } from "sonner";
import { useAuthStore } from "@/stores/auth";

type Role = "STUDENT" | "TEACHER" | "ADMIN";

interface U {
  id: string;
  name: string | null;
  email: string;
  phone: string | null;
  username?: string | null;
  role: Role;
  isBanned: boolean;
  isVerified: boolean;
  signupMethod?: string | null;
  createdAt: string;
  lastActiveAt?: string | null;
}

interface Stats {
  total: number;
  totalStudents: number;
  totalTeachers: number;
  totalAdmins: number;
  bannedUsers: number;
  verifiedUsers: number;
}

export function AdminUsers({ role }: { role?: Role }) {
  const { user: currentUser } = useAuthStore();
  const isAdmin = currentUser?.role === "ADMIN";

  // Pre-select the requested tab; admins can switch freely, teachers are locked to students
  const initialTab: "students" | "teachers" =
    role === "TEACHER" && isAdmin ? "teachers" : "students";
  const [tab, setTab] = useState<"students" | "teachers">(initialTab);

  const [users, setUsers] = useState<U[]>([]);
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [createOpen, setCreateOpen] = useState(false);
  const [detailUserId, setDetailUserId] = useState<string | null>(null);

  // Active role filter — derived from tab
  const activeRole: Role = tab === "teachers" ? "TEACHER" : "STUDENT";

  function load() {
    setLoading(true);
    const params = new URLSearchParams();
    params.set("role", activeRole);
    if (search) params.set("q", search);
    fetch(`/api/admin/users?${params.toString()}`)
      .then((r) => r.json())
      .then((d) => {
        setUsers(d.users || []);
        setStats(d.stats || null);
      })
      .catch(() => {
        setUsers([]);
        setStats(null);
      })
      .finally(() => setLoading(false));
  }

  // Reload when tab or search changes
  useEffect(() => {
    load();
  }, [tab, search]);

  // Sync external role navigation
  useEffect(() => {
    if (role === "TEACHER" && isAdmin) setTab("teachers");
    else if (role === "STUDENT") setTab("students");
  }, [role, isAdmin]);

  async function toggleBan(u: U) {
    try {
      const res = await fetch(`/api/admin/users/${u.id}/ban`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ban: !u.isBanned }),
      });
      if (!res.ok) {
        const d = await res.json();
        toast.error(d.error || "Action failed");
        return;
      }
      toast.success(u.isBanned ? "User unbanned" : "User banned");
      load();
    } catch {
      toast.error("Action failed");
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">User Management</h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isAdmin
              ? "Manage students and teachers. Create new teacher accounts."
              : "View student accounts."}
          </p>
        </div>
        {/* Only admins can create teachers */}
        {isAdmin && (
          <Button size="lg" onClick={() => setCreateOpen(true)}>
            <Plus className="w-4 h-4 mr-1" /> Create Teacher
          </Button>
        )}
      </div>

      {/* Stats */}
      {stats && isAdmin && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <StatCard
            icon={<GraduationCap className="h-4 w-4" />}
            label="Students"
            value={stats.totalStudents}
            color="bg-emerald-50 text-emerald-700"
          />
          <StatCard
            icon={<UserCog className="h-4 w-4" />}
            label="Teachers"
            value={stats.totalTeachers}
            color="bg-amber-50 text-amber-700"
          />
          <StatCard
            icon={<ShieldCheck className="h-4 w-4" />}
            label="Verified"
            value={stats.verifiedUsers}
            color="bg-blue-50 text-blue-700"
          />
          <StatCard
            icon={<Ban className="h-4 w-4" />}
            label="Banned"
            value={stats.bannedUsers}
            color="bg-rose-50 text-rose-700"
          />
        </div>
      )}

      {/* Tabs */}
      <Tabs value={tab} onValueChange={(v) => setTab(v as "students" | "teachers")}>
        <TabsList>
          <TabsTrigger value="students">
            <GraduationCap className="w-4 h-4 mr-1" /> Students
          </TabsTrigger>
          {/* Teachers tab — only admins can see the teacher roster */}
          {isAdmin && (
            <TabsTrigger value="teachers">
              <UserCog className="w-4 h-4 mr-1" /> Teachers
            </TabsTrigger>
          )}
        </TabsList>

        {/* Search */}
        <div className="relative my-3 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search by name, email, phone…"
            className="pl-9"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <TabsContent value="students">
          <UserList
            users={users}
            loading={loading}
            emptyText="No students found."
            canBan={isAdmin}
            onToggleBan={toggleBan}
            onViewDetails={(u) => setDetailUserId(u.id)}
          />
        </TabsContent>

        {isAdmin && (
          <TabsContent value="teachers">
            <UserList
              users={users}
              loading={loading}
              emptyText="No teachers found. Create one with the button above."
              canBan={isAdmin}
              onToggleBan={toggleBan}
              accent="amber"
              onViewDetails={(u) => setDetailUserId(u.id)}
            />
          </TabsContent>
        )}
      </Tabs>

      {createOpen && (
        <CreateTeacherDialog
          open={createOpen}
          onOpenChange={setCreateOpen}
          onCreated={() => { setCreateOpen(false); setTab("teachers"); load(); }}
          isAdmin={isAdmin}
        />
      )}

      {detailUserId && (
        <UserDetailsDialog
          userId={detailUserId}
          onOpenChange={(v) => { if (!v) setDetailUserId(null); }}
          onUserUpdated={load}
        />
      )}
    </div>
  );
}

function UserList({
  users, loading, emptyText, canBan, onToggleBan, onViewDetails, accent = "emerald",
}: {
  users: U[];
  loading: boolean;
  emptyText: string;
  canBan: boolean;
  onToggleBan: (u: U) => void;
  onViewDetails: (u: U) => void;
  accent?: "emerald" | "amber";
}) {
  const fallbackBg =
    accent === "amber" ? "bg-amber-100 text-amber-700" : "bg-emerald-100 text-emerald-700";

  return (
    <Card>
      <CardContent className="p-0">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : users.length === 0 ? (
          <div className="p-8 text-center text-muted-foreground text-sm">{emptyText}</div>
        ) : (
          <div className="divide-y max-h-[70vh] overflow-y-auto">
            {users.map((u) => (
              <div key={u.id} className="flex items-center gap-3 p-3 hover:bg-muted/30">
                <Avatar>
                  <AvatarFallback className={fallbackBg}>
                    {(u.name || u.email).slice(0, 2).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <div className="font-medium truncate">{u.name || "Unnamed"}</div>
                    {u.isBanned && (
                      <Badge variant="outline" className="text-rose-600 border-rose-300">
                        Banned
                      </Badge>
                    )}
                    {!u.isVerified && (
                      <Badge variant="outline" className="text-slate-500">
                        Unverified
                      </Badge>
                    )}
                  </div>
                  <div className="text-xs text-muted-foreground truncate flex flex-wrap gap-x-2">
                    <span>{u.email}</span>
                    {u.phone && <span>· {u.phone}</span>}
                    {u.username && <span className="font-mono">@{u.username}</span>}
                  </div>
                  <div className="text-[10px] text-muted-foreground/70 mt-0.5">
                    Joined {new Date(u.createdAt).toLocaleDateString()}
                    {u.signupMethod && ` · via ${u.signupMethod.replace(/_/g, " ")}`}
                    {u.lastActiveAt && ` · last active ${new Date(u.lastActiveAt).toLocaleDateString()}`}
                  </div>
                </div>
                <Badge
                  variant={
                    u.role === "ADMIN" ? "destructive" : u.role === "TEACHER" ? "default" : "secondary"
                  }
                >
                  {u.role.toLowerCase()}
                </Badge>
                <Button size="sm" variant="outline" onClick={() => onViewDetails(u)}>
                  <Eye className="h-4 w-4 mr-1" /> Details
                </Button>
                {canBan && u.role !== "ADMIN" && (
                  <Button size="sm" variant="ghost" onClick={() => onToggleBan(u)}>
                    {u.isBanned ? (
                      <>
                        <ShieldCheck className="h-4 w-4 mr-1" /> Unban
                      </>
                    ) : (
                      <>
                        <Ban className="h-4 w-4 mr-1 text-rose-500" /> Ban
                      </>
                    )}
                  </Button>
                )}
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function CreateTeacherDialog({
  open, onOpenChange, onCreated, isAdmin,
}: {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onCreated: () => void;
  isAdmin: boolean;
}) {
  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    username: "",
    password: "",
    canCreateTeachers: false,
  });
  const [busy, setBusy] = useState(false);

  async function create() {
    if (!form.name.trim()) { toast.error("Name is required"); return; }
    if (!form.email.trim()) { toast.error("Email is required"); return; }
    if (form.password.length < 6) { toast.error("Password must be at least 6 characters"); return; }
    if (form.username.length < 3) { toast.error("Username must be at least 3 characters"); return; }
    setBusy(true);
    try {
      const res = await fetch("/api/admin/users", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: "create_teacher",
          name: form.name.trim(),
          email: form.email.trim(),
          phone: form.phone.trim(),
          username: form.username.trim().toLowerCase(),
          password: form.password,
          canCreateTeachers: form.canCreateTeachers,
        }),
      });
      const d = await res.json();
      if (!res.ok) { toast.error(d.error || "Failed to create teacher"); return; }
      toast.success("Teacher created");
      setForm({ name: "", email: "", phone: "", username: "", password: "", canCreateTeachers: false });
      onCreated();
    } catch {
      toast.error("Failed to create teacher");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create Teacher Account</DialogTitle>
          <DialogDescription>
            Teachers log in at{" "}
            <code className="text-xs bg-muted px-1.5 py-0.5 rounded">/teacher</code>{" "}
            with username + password (no OTP).
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <Label className="text-sm font-semibold">Full Name *</Label>
              <Input
                placeholder="Teacher's full name"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                className="h-12 text-base"
              />
            </div>
            <div>
              <Label className="text-sm font-semibold">Username *</Label>
              <Input
                placeholder="e.g. ramesh.k"
                value={form.username}
                onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
                pattern="[a-zA-Z0-9._-]+"
                minLength={3}
                className="h-12 text-base"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <Label className="text-sm font-semibold">Email *</Label>
              <Input
                type="email"
                placeholder="teacher@dreamkorea.com"
                value={form.email}
                onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                className="h-12 text-base"
              />
            </div>
            <div>
              <Label className="text-sm font-semibold">Phone</Label>
              <Input
                placeholder="+977 98XXXXXXXX"
                value={form.phone}
                onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
                className="h-12 text-base"
              />
            </div>
          </div>

          <div>
            <Label className="text-sm font-semibold">Password *</Label>
            <Input
              type="text"
              placeholder="Min 6 characters"
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
              minLength={6}
              className="h-12 text-base"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Teacher will use this password to log in. You can share it with them directly.
            </p>
          </div>

          {/* "Can create teachers" toggle — only admins see this */}
          {isAdmin && (
            <div className="flex items-center justify-between p-3 border rounded-lg bg-slate-50">
              <div>
                <Label className="text-sm font-semibold">Can create teachers</Label>
                <p className="text-xs text-muted-foreground mt-0.5">
                  Grant this teacher the ability to create other teacher accounts.
                </p>
              </div>
              <Switch
                checked={form.canCreateTeachers}
                onCheckedChange={(v) => setForm((f) => ({ ...f, canCreateTeachers: v }))}
              />
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" size="lg" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button size="lg" onClick={create} disabled={busy}>
            {busy ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Plus className="w-4 h-4 mr-2" />}
            {busy ? "Creating…" : "Create Teacher"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function StatCard({ icon, label, value, color }: {
  icon: React.ReactNode;
  label: string;
  value: number;
  color: string;
}) {
  return (
    <Card className="p-3">
      <div className="flex items-center gap-2">
        <div className={`h-8 w-8 rounded-lg grid place-items-center ${color}`}>{icon}</div>
        <div>
          <div className="text-xs text-muted-foreground">{label}</div>
          <div className="text-lg font-bold leading-tight">{value}</div>
        </div>
      </div>
    </Card>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// USER DETAILS DIALOG — shows full user info + stats + subscription + submissions
// ═══════════════════════════════════════════════════════════════════════════

interface UserDetail {
  user: {
    id: string;
    name: string | null;
    email: string;
    phone: string | null;
    username?: string | null;
    role: string;
    avatarUrl?: string | null;
    isBanned: boolean;
    isVerified: boolean;
    signupMethod?: string | null;
    createdAt: string;
    lastActiveAt?: string | null;
    subscriptionType?: string | null;
    subscribedUntil?: string | null;
    subscriptionPrice?: number | null;
    isSubscribed: boolean;
  };
  stats: {
    totalExamsTaken: number;
    totalCorrectAnswers: number;
    totalQuestionsAnswered: number;
    averageScore: number;
    studyStreakDays: number;
    lastStudyDate?: string | null;
  };
  submissions: Array<{
    id: string;
    testId: string;
    score: number | null;
    maxScore: number | null;
    graded: boolean;
    submittedAt: string | null;
    test: { id: string; title: string; testCategory: string | null; examType: string };
  }>;
  totalSubmissions: number;
}

function UserDetailsDialog({
  userId, onOpenChange, onUserUpdated,
}: {
  userId: string;
  onOpenChange: (v: boolean) => void;
  onUserUpdated: () => void;
}) {
  const [detail, setDetail] = useState<UserDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [subscribing, setSubscribing] = useState(false);

  function load() {
    setLoading(true);
    fetch(`/api/admin/users/${userId}`)
      .then((r) => r.json())
      .then((d) => setDetail(d))
      .catch(() => setDetail(null))
      .finally(() => setLoading(false));
  }

  useEffect(load, [userId]);

  async function subscribe(type: "monthly" | "yearly" | "lifetime") {
    setSubscribing(true);
    try {
      const until = new Date();
      if (type === "monthly") until.setMonth(until.getMonth() + 1);
      else if (type === "yearly") until.setFullYear(until.getFullYear() + 1);
      else if (type === "lifetime") until.setFullYear(until.getFullYear() + 100);

      const res = await fetch(`/api/admin/users/${userId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          subscriptionType: type,
          subscribedUntil: until.toISOString(),
        }),
      });
      if (!res.ok) { toast.error("Failed to subscribe"); return; }
      toast.success(`Subscribed (${type})`);
      load();
      onUserUpdated();
    } finally {
      setSubscribing(false);
    }
  }

  async function unsubscribe() {
    setSubscribing(true);
    try {
      const res = await fetch(`/api/admin/users/${userId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          subscriptionType: null,
          subscribedUntil: null,
        }),
      });
      if (!res.ok) { toast.error("Failed to unsubscribe"); return; }
      toast.success("Unsubscribed");
      load();
      onUserUpdated();
    } finally {
      setSubscribing(false);
    }
  }

  return (
    <Dialog open={true} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Eye className="w-5 h-5" /> User Details
          </DialogTitle>
        </DialogHeader>

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : !detail ? (
          <div className="py-8 text-center text-muted-foreground">User not found.</div>
        ) : (
          <div className="space-y-5">
            {/* Account Info */}
            <div className="flex items-start gap-4 p-4 bg-slate-50 rounded-lg">
              <Avatar className="h-16 w-16">
                <AvatarFallback className="bg-emerald-100 text-emerald-700 text-xl">
                  {(detail.user.name || detail.user.email).slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="text-lg font-bold">{detail.user.name || "Unnamed"}</h3>
                  <Badge variant={detail.user.role === "ADMIN" ? "destructive" : detail.user.role === "TEACHER" ? "default" : "secondary"}>
                    {detail.user.role.toLowerCase()}
                  </Badge>
                  {detail.user.isBanned && <Badge variant="outline" className="text-rose-600 border-rose-300">Banned</Badge>}
                  {detail.user.isSubscribed && (
                    <Badge className="bg-purple-500">
                      <Crown className="w-3 h-3 mr-1" /> Subscriber
                    </Badge>
                  )}
                </div>
                <div className="text-sm text-muted-foreground mt-1 space-y-0.5">
                  <div>{detail.user.email}</div>
                  {detail.user.phone && <div>{detail.user.phone}</div>}
                  {detail.user.username && <div className="font-mono text-xs">@{detail.user.username}</div>}
                  <div className="text-xs text-muted-foreground/70">
                    Joined {new Date(detail.user.createdAt).toLocaleDateString()}
                    {detail.user.signupMethod && ` · via ${detail.user.signupMethod.replace(/_/g, " ")}`}
                  </div>
                </div>
              </div>
            </div>

            {/* Stats Grid */}
            <div>
              <h4 className="text-sm font-semibold mb-2 flex items-center gap-1.5">
                <TrendingUp className="w-4 h-4" /> Study Stats
              </h4>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                <div className="p-3 bg-blue-50 rounded-lg">
                  <div className="text-xs text-blue-600 font-medium">Exams Taken</div>
                  <div className="text-xl font-bold text-blue-700">{detail.stats.totalExamsTaken}</div>
                </div>
                <div className="p-3 bg-emerald-50 rounded-lg">
                  <div className="text-xs text-emerald-600 font-medium">Correct Answers</div>
                  <div className="text-xl font-bold text-emerald-700">{detail.stats.totalCorrectAnswers}</div>
                </div>
                <div className="p-3 bg-amber-50 rounded-lg">
                  <div className="text-xs text-amber-600 font-medium">Avg Score</div>
                  <div className="text-xl font-bold text-amber-700">{Math.round(detail.stats.averageScore)}%</div>
                </div>
                <div className="p-3 bg-purple-50 rounded-lg">
                  <div className="text-xs text-purple-600 font-medium">Streak</div>
                  <div className="text-xl font-bold text-purple-700">{detail.stats.studyStreakDays}d</div>
                </div>
              </div>
            </div>

            {/* Subscription */}
            <div>
              <h4 className="text-sm font-semibold mb-2 flex items-center gap-1.5">
                <Crown className="w-4 h-4" /> Subscription
              </h4>
              {detail.user.isSubscribed ? (
                <div className="p-3 bg-purple-50 border border-purple-200 rounded-lg">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-semibold text-purple-700 capitalize">
                        {detail.user.subscriptionType} Subscription
                      </div>
                      <div className="text-xs text-purple-600 flex items-center gap-1 mt-0.5">
                        <Clock className="w-3 h-3" />
                        Expires: {detail.user.subscribedUntil ? new Date(detail.user.subscribedUntil).toLocaleDateString() : "—"}
                      </div>
                    </div>
                    <Button size="sm" variant="outline" onClick={unsubscribe} disabled={subscribing}>
                      Unsubscribe
                    </Button>
                  </div>
                </div>
              ) : (
                <div className="p-3 bg-slate-50 border rounded-lg">
                  <div className="text-sm text-muted-foreground mb-2">Not subscribed. Subscribers get unlimited exam retakes.</div>
                  <div className="flex gap-2 flex-wrap">
                    <Button size="sm" variant="outline" onClick={() => subscribe("monthly")} disabled={subscribing}>
                      Monthly (1 month)
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => subscribe("yearly")} disabled={subscribing}>
                      Yearly (1 year)
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => subscribe("lifetime")} disabled={subscribing}>
                      Lifetime
                    </Button>
                  </div>
                </div>
              )}
            </div>

            {/* Recent Submissions */}
            <div>
              <h4 className="text-sm font-semibold mb-2 flex items-center gap-1.5">
                <Award className="w-4 h-4" /> Recent Submissions ({detail.totalSubmissions} total)
              </h4>
              {detail.submissions.length === 0 ? (
                <div className="text-sm text-muted-foreground p-3 bg-slate-50 rounded-lg">
                  No exam submissions yet.
                </div>
              ) : (
                <div className="max-h-60 overflow-y-auto divide-y border rounded-lg">
                  {detail.submissions.map((s) => (
                    <div key={s.id} className="flex items-center gap-3 p-2.5 text-sm">
                      <div className="flex-1 min-w-0">
                        <div className="font-medium truncate">{s.test.title}</div>
                        <div className="text-xs text-muted-foreground">
                          {s.test.testCategory || s.test.examType}
                          {s.submittedAt && ` · ${new Date(s.submittedAt).toLocaleDateString()}`}
                        </div>
                      </div>
                      <Badge variant="outline">
                        {s.score ?? "—"}/{s.maxScore ?? "—"}
                      </Badge>
                      {s.graded ? (
                        <Badge className="bg-emerald-500">Graded</Badge>
                      ) : (
                        <Badge variant="secondary">Pending</Badge>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
