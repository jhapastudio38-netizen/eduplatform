"use client";

/**
 * Teacher Login / Signup — /teacher
 *
 * Two tabs:
 *   • "Sign in" — existing teacher with username/email + password
 *   • "Create account" — teacher with an admin-issued invite code
 *
 * Students are NOT supported on this page — they sign up via the app's
 * OTP or password-signup flow at "/".
 */

import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { School, ArrowRight, KeyRound, User as UserIcon, Ticket, Mail, Phone, UserPlus, LogIn } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { useToast } from "@/hooks/use-toast";
import { useAuthStore } from "@/stores/auth";

export default function TeacherLoginPage() {
  const { toast } = useToast();
  const { fetchUser } = useAuthStore();

  // Prefill invite code from ?invite=DK-XXXX-XXXX and prefill name/email if the
  // admin tied them to the invite when generating it.
  const [initialInviteCode, setInitialInviteCode] = useState("");
  const [initialName, setInitialName] = useState("");
  const [initialEmail, setInitialEmail] = useState("");

  useEffect(() => {
    if (typeof window === "undefined") return;
    const params = new URLSearchParams(window.location.search);
    const code = params.get("invite");
    if (code) setInitialInviteCode(code.toUpperCase());
    const name = params.get("name");
    if (name) setInitialName(name);
    const email = params.get("email");
    if (email) setInitialEmail(email);
  }, []);

  // ─── Sign-in state ────────────────────────────────────────────────────────
  const [signinUsername, setSigninUsername] = useState("");
  const [signinPassword, setSigninPassword] = useState("");
  const [signinBusy, setSigninBusy] = useState(false);

  async function signin(e?: React.FormEvent) {
    e?.preventDefault();
    if (!signinUsername.trim() || !signinPassword) {
      toast({ title: "Error", description: "Enter your username and password", variant: "destructive" });
      return;
    }
    setSigninBusy(true);
    try {
      const res = await fetch("/api/auth/credentials", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: signinUsername, password: signinPassword }),
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
      setSigninBusy(false);
    }
  }

  // ─── Signup state ─────────────────────────────────────────────────────────
  const [signupName, setSignupName] = useState(initialName);
  const [signupEmail, setSignupEmail] = useState(initialEmail);
  const [signupUsername, setSignupUsername] = useState("");
  const [signupPassword, setSignupPassword] = useState("");
  const [signupPhone, setSignupPhone] = useState("");
  const [signupInviteCode, setSignupInviteCode] = useState(initialInviteCode);
  const [signupBusy, setSignupBusy] = useState(false);

  // When prefill values arrive late (after first render), update the form
  useEffect(() => {
    if (initialName && !signupName) setSignupName(initialName);
    if (initialEmail && !signupEmail) setSignupEmail(initialEmail);
    if (initialInviteCode && !signupInviteCode) setSignupInviteCode(initialInviteCode);
  }, [initialName, initialEmail, initialInviteCode, signupName, signupEmail, signupInviteCode]);

  async function signup(e?: React.FormEvent) {
    e?.preventDefault();
    if (!signupName.trim()) { toast({ title: "Error", description: "Name is required", variant: "destructive" }); return; }
    if (!signupEmail.trim()) { toast({ title: "Error", description: "Email is required", variant: "destructive" }); return; }
    if (signupUsername.trim().length < 3) { toast({ title: "Error", description: "Username must be at least 3 characters", variant: "destructive" }); return; }
    if (signupPassword.length < 6) { toast({ title: "Error", description: "Password must be at least 6 characters", variant: "destructive" }); return; }
    if (!signupInviteCode.trim()) { toast({ title: "Error", description: "Invite code is required", variant: "destructive" }); return; }

    setSignupBusy(true);
    try {
      const res = await fetch("/api/auth/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          mode: "teacher",
          name: signupName.trim(),
          email: signupEmail.trim(),
          username: signupUsername.trim().toLowerCase(),
          password: signupPassword,
          phone: signupPhone.trim() || undefined,
          inviteCode: signupInviteCode.trim().toUpperCase(),
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast({ title: "Signup failed", description: data.error || "Could not create account", variant: "destructive" });
        return;
      }
      await fetchUser();
      toast({ title: "Welcome to DreamKorea", description: "Your teacher account is ready." });
      window.location.href = "/";
    } finally {
      setSignupBusy(false);
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

        <Tabs defaultValue="signin" className="w-full">
          <TabsList className="grid w-full grid-cols-2 mb-4">
            <TabsTrigger value="signin"><LogIn className="w-4 h-4 mr-1.5" /> Sign in</TabsTrigger>
            <TabsTrigger value="signup"><UserPlus className="w-4 h-4 mr-1.5" /> Create account</TabsTrigger>
          </TabsList>

          {/* ─── SIGN IN ─── */}
          <TabsContent value="signin">
            <Card>
              <CardHeader>
                <CardTitle>Sign in</CardTitle>
                <CardDescription>
                  Enter your username (or email) and password. Contact admin if you forgot your credentials.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={signin} className="space-y-4">
                  <div>
                    <Label>Username or email</Label>
                    <div className="relative">
                      <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                      <Input
                        className="pl-9"
                        placeholder="teacher.username"
                        value={signinUsername}
                        onChange={(e) => setSigninUsername(e.target.value)}
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
                        value={signinPassword}
                        onChange={(e) => setSigninPassword(e.target.value)}
                        autoComplete="current-password"
                      />
                    </div>
                  </div>
                  <Button type="submit" disabled={signinBusy} className="w-full bg-amber-600 hover:bg-amber-700">
                    {signinBusy ? "Signing in…" : "Sign in"} <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </form>
              </CardContent>
            </Card>
          </TabsContent>

          {/* ─── SIGN UP (teacher with invite code) ─── */}
          <TabsContent value="signup">
            <Card>
              <CardHeader>
                <CardTitle>Create teacher account</CardTitle>
                <CardDescription>
                  Got an invite code from the admin? Fill in your details below to activate your teacher account.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={signup} className="space-y-4">
                  {/* Invite code — highlighted */}
                  <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 space-y-2">
                    <Label className="text-amber-900 font-semibold flex items-center gap-1.5">
                      <Ticket className="w-4 h-4" /> Invite Code *
                    </Label>
                    <Input
                      placeholder="DK-XXXX-XXXX"
                      value={signupInviteCode}
                      onChange={(e) => setSignupInviteCode(e.target.value.toUpperCase())}
                      className="font-mono tracking-wider bg-white"
                      autoComplete="off"
                    />
                    <p className="text-xs text-amber-700">
                      Ask the admin if you don't have one yet.
                    </p>
                  </div>

                  <div>
                    <Label>Full name *</Label>
                    <div className="relative">
                      <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                      <Input
                        className="pl-9"
                        placeholder="Teacher's full name"
                        value={signupName}
                        onChange={(e) => setSignupName(e.target.value)}
                        autoComplete="name"
                      />
                    </div>
                  </div>

                  <div>
                    <Label>Username *</Label>
                    <div className="relative">
                      <UserIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                      <Input
                        className="pl-9"
                        placeholder="e.g. ramesh.k"
                        value={signupUsername}
                        onChange={(e) => setSignupUsername(e.target.value.toLowerCase())}
                        pattern="[a-z0-9._-]+"
                        minLength={3}
                        autoComplete="username"
                      />
                    </div>
                  </div>

                  <div>
                    <Label>Email *</Label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                      <Input
                        className="pl-9"
                        type="email"
                        placeholder="teacher@dreamkorea.com"
                        value={signupEmail}
                        onChange={(e) => setSignupEmail(e.target.value)}
                        autoComplete="email"
                      />
                    </div>
                  </div>

                  <div>
                    <Label>Phone (optional)</Label>
                    <div className="relative">
                      <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                      <Input
                        className="pl-9"
                        placeholder="+977 98XXXXXXXX"
                        value={signupPhone}
                        onChange={(e) => setSignupPhone(e.target.value)}
                        autoComplete="tel"
                      />
                    </div>
                  </div>

                  <div>
                    <Label>Password *</Label>
                    <div className="relative">
                      <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
                      <Input
                        className="pl-9"
                        type="password"
                        placeholder="Min 6 characters"
                        value={signupPassword}
                        onChange={(e) => setSignupPassword(e.target.value)}
                        minLength={6}
                        autoComplete="new-password"
                      />
                    </div>
                  </div>

                  <Button type="submit" disabled={signupBusy} className="w-full bg-amber-600 hover:bg-amber-700">
                    {signupBusy ? "Creating…" : <>Create account <ArrowRight className="ml-2 h-4 w-4" /></>}
                  </Button>
                </form>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        <p className="text-center text-xs text-slate-400 mt-6">
          Don't have an invite code? Contact the admin to request one.
        </p>
      </motion.div>
    </div>
  );
}
