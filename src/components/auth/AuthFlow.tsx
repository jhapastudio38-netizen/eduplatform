"use client";

/**
 * AuthFlow — multi-step animated login:
 * 1. Role pick
 * 2a. Contact (phone / Gmail) entry  → OTP path (default)
 * 2b. Email + password  → password path (toggle on contact screen)
 * 3. OTP entry (6-digit, input-otp) — only on OTP path
 * 4. Success → trigger store refresh
 *
 * No next/router needed — the parent AppShell swaps to the role app when
 * the user state changes.
 */

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { GraduationCap, School, ShieldCheck, ArrowRight, ArrowLeft, Mail, Phone, KeyRound, UserPlus, LogIn } from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import type { Role } from "@/types";

type Step = "role" | "contact" | "otp";
type AuthMode = "otp" | "password";

const ROLES: { id: Role; title: string; desc: string; icon: typeof GraduationCap; color: string }[] = [
  {
    id: "STUDENT",
    title: "Student",
    desc: "Learn, take tests, track progress",
    icon: GraduationCap,
    color: "from-emerald-500 to-teal-500",
  },
  {
    id: "TEACHER",
    title: "Teacher",
    desc: "Go live, teach, monitor students",
    icon: School,
    color: "from-amber-500 to-orange-500",
  },
  {
    id: "ADMIN",
    title: "Admin",
    desc: "Manage chapters, lessons, content",
    icon: ShieldCheck,
    color: "from-rose-500 to-pink-500",
  },
];

