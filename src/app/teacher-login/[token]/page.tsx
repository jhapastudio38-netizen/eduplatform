"use client";

/**
 * Secure Teacher Login — unguessable URL.
 * URL pattern: /teacher-login/<secure-token>
 */

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { School, Lock, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { useAuthStore } from "@/stores/auth";

export default function TeacherLoginPage() {
  const params = useParams<{ token: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const { fetchUser } = useAuthStore();
  const [verifying, setVerifying] = useState(true);
  const [validToken, setValidToken] = useState(false);
  const [contact, setContact] = useState("");
  const [step, setStep] = useState<"contact" | "otp">("contact");
  const [otp, setOtp] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    async function verify() {
      try {
        const res = await fetch(`/api/teacher/login-link?token=${params.token}`);
        if (res.ok) setValidToken(true);
        else router.push("/404");
      } catch {
        router.push("/404");
      } finally {
        setVerifying(false);
      }
    }
    verify();
  }, [params.token, router]);

  async function requestOtp() {
    if (!contact.trim()) return;
    setBusy(true);
    try {
      const res = await fetch("/api/auth/request-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contact }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast({ title: "Error", description: data.error, variant: "destructive" });
        return;
      }
      toast({ title: "Code sent", description: "Check your email or phone." });
      setStep("otp");
    } finally {
      setBusy(false);
    }
  }

  async function verifyOtp() {
    if (otp.length < 6) return;
    setBusy(true);
    try {
      const res = await fetch("/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contact, code: otp, role: "TEACHER" }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast({ title: "Error", description: data.error, variant: "destructive" });
        return;
      }
      if (data.user.role !== "TEACHER" && data.user.role !== "ADMIN") {
        toast({
          title: "Access denied",
          description: "This login URL is for teachers only.",
          variant: "destructive",
        });
        return;
      }
      await fetchUser();
      toast({ title: "Welcome", description: "Teacher access granted." });
      window.location.href = "/";
    } finally {
      setBusy(false);
    }
  }

  if (verifying) {
    return (
      <div className="min-h-screen grid place-items-center bg-slate-950">
        <div className="flex flex-col items-center gap-3 text-slate-400">
          <Lock className="h-8 w-8 animate-pulse" />
          <p className="text-sm">Verifying secure link…</p>
        </div>
      </div>
    );
  }

  if (!validToken) return null;

  return (
    <div className="min-h-screen grid place-items-center bg-gradient-to-br from-slate-950 via-amber-950/20 to-slate-950 p-4">
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md"
      >
        <div className="text-center mb-6">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-500 to-orange-600 text-white shadow-lg shadow-amber-500/20 mb-3">
            <School className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-bold text-white">DreamKorea Teacher</h1>
          <p className="text-sm text-slate-400 mt-1">Secure access — teachers only</p>
        </div>

        <Card className="border-slate-800 bg-slate-900/80 backdrop-blur">
          <CardHeader>
            <CardTitle className="text-white">
              {step === "contact" ? "Sign in" : "Verify identity"}
            </CardTitle>
            <CardDescription className="text-slate-400">
              {step === "contact"
                ? "Enter your teacher email or phone to receive a one-time code."
                : `Enter the 6-digit code sent to ${contact}.`}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <AnimatePresence mode="wait">
              {step === "contact" ? (
                <motion.div key="contact" className="space-y-3">
                  <div>
                    <Label className="text-slate-200">Email or phone</Label>
                    <Input
                      className="bg-slate-800 border-slate-700 text-white"
                      placeholder="teacher@dreamkoreasmartclass.com"
                      value={contact}
                      onChange={(e) => setContact(e.target.value)}
                      onKeyDown={(e) => e.key === "Enter" && requestOtp()}
                    />
                  </div>
                  <Button onClick={requestOtp} disabled={busy || !contact} className="w-full bg-amber-600 hover:bg-amber-700">
                    {busy ? "Sending…" : "Send code"}
                  </Button>
                </motion.div>
              ) : (
                <motion.div key="otp" className="space-y-3">
                  <div>
                    <Label className="text-slate-200">6-digit code</Label>
                    <Input
                      className="bg-slate-800 border-slate-700 text-white text-center tracking-[0.5em] text-lg"
                      placeholder="000000"
                      maxLength={6}
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, ""))}
                      onKeyDown={(e) => e.key === "Enter" && verifyOtp()}
                    />
                  </div>
                  <Button onClick={verifyOtp} disabled={busy || otp.length < 6} className="w-full bg-amber-600 hover:bg-amber-700">
                    {busy ? "Verifying…" : "Verify and enter"}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="w-full text-slate-400"
                    onClick={() => setStep("contact")}
                  >
                    <ArrowLeft className="mr-1 h-3 w-3" /> Change contact
                  </Button>
                </motion.div>
              )}
            </AnimatePresence>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}
