"use client";

/**
 * AuthFlow — multi-step animated login:
 * 1. Role pick
 * 2. Contact (phone / Gmail) entry
 * 3. OTP entry (6-digit, input-otp)
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
import { GraduationCap, School, ShieldCheck, ArrowRight, ArrowLeft, Mail, Phone } from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import type { Role } from "@/types";

type Step = "role" | "contact" | "otp";

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
              {step === "contact" && "Sign in or sign up"}
              {step === "otp" && "Verify it's you"}
            </CardTitle>
            <CardDescription>
              {step === "role" && "Select how you'll use DreamKorea SmartClass today."}
              {step === "contact" && "Use your Gmail or phone number — we'll send a one-time code."}
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

              {step === "contact" && (
                <motion.div
                  key="contact"
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
