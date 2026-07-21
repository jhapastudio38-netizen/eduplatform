"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Ban, ShieldCheck } from "lucide-react";

interface U {
  id: string;
  name: string | null;
  email: string;
  phone: string | null;
  role: "STUDENT" | "TEACHER" | "ADMIN";
  isBanned: boolean;
  createdAt: string;
}

export function AdminUsers() {
  const [users, setUsers] = useState<U[]>([]);

  function load() {
    fetch("/api/admin/users").then((r) => r.json()).then((d) => setUsers(d.users || []));
  }
  useEffect(load, []);

  async function toggleBan(u: U) {
    await fetch(`/api/admin/users/${u.id}/ban`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ban: !u.isBanned }),
    });
    load();
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Users ({users.length})</h1>
      <Card>
        <CardContent className="p-0">
          <div className="divide-y">
            {users.map((u) => (
              <div key={u.id} className="flex items-center gap-3 p-3">
                <Avatar>
                  <AvatarFallback className="bg-slate-100 text-slate-700">
                    {(u.name || u.email).slice(0, 2).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <div className="font-medium truncate">{u.name || "Unnamed"}</div>
                  <div className="text-xs text-muted-foreground truncate">{u.email}{u.phone ? ` · ${u.phone}` : ""}</div>
                </div>
                <Badge variant={u.role === "ADMIN" ? "destructive" : u.role === "TEACHER" ? "default" : "secondary"}>
                  {u.role.toLowerCase()}
                </Badge>
                {u.isBanned && <Badge variant="outline" className="text-rose-600">Banned</Badge>}
                <Button size="sm" variant="ghost" onClick={() => toggleBan(u)}>
                  {u.isBanned ? <><ShieldCheck className="h-4 w-4 mr-1" /> Unban</> : <><Ban className="h-4 w-4 mr-1 text-rose-500" /> Ban</>}
                </Button>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
