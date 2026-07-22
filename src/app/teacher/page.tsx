"use client";

/**
 * Teacher Login — /teacher
 * Teachers login with username (or email) + password.
 * Admin creates teacher accounts first, then teachers can login here.
 * No OTP for teachers — students use OTP via the app.
 */

import { useState } from "react";
import { motion } from "framer-motion";
import { School, ArrowRight, KeyRound, User as UserIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { useAuthStore } from "@/stores/auth";

export default function TeacherLoginPage() {
  const { toast } = useToast();
  const { fetchUser } = useAuthStore();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);

  async function login(e?: React.FormEvent) {
    e?.preventDefault();
    if (!username.trim() || !password) {
      toast({ title: "Error", description: "Enter your username and password", variant: "destructive" });
      return;
    }
    setBusy(true);
    try {
      const res = await fetch("/api/auth/credentials", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast({ title: "Login failed", description: data.error || "Invalid credentials", variant: "destructive" });
        return;
      }
      if (data.user.role !== "TEACHER" && data.user.role !== "ADMIN") {
        toast({ title: "Access denied", description: "This page is for teachers only.", variant: "destructive" });
        return;
      }
      await fetchUser();
      toast({ title: "Welcome", description: "Teacher access granted." });
      window.location.href = "/";
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-screen grid place-items-center bg-gradient-to-br from-amber-50 via-white to-orange-50 p-4">
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-md">
        <div className="text-center mb-6">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-500 to-orange-600 text-white shadow-lg shadow-amber-500/20 mb-3">
            <School className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900">DreamKorea Teacher</h1>
          <p className="text-sm text-slate-500 mt-1">Teachers only — students use the mobile app</p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Sign in</CardTitle>
            <CardDescription>
              Enter your username and password. Contact admin if you forgot your credentials.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={login} className="space-y-4">
              <div>
                <Label>Username or email</Label>
                <div className="relative">
                  <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <Input
                    className="pl-9"
                    placeholder="teacher.username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    autoComplete="username"
                  />
                </div>
              </div>
              <div>
                <Label>Password</Label>
                <div className="relative">
                  <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <Input
                    className="pl-9"
                    type="password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    autoComplete="current-password"
                  />
                </div>
              </div>
              <Button type="submit" disabled={busy} className="w-full bg-amber-600 hover:bg-amber-700">
                {busy ? "Signing in…" : "Sign in"} <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="text-center text-xs text-slate-400 mt-6">
          Don't have an account? Contact the admin to create one for you.
        </p>
      </motion.div>
    </div>
  );
}
