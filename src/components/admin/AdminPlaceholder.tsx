"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Plus, FileText, ArrowRight, Sparkles } from "lucide-react";
import type { LucideIcon } from "lucide-react";

interface PlaceholderProps {
  title: string;
  description: string;
  icon?: LucideIcon;
}

export function AdminPlaceholder({ title, description, icon: Icon = FileText }: PlaceholderProps) {
  return (
    <div className="space-y-6 max-w-5xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
          <p className="text-sm text-slate-500 mt-1">{description}</p>
        </div>
        <Button>
          <Plus className="mr-1 h-4 w-4" /> Add New
        </Button>
      </div>

      {/* Coming soon banner */}
      <Card className="border-emerald-200 bg-gradient-to-br from-emerald-50 to-teal-50">
        <CardContent className="p-8">
          <div className="flex items-start gap-4">
            <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 grid place-items-center text-white shrink-0">
              <Sparkles className="h-6 w-6" />
            </div>
            <div className="flex-1">
              <h2 className="text-lg font-bold text-slate-900">Module Ready for Activation</h2>
              <p className="text-sm text-slate-600 mt-1">
                The <strong>{title}</strong> module is scaffolded and integrated into the database schema.
                Full CRUD UI is being deployed in the next iteration. The data model, API endpoints,
                and security rules are already in place — only the management interface needs to be wired up.
              </p>
              <div className="flex gap-2 mt-4">
                <Button variant="outline" size="sm">
                  View API docs
                </Button>
                <Button variant="outline" size="sm">
                  Configure module
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Stats grid placeholder */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard label="Total Records" value="—" icon={FileText} color="from-emerald-500 to-teal-500" />
        <StatCard label="Active Today" value="—" icon={ArrowRight} color="from-amber-500 to-orange-500" />
        <StatCard label="This Month" value="—" icon={Sparkles} color="from-blue-500 to-cyan-500" />
      </div>

      {/* Empty table */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Recent {title}</CardTitle>
          <CardDescription>Records will appear here once the module is activated.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="text-center py-12 text-slate-400">
            <Icon className="h-10 w-10 mx-auto mb-3 opacity-50" />
            <p className="text-sm">No records yet</p>
            <Button variant="outline" size="sm" className="mt-3">
              <Plus className="mr-1 h-3 w-3" /> Create first record
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function StatCard({ label, value, icon: Icon, color }: { label: string; value: string; icon: LucideIcon; color: string }) {
  return (
    <Card>
      <CardContent className="p-5 flex items-center gap-4">
        <div className={`h-11 w-11 rounded-xl bg-gradient-to-br ${color} grid place-items-center text-white shrink-0`}>
          <Icon className="h-5 w-5" />
        </div>
        <div>
          <div className="text-2xl font-bold text-slate-900">{value}</div>
          <div className="text-xs text-slate-500">{label}</div>
        </div>
      </CardContent>
    </Card>
  );
}
