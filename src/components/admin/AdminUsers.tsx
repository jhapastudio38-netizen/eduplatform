"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter,
} from "@/components/ui/dialog";
import { Ban, ShieldCheck, Search, UserPlus, Users, GraduationCap, Crown, UserCheck } from "lucide-react";
import { useToast } from "@/hooks/use-toast";

interface U {
  id: string;
  name: string | null;
  email: string;
  phone: string | null;
  username?: string | null;
  role: "STUDENT" | "TEACHER" | "ADMIN";
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

export function AdminUsers() {
  const { toast } = useToast();
  const [users, setUsers] = useState<U[]>([]);
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<"ALL" | "STUDENT" | "TEACHER" | "ADMIN">("ALL");
  const [createOpen, setCreateOpen] = useState(false);

  // Create teacher form state
  const [newTeacher, setNewTeacher] = useState({
    username: "", password: "", name: "", email: "", phone: "",
  });
  const [creating, setCreating] = useState(false);

  function load() {
    setLoading(true);
    const params = new URLSearchParams();
    if (roleFilter !== "ALL") params.set("role", roleFilter);
    if (search) params.set("q", search);
    fetch(`/api/admin/users?${params.toString()}`)
      .then((r) => r.json())
      .then((d) => {
        setUsers(d.users || []);
        setStats(d.stats || null);
      })
      .finally(() => setLoading(false));
  }
  useEffect(load, [roleFilter, search]);

  async function toggleBan(u: U) {
    await fetch(`/api/admin/users/${u.id}/ban`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ban: !u.isBanned }),
    });
    load();
  }

  async function createTeacher(e: React.FormEvent) {
    e.preventDefault();
    setCreating(true);
    try {
      const res = await fetch("/api/admin/users", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ action: "create_teacher", ...newTeacher }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast({ title: "Error", description: data.error, variant: "destructive" });
        return;
      }
      toast({ title: "Teacher created", description: `Username: ${newTeacher.username}` });
      setCreateOpen(false);
      setNewTeacher({ username: "", password: "", name: "", email: "", phone: "" });
      load();
    } finally {
      setCreating(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">User Management</h1>
          <p className="text-sm text-muted-foreground mt-1">Manage students, teachers, and admins.</p>
        </div>
        <Button onClick={() => setCreateOpen(true)}>
          <UserPlus className="mr-2 h-4 w-4" /> Create Teacher
        </Button>
      </div>

      {/* Stats grid */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
          <StatCard icon={<Users className="h-4 w-4" />} label="Total" value={stats.total} color="bg-slate-100 text-slate-700" />
          <StatCard icon={<GraduationCap className="h-4 w-4" />} label="Students" value={stats.totalStudents} color="bg-blue-50 text-blue-700" />
          <StatCard icon={<UserCheck className="h-4 w-4" />} label="Teachers" value={stats.totalTeachers} color="bg-amber-50 text-amber-700" />
          <StatCard icon={<Crown className="h-4 w-4" />} label="Admins" value={stats.totalAdmins} color="bg-purple-50 text-purple-700" />
          <StatCard icon={<ShieldCheck className="h-4 w-4" />} label="Verified" value={stats.verifiedUsers} color="bg-green-50 text-green-700" />
          <StatCard icon={<Ban className="h-4 w-4" />} label="Banned" value={stats.bannedUsers} color="bg-rose-50 text-rose-700" />
        </div>
      )}

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search by name, email, phone, username…"
            className="pl-9"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="flex gap-1">
          {(["ALL", "STUDENT", "TEACHER", "ADMIN"] as const).map((r) => (
            <Button
              key={r}
              size="sm"
              variant={roleFilter === r ? "default" : "outline"}
              onClick={() => setRoleFilter(r)}
            >
              {r === "ALL" ? "All" : r.charAt(0) + r.slice(1).toLowerCase()}
            </Button>
          ))}
        </div>
      </div>

      {/* User list */}
      <Card>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-8 text-center text-muted-foreground">Loading…</div>
          ) : users.length === 0 ? (
            <div className="p-8 text-center text-muted-foreground">No users found.</div>
          ) : (
            <div className="divide-y">
              {users.map((u) => (
                <div key={u.id} className="flex items-center gap-3 p-3 hover:bg-muted/30">
                  <Avatar>
                    <AvatarFallback className={
                      u.role === "ADMIN" ? "bg-purple-100 text-purple-700" :
                      u.role === "TEACHER" ? "bg-amber-100 text-amber-700" :
                      "bg-blue-100 text-blue-700"
                    }>
                      {(u.name || u.email).slice(0, 2).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <div className="font-medium truncate">{u.name || "Unnamed"}</div>
                      {u.isBanned && <Badge variant="outline" className="text-rose-600 border-rose-300">Banned</Badge>}
                      {!u.isVerified && <Badge variant="outline" className="text-slate-500">Unverified</Badge>}
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
                  <Badge variant={u.role === "ADMIN" ? "destructive" : u.role === "TEACHER" ? "default" : "secondary"}>
                    {u.role.toLowerCase()}
                  </Badge>
                  {u.role !== "ADMIN" && (
                    <Button size="sm" variant="ghost" onClick={() => toggleBan(u)}>
                      {u.isBanned ? <><ShieldCheck className="h-4 w-4 mr-1" /> Unban</> : <><Ban className="h-4 w-4 mr-1 text-rose-500" /> Ban</>}
                    </Button>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Create Teacher dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Teacher Account</DialogTitle>
            <DialogDescription>
              Teachers login at <code className="text-xs bg-muted px-1.5 py-0.5 rounded">/teacher</code> with username + password (no OTP).
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={createTeacher} className="space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>Username *</Label>
                <Input
                  placeholder="e.g. ramesh.k"
                  value={newTeacher.username}
                  onChange={(e) => setNewTeacher({ ...newTeacher, username: e.target.value })}
                  required
                  minLength={3}
                  pattern="[a-zA-Z0-9._-]+"
                />
              </div>
              <div>
                <Label>Password *</Label>
                <Input
                  type="text"
                  placeholder="Min 6 characters"
                  value={newTeacher.password}
                  onChange={(e) => setNewTeacher({ ...newTeacher, password: e.target.value })}
                  required
                  minLength={6}
                />
              </div>
            </div>
            <div>
              <Label>Full name *</Label>
              <Input
                placeholder="Teacher's name"
                value={newTeacher.name}
                onChange={(e) => setNewTeacher({ ...newTeacher, name: e.target.value })}
                required
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label>Email *</Label>
                <Input
                  type="email"
                  placeholder="teacher@dreamkorea.com"
                  value={newTeacher.email}
                  onChange={(e) => setNewTeacher({ ...newTeacher, email: e.target.value })}
                  required
                />
              </div>
              <div>
                <Label>Phone (optional)</Label>
                <Input
                  placeholder="+977 98XXXXXXXX"
                  value={newTeacher.phone}
                  onChange={(e) => setNewTeacher({ ...newTeacher, phone: e.target.value })}
                />
              </div>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setCreateOpen(false)}>Cancel</Button>
              <Button type="submit" disabled={creating}>
                {creating ? "Creating…" : "Create Teacher"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: number; color: string }) {
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
