"use client";

import { create } from "zustand";
import type { Role } from "@/types";

export interface AuthUser {
  id: string;
  name: string | null;
  email: string;
  phone: string | null;
  role: Role;
  avatarUrl?: string | null;
}

interface AuthState {
  user: AuthUser | null;
  loading: boolean;
  fetchUser: () => Promise<void>;
  setUser: (u: AuthUser | null) => void;
  logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  loading: true,
  fetchUser: async () => {
    set({ loading: true });
    try {
      const res = await fetch("/api/auth/me", { cache: "no-store" });
      if (!res.ok) {
        set({ user: null });
        return;
      }
      const data = (await res.json()) as { user: AuthUser | null };
      set({ user: data.user });
    } catch {
      set({ user: null });
    } finally {
      set({ loading: false });
    }
  },
  setUser: (u) => set({ user: u }),
  logout: async () => {
    await fetch("/api/auth/logout", { method: "POST" });
    set({ user: null });
  },
}));
