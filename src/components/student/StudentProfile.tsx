"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { LogOut, Award, BookOpen, Mail, Phone, Shield } from "lucide-react";
import { useAuthStore } from "@/stores/auth";

export function StudentProfile() {
  const { user, logout } = useAuthStore();
  const initials = (user?.name || user?.email || "S").slice(0, 2).toUpperCase();

  return (
    <div className="space-y-4 max-w-2xl mx-auto">
      <Card>
        <CardContent className="p-6 text-center">
          <Avatar className="h-20 w-20 mx-auto mb-3">
            <AvatarFallback className="bg-gradient-to-br from-emerald-500 to-teal-600 text-white text-xl">
              {initials}
            </AvatarFallback>
          </Avatar>
          <h2 className="text-xl font-bold">{user?.name}</h2>
          <p className="text-sm text-muted-foreground capitalize">{user?.role.toLowerCase()}</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-base">Account info</CardTitle></CardHeader>
        <CardContent className="space-y-3 text-sm">
          <div className="flex items-center gap-3">
            <Mail className="h-4 w-4 text-muted-foreground" />
            <span>{user?.email}</span>
          </div>
          {user?.phone && (
            <div className="flex items-center gap-3">
              <Phone className="h-4 w-4 text-muted-foreground" />
              <span>{user.phone}</span>
            </div>
          )}
          <div className="flex items-center gap-3">
            <Shield className="h-4 w-4 text-muted-foreground" />
            <span>Verified</span>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-base">Achievements</CardTitle></CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-3 text-center">
            <div>
              <Award className="h-6 w-6 mx-auto text-amber-500 mb-1" />
              <div className="text-xs text-muted-foreground">Streak</div>
              <div className="font-semibold">7 days</div>
            </div>
            <div>
              <BookOpen className="h-6 w-6 mx-auto text-emerald-500 mb-1" />
              <div className="text-xs text-muted-foreground">Lessons</div>
              <div className="font-semibold">12</div>
            </div>
            <div>
              <Award className="h-6 w-6 mx-auto text-rose-500 mb-1" />
              <div className="text-xs text-muted-foreground">Badges</div>
              <div className="font-semibold">3</div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Button variant="outline" className="w-full" onClick={logout}>
        <LogOut className="mr-2 h-4 w-4" /> Sign out
      </Button>

      <p className="text-center text-xs text-muted-foreground">
        EduPlatform v1.0.0 · <a className="underline" href="#/privacy">Privacy</a> · <a className="underline" href="#/terms">Terms</a>
      </p>
    </div>
  );
}
