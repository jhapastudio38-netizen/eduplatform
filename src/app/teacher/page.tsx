"use client";

/**
 * Teacher Login — /teacher
 * Teachers enter email/phone + OTP.
 * Admin creates teacher accounts first, then teachers can login here.
 * No student access on this page.
 */

import { useState } from "react";
import { motion } from "framer-motion";
import { School, ArrowRight, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { useAuthStore } from "@/stores/auth";

export default function TeacherLoginPage() {
  const { toast } = useToast();
  const { fetchUser } = useAuthStore();
  const [contact, setContact] = useState("");
  const [code, setCode] = useState("");
  const [step, setStep] = useState<"contact" | "otp">("contact");
  const [busy, setBusy] = useState(false);

  async function requestOtp() {
    if (!contact.trim()) {
      toast({ title: "Error", description: "Enter your email or phone", variant: "destructive" });
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
        toast({ title: "Error", description: data.error, variant: "destructive" });
        return;
      }
      toast({ title: "Code sent", description: "Check your email for the verification code." });
      setStep("otp");
    } finally {
      setBusy(false);
    }
  }

  async function verifyOtp() {
    if (code.length < 6) return;
    setBusy(true);
    try {
      const res = await fetch("/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ contact, code, role: "TEACHER" }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast({ title: "Error", description: data.error, variant: "destructive" });
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
            <CardTitle>{step === "contact" ? "Sign in" : "Verify identity"}</CardTitle>
            <CardDescription>
              {step === "contact"
                ? "Enter your email or phone to receive a verification code."
                : `Enter the 6-digit code sent to ${contact}. Check your email.`}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {step === "contact" ? (
              <div className="space-y-4">
                <div>
                  <Label>Email or phone</Label>
                  <Input
                    placeholder="teacher@dreamkoreansmartclass.com"
                    value={contact}
                    onChange={(e) => setContact(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && requestOtp()}
                  />
                </div>
                <Button onClick={requestOtp} disabled={busy} className="w-full bg-amber-600 hover:bg-amber-700">
                  {busy ? "Sending…" : "Send code"} <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </div>
            ) : (
              <div className="space-y-4">
                <div>
                  <Label>6-digit code</Label>
                  <Input
                    placeholder="000000"
                    maxLength={6}
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                    onKeyDown={(e) => e.key === "Enter" && verifyOtp()}
                  />
                </div>
                <Button onClick={verifyOtp} disabled={busy || code.length < 6} className="w-full bg-amber-600 hover:bg-amber-700">
                  {busy ? "Verifying…" : "Verify and continue"}
                </Button>
                <Button variant="ghost" size="sm" className="w-full" onClick={() => setStep("contact")}>
                  <ArrowLeft className="mr-1 h-3 w-3" /> Change email/phone
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        <p className="text-center text-xs text-slate-400 mt-6">
          Don't have an account? Contact the admin to create one for you.
        </p>
      </motion.div>
    </div>
  );
}