export function AuthFlow() {
  const [step, setStep] = useState<Step>("role");
  const [role, setRole] = useState<Role>("STUDENT");
  const [contact, setContact] = useState("");
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);
  // Auth mode toggle — students can use OTP (default) or email+password
  const [authMode, setAuthMode] = useState<AuthMode>("otp");
  // Password mode state
  const [pwEmail, setPwEmail] = useState("");
  const [pwPassword, setPwPassword] = useState("");
  const [pwName, setPwName] = useState("");
  const [pwPhone, setPwPhone] = useState("");
  const [pwIsSignup, setPwIsSignup] = useState(false);
  const { fetchUser } = useAuthStore();

  async function requestOtp() {
    if (!contact.trim()) {
      toast.error("Enter your email or phone number");
      return;
    }
    setBusy(true);
    try {
      const res = await fetch("/api/auth/request-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contact }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast.error(data.error || "Failed to send code");
        return;
      }
      toast.success("Code sent. Check your inbox or SMS.");
      toast.success("Code sent. Check your email inbox.");
      setStep("otp");
    } finally {
      setBusy(false);
    }
  }

  async function verifyOtp() {
    if (code.length !== 6) {
      toast.error("Enter the 6-digit code");
      return;
    }
    setBusy(true);
    try {
      const res = await fetch("/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contact, code, role, name }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast.error(data.error || "Verification failed");
        return;
      }
      toast.success("Welcome! Redirecting…");
      await fetchUser();
    } finally {
      setBusy(false);
    }
  }

  // ─── Password sign-in / sign-up ────────────────────────────────────────────
  async function passwordSubmit() {
    if (!pwEmail.trim()) { toast.error("Enter your email"); return; }
    if (!pwPassword) { toast.error("Enter your password"); return; }
    if (pwIsSignup && pwPassword.length < 6) { toast.error("Password must be at least 6 characters"); return; }
    if (pwIsSignup && !pwName.trim()) { toast.error("Enter your name"); return; }

    setBusy(true);
    try {
      if (pwIsSignup) {
        // Student signup with email + password
        const res = await fetch("/api/auth/signup", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            mode: "student",
            name: pwName.trim(),
            email: pwEmail.trim(),
            password: pwPassword,
            phone: pwPhone.trim() || undefined,
          }),
        });
        const data = await res.json();
        if (!res.ok) {
          toast.error(data.error || "Signup failed");
          return;
        }
        toast.success("Account created — welcome!");
        await fetchUser();
      } else {
        // Login with credentials — students can use email + password too
        const res = await fetch("/api/auth/credentials", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: pwEmail.trim(), password: pwPassword }),
        });
        const data = await res.json();
        if (!res.ok) {
          toast.error(data.error || "Login failed");
          return;
        }
        toast.success("Welcome back!");
        await fetchUser();
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="min-h-[100dvh] flex items-center justify-center p-4 bg-gradient-to-br from-slate-50 via-white to-slate-100">
      <div className="w-full max-w-md">
        {/* Logo header */}
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="text-center mb-6"
        >
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg shadow-emerald-500/20 mb-3">
            <GraduationCap className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight">DreamKorea SmartClass</h1>
          <p className="text-sm text-muted-foreground mt-1">
            One app. Three roles. Endless learning.
          </p>
        </motion.div>

        <Card className="border-border/60 shadow-xl">
          <CardHeader>
            <CardTitle className="text-xl">
              {step === "role" && "Choose your role"}
              {step === "contact" && (authMode === "otp" ? "Sign in or sign up" : (pwIsSignup ? "Create student account" : "Sign in with password"))}
              {step === "otp" && "Verify it's you"}
            </CardTitle>
            <CardDescription>
              {step === "role" && "Select how you'll use DreamKorea SmartClass today."}
              {step === "contact" && authMode === "otp" && "Use your Gmail or phone number — we'll send a one-time code."}
              {step === "contact" && authMode === "password" && (pwIsSignup ? "Create a student account with email + password." : "Sign in with your email + password.")}
              {step === "otp" && `We sent a 6-digit code to ${contact}.`}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <AnimatePresence mode="wait">
              {step === "role" && (
                <motion.div
                  key="role"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  className="space-y-3"
                >
                  {ROLES.map((r) => {
                    const Icon = r.icon;
                    const active = role === r.id;
                    return (
                      <button
                        key={r.id}
                        onClick={() => setRole(r.id)}
                        className={`w-full flex items-center gap-4 p-4 rounded-xl border-2 transition-all text-left ${
                          active
                            ? "border-primary bg-primary/5 shadow-sm"
                            : "border-border hover:border-primary/40 hover:bg-muted/40"
                        }`}
                      >
                        <div className={`h-10 w-10 rounded-lg bg-gradient-to-br ${r.color} text-white grid place-items-center shrink-0`}>
                          <Icon className="h-5 w-5" />
                        </div>
                        <div className="flex-1">
                          <div className="font-semibold">{r.title}</div>
                          <div className="text-xs text-muted-foreground">{r.desc}</div>
                        </div>
                        <div
                          className={`h-5 w-5 rounded-full border-2 transition-all ${
                            active ? "border-primary bg-primary" : "border-muted"
                          }`}
                        />
                      </button>
                    );
                  })}
                  <Button className="w-full mt-2" onClick={() => setStep("contact")}>
                    Continue <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </motion.div>
              )}

              {step === "contact" && authMode === "otp" && (
                <motion.div
                  key="contact-otp"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  className="space-y-4"
                >
                  <div className="space-y-2">
                    <Label htmlFor="contact">Email or phone</Label>
                    <div className="relative">
                      {contact.includes("@") ? (
                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      ) : (
                        <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      )}
                      <Input
                        id="contact"
                        autoFocus
                        inputMode="email"
                        placeholder="you@gmail.com  or  +97798XXXXXXXX"
                        className="pl-9"
                        value={contact}
                        onChange={(e) => setContact(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && requestOtp()}
                      />
                    </div>
                    <p className="text-xs text-muted-foreground">
                      New here? We'll create your account automatically.
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="name">Display name (optional)</Label>
                    <Input
                      id="name"
                      placeholder="Your name"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                    />
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" onClick={() => setStep("role")}>
                      <ArrowLeft className="mr-2 h-4 w-4" /> Back
                    </Button>
                    <Button className="flex-1" onClick={requestOtp} disabled={busy}>
                      {busy ? "Sending…" : "Send code"} <ArrowRight className="ml-2 h-4 w-4" />
                    </Button>
                  </div>

                  {/* Toggle to password mode */}
                  <div className="pt-2 border-t text-center">
                    <p className="text-xs text-muted-foreground mb-2">Prefer a password?</p>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="w-full"
                      onClick={() => { setAuthMode("password"); setPwIsSignup(false); }}
                    >
                      <KeyRound className="w-4 h-4 mr-1.5" /> Sign in with email + password
                    </Button>
                  </div>
                </motion.div>
              )}

              {step === "contact" && authMode === "password" && (
                <motion.div
                  key="contact-password"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  className="space-y-4"
                >
                  {/* Tabs: Login | Signup */}
                  <div className="grid grid-cols-2 gap-1 p-1 bg-muted rounded-lg">
                    <button
                      className={`flex items-center justify-center gap-1.5 py-2 rounded-md text-sm font-medium transition ${!pwIsSignup ? "bg-white shadow-sm text-foreground" : "text-muted-foreground"}`}
                      onClick={() => setPwIsSignup(false)}
                    >
                      <LogIn className="w-4 h-4" /> Sign in
                    </button>
                    <button
                      className={`flex items-center justify-center gap-1.5 py-2 rounded-md text-sm font-medium transition ${pwIsSignup ? "bg-white shadow-sm text-foreground" : "text-muted-foreground"}`}
                      onClick={() => setPwIsSignup(true)}
                    >
                      <UserPlus className="w-4 h-4" /> Sign up
                    </button>
                  </div>

                  {pwIsSignup && (
                    <div className="space-y-2">
                      <Label htmlFor="pw-name">Full name</Label>
                      <Input
                        id="pw-name"
                        placeholder="Your name"
                        value={pwName}
                        onChange={(e) => setPwName(e.target.value)}
                      />
                    </div>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="pw-email">Email</Label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        id="pw-email"
                        type="email"
                        autoFocus
                        placeholder="you@gmail.com"
                        className="pl-9"
                        value={pwEmail}
                        onChange={(e) => setPwEmail(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && passwordSubmit()}
                      />
                    </div>
                  </div>

                  {pwIsSignup && (
                    <div className="space-y-2">
                      <Label htmlFor="pw-phone">Phone (optional)</Label>
                      <div className="relative">
                        <Phone className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                          id="pw-phone"
                          placeholder="+977 98XXXXXXXX"
                          className="pl-9"
                          value={pwPhone}
                          onChange={(e) => setPwPhone(e.target.value)}
                        />
                      </div>
                    </div>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="pw-pass">Password</Label>
                    <div className="relative">
                      <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        id="pw-pass"
                        type="password"
                        placeholder={pwIsSignup ? "Min 6 characters" : "••••••••"}
                        className="pl-9"
                        value={pwPassword}
                        onChange={(e) => setPwPassword(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && passwordSubmit()}
                        autoComplete={pwIsSignup ? "new-password" : "current-password"}
                      />
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <Button variant="outline" onClick={() => { setStep("role"); setAuthMode("otp"); }}>
                      <ArrowLeft className="mr-2 h-4 w-4" /> Back
                    </Button>
                    <Button className="flex-1" onClick={passwordSubmit} disabled={busy}>
                      {busy ? "Please wait…" : (pwIsSignup ? "Create account" : "Sign in")}
                      <ArrowRight className="ml-2 h-4 w-4" />
                    </Button>
                  </div>

                  {/* Toggle back to OTP mode */}
                  <div className="pt-2 border-t text-center">
                    <p className="text-xs text-muted-foreground mb-2">No password yet?</p>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="w-full"
                      onClick={() => setAuthMode("otp")}
                    >
                      <Mail className="w-4 h-4 mr-1.5" /> Use email / phone OTP instead
                    </Button>
                  </div>
                </motion.div>
              )}

              {step === "otp" && (
                <motion.div
                  key="otp"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  className="space-y-4"
                >
                  <div className="space-y-2">
                    <Label>Enter 6-digit code</Label>
                    <div className="flex justify-center py-2">
                      <InputOTP
                        maxLength={6}
                        value={code}
                        onChange={(v) => setCode(v)}
                      >
                        <InputOTPGroup>
                          <InputOTPSlot index={0} />
                          <InputOTPSlot index={1} />
                          <InputOTPSlot index={2} />
                          <InputOTPSlot index={3} />
                          <InputOTPSlot index={4} />
                          <InputOTPSlot index={5} />
                        </InputOTPGroup>
                      </InputOTP>
                    </div>
                  </div>
                  <Button className="w-full" onClick={verifyOtp} disabled={busy || code.length !== 6}>
                    {busy ? "Verifying…" : "Verify & continue"}
                  </Button>
                  <div className="flex items-center justify-between text-sm">
                    <button
                      className="text-muted-foreground hover:text-foreground transition"
                      onClick={() => setStep("contact")}
                    >
                      <ArrowLeft className="inline h-4 w-4 mr-1" /> Change contact
                    </button>
                    <button
                      className="text-primary hover:underline"
                      onClick={requestOtp}
                      disabled={busy}
                    >
                      Resend code
                    </button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </CardContent>
        </Card>

        <p className="text-center text-xs text-muted-foreground mt-6">
          By continuing you agree to our{" "}
          <a className="underline" href="#/terms">Terms</a> and{" "}
          <a className="underline" href="#/privacy">Privacy Policy</a>.
        </p>
      </div>
    </div>
  );
}
