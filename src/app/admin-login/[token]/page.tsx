"use client";

/**
 * Admin Login — ID + Password (no OTP, fixed credentials).
 * Accessible only at a secret URL: /admin-login/<token>
 * The token is verified server-side, then admin enters ID + password.
 */

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { Shield, Lock, ArrowRight, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import { useAuthStore } from "@/stores/auth";

export default function AdminLoginPage() {
  const params = useParams<{ token: string }>();
  const router = useRouter();
  const { toast } = useToast();
  const { fetchUser } = useAuthStore();
  const [verifying, setVerifying] = useState(true);
  const [validToken, setValidToken] = useState(false);
  const [adminId, setAdminId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    async function verify() {
      try {
        const res = await fetch(`/api/admin/login-link?token=${params.token}`);
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

  async function login() {
    if (!adminId.trim() || !password.trim()) {
      toast({ title: "Error", description: "Enter admin ID and password", variant: "destructive" });
      return;
    }
    setBusy(true);
    try {
      const res = await fetch("/api/admin/login-credentials", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ adminId, password }),
      });
      const data = await res.json();
      if (!res.ok) {
        toast({ title: "Access denied", description: data.error || "Invalid credentials", variant: "destructive" });
        return;
      }
      await fetchUser();
      toast({ title: "Welcome", description: "Admin access granted." });
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
    <div className="min-h-screen grid place-items-center bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-4">
      <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-md">
        <div className="text-center mb-6">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-rose-500 to-pink-600 text-white shadow-lg shadow-rose-500/20 mb-3">
            <Shield className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-bold text-white">DreamKorea Admin</h1>
          <p className="text-sm text-slate-400 mt-1">Secure access — authorized personnel only</p>
        </div>

        <Card className="border-slate-800 bg-slate-900/80 backdrop-blur">
          <CardHeader>
            <CardTitle className="text-white">Admin Login</CardTitle>
            <CardDescription className="text-slate-400">Enter your admin ID and password</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label className="text-slate-200">Admin ID</Label>
              <Input
                className="bg-slate-800 border-slate-700 text-white"
                placeholder="Enter admin ID"
                value={adminId}
                onChange={(e) => setAdminId(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && login()}
              />
            </div>
            <div>
              <Label className="text-slate-200">Password</Label>
              <div className="relative">
                <Input
                  className="bg-slate-800 border-slate-700 text-white pr-10"
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && login()}
                />
                <button
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>
            <Button onClick={login} disabled={busy} className="w-full bg-rose-600 hover:bg-rose-700">
              {busy ? "Verifying…" : "Login"} <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </CardContent>
        </Card>

        <p className="text-center text-xs text-slate-500 mt-6">
          All access is logged. Unauthorized access attempts are prosecuted.
        </p>
      </motion.div>
    </div>
  );
}
