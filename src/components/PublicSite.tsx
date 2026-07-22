"use client";

/**
 * DreamKorea SmartClass — Public Website (Viewer Only)
 *
 * This is the public-facing website. It shows institute info, NOT the app system.
 * Visitors can: read about the institute, see features, contact, download app.
 * Visitors CANNOT: login, access admin, see any system features.
 *
 * Admin access: /admin-login/<secret-token> → ID + Password
 * Teacher access: /teacher-login/<secret-token> → OTP login
 *
 * Students use the mobile app only.
 */

import { useState } from "react";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Home, BookOpen, Image, FileText, Phone, MapPin, Facebook, Youtube,
  Download, Award, Users, Video, Headphones, CheckCircle2, Menu, X,
  GraduationCap, Smartphone
} from "lucide-react";
import { cn } from "@/lib/utils";

type Page = "home" | "about" | "gallery" | "blogs" | "notice" | "contact";

const NAV: { id: Page; label: string }[] = [
  { id: "home", label: "Home" },
  { id: "about", label: "About Us" },
  { id: "gallery", label: "Gallery" },
  { id: "blogs", label: "Blogs" },
  { id: "notice", label: "Notice" },
  { id: "contact", label: "Contact Us" },
];

const FEATURES = [
  "TAB (UBT, CBT) बाट परिक्षा लिईने",
  "SMART BOARD को प्रयोग गरि Audio, Video द्वारा Class गराईने",
  "सम्पूर्ण कक्षाको रेकर्डेड भिडिओ उपलब्ध गराईन्छ",
  "अनलाइन तथा भौतिक कक्षा संचालन हुने",
  "शान्त वातावरणमा पढाई हुने",
  "उच्चारण र ब्याकरणमा विशेष जोड़ दिईने",
  "हफ्ता पिच्छे परिक्षा लिईने",
  "होस्टेलको व्यवस्था भएको",
  "विगतका प्रश्नपत्रको बारेमा अध्यापन गराइने",
  "आफ्नै App मा परीक्षा गराइने",
  "कुनै दिन कक्षा आउन नसकेमा अनलाइन मार्फत पनि सो कक्षा लिन मिल्ने",
];

export default function PublicSite() {
  const [page, setPage] = useState<Page>("home");
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="min-h-screen bg-white flex flex-col">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white border-b shadow-sm">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="h-9 w-9 rounded-lg bg-gradient-to-br from-blue-600 to-indigo-700 grid place-items-center text-white">
              <GraduationCap className="h-5 w-5" />
            </div>
            <span className="font-bold text-lg text-slate-900">Dream Korean</span>
          </div>
          <nav className="hidden md:flex items-center gap-6">
            {NAV.map((n) => (
              <button
                key={n.id}
                onClick={() => setPage(n.id)}
                className={cn(
                  "text-sm font-medium transition-colors",
                  page === n.id ? "text-blue-600" : "text-slate-600 hover:text-slate-900"
                )}
              >
                {n.label}
              </button>
            ))}
          </nav>
          <Button
            size="sm"
            className="hidden md:flex bg-blue-600 hover:bg-blue-700"
            onClick={() => window.open("https://github.com/jhapastudio38-netizen/eduplatform/actions/runs/29900757191")}
          >
            <Download className="mr-1 h-4 w-4" /> Get App
          </Button>
          <button className="md:hidden" onClick={() => setMobileOpen(!mobileOpen)}>
            {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
          </button>
        </div>
        {mobileOpen && (
          <div className="md:hidden border-t bg-white px-4 py-3 space-y-2">
            {NAV.map((n) => (
              <button
                key={n.id}
                onClick={() => { setPage(n.id); setMobileOpen(false); }}
                className={cn(
                  "block w-full text-left py-2 text-sm font-medium",
                  page === n.id ? "text-blue-600" : "text-slate-600"
                )}
              >
                {n.label}
              </button>
            ))}
            <Button size="sm" className="w-full bg-blue-600">
              <Download className="mr-1 h-4 w-4" /> Get App
            </Button>
          </div>
        )}
      </header>

      {/* Content */}
      <main className="flex-1">
        {page === "home" && <HomePage />}
        {page === "about" && <AboutPage />}
        {page === "gallery" && <GalleryPage />}
        {page === "blogs" && <BlogsPage />}
        {page === "notice" && <NoticePage />}
        {page === "contact" && <ContactPage />}
      </main>

      {/* Footer */}
      <footer className="bg-slate-900 text-slate-300 py-8">
        <div className="max-w-6xl mx-auto px-4 grid sm:grid-cols-3 gap-6">
          <div>
            <h3 className="font-bold text-white mb-2">Dream Korean Smart Class</h3>
            <p className="text-sm text-slate-400">Navigates your Korean Dream</p>
            <div className="flex gap-3 mt-3">
              <a href="https://facebook.com" target="_blank" rel="noreferrer" className="h-9 w-9 rounded-lg bg-slate-800 grid place-items-center hover:bg-blue-600 transition">
                <Facebook className="h-4 w-4" />
              </a>
              <a href="https://youtube.com" target="_blank" rel="noreferrer" className="h-9 w-9 rounded-lg bg-slate-800 grid place-items-center hover:bg-red-600 transition">
                <Youtube className="h-4 w-4" />
              </a>
            </div>
          </div>
          <div>
            <h3 className="font-bold text-white mb-2">Contact</h3>
            <p className="text-sm text-slate-400 flex items-center gap-2 mb-1">
              <MapPin className="h-4 w-4" /> Krishithok Road, Birtamod, Jhapa, Nepal
            </p>
            <p className="text-sm text-slate-400 flex items-center gap-2 mb-1">
              <Phone className="h-4 w-4" /> 023-591658
            </p>
            <p className="text-sm text-slate-400 flex items-center gap-2">
              <Phone className="h-4 w-4" /> 9852677658 / 9765308000
            </p>
          </div>
          <div>
            <h3 className="font-bold text-white mb-2">Download App</h3>
            <p className="text-sm text-slate-400 mb-3">Get our mobile app for students</p>
            <Button size="sm" className="bg-blue-600 hover:bg-blue-700" onClick={() => window.open("https://github.com/jhapastudio38-netizen/eduplatform/actions/runs/29900757191")}>
              <Smartphone className="mr-1 h-4 w-4" /> Download APK
            </Button>
          </div>
        </div>
        <div className="max-w-6xl mx-auto px-4 mt-6 pt-6 border-t border-slate-800 text-center text-xs text-slate-500">
          Copyright 2024. dreamkoreansmartclass.com All rights reserved
        </div>
      </footer>
    </div>
  );
}

// ─── Home Page ────────────────────────────────────────────────────────────────
function HomePage() {
  return (
    <div>
      {/* Hero */}
      <section className="relative bg-gradient-to-br from-blue-600 via-indigo-700 to-blue-800 text-white py-20">
        <div className="max-w-6xl mx-auto px-4 text-center">
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="text-3xl md:text-5xl font-bold mb-4"
          >
            WELCOME TO DREAM KOREAN SMART CLASS
          </motion.h1>
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="text-lg md:text-xl text-white/80 mb-8"
          >
            UBT टेस्ट सुरु गर्न तल Start Exam मा थिच्नु होस्
          </motion.p>
          <Button size="lg" className="bg-white text-blue-600 hover:bg-white/90" onClick={() => window.open("https://github.com/jhapastudio38-netizen/eduplatform/actions/runs/29900757191")}>
            Start Exam
          </Button>
        </div>
      </section>

      {/* Registration banner */}
      <section className="bg-slate-50 py-8 border-b">
        <div className="max-w-6xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">ड्रिम कोरियन स्मार्ट क्लास</h2>
            <p className="text-slate-600">Navigates your Korean Dream</p>
          </div>
          <div className="flex gap-3">
            <Button className="bg-blue-600 hover:bg-blue-700">CLICK HERE TO REGISTER</Button>
            <Button variant="outline" onClick={() => window.open("https://github.com/jhapastudio38-netizen/eduplatform/actions/runs/29900757191")}>
              <Download className="mr-1 h-4 w-4" /> GET APP
            </Button>
          </div>
        </div>
      </section>

      {/* Why Choose Us */}
      <section className="py-16">
        <div className="max-w-6xl mx-auto px-4">
          <h2 className="text-2xl md:text-3xl font-bold text-center text-slate-900 mb-8">
            WHY DREAM KOREAN SMART CLASS?
          </h2>
          <div className="grid md:grid-cols-2 gap-8">
            <div>
              <p className="text-slate-600 leading-relaxed mb-6">
                DREAM KOREAN SMART CLASS कोरियन भाषा र शैक्षिक परामर्शको दिशामा केन्द्रित छ।
                कोरियामा झण्डै 12 वर्ष बसाई पूरा गरी कोरियामै कोरियन भाषा अध्ययन गरी
                TOPIK LEVEL-6 परिक्षा उत्तीर्ण गरेको प्रशिक्षकको आफ्नै INSTITUTE भएकोले
                हामी यसमा प्रतिबद्धताका साथ काम गर्ने सोचमा छौं। अहिले को स्मार्ट जमानामा
                स्मार्ट प्रबिधि को प्रयोग गरेर बिद्यार्थीहरु लाइ स्मार्ट तरिकाको अध्यापन गराईन्छ।
              </p>
              <ul className="space-y-3">
                {FEATURES.map((f, i) => (
                  <li key={i} className="flex items-start gap-2 text-sm text-slate-700">
                    <CheckCircle2 className="h-5 w-5 text-blue-600 shrink-0 mt-0.5" />
                    <span>{f}</span>
                  </li>
                ))}
              </ul>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <FeatureCard icon={Award} title="TOPIK Level 6" desc="Certified Instructor" />
              <FeatureCard icon={Video} title="Smart Board" desc="Audio-Video Classes" />
              <FeatureCard icon={Headphones} title="Recorded Videos" desc="All classes recorded" />
              <FeatureCard icon={Users} title="Hostel Facility" desc="Available for students" />
              <FeatureCard icon={BookOpen} title="Past Papers" desc="Previous exam practice" />
              <FeatureCard icon={Smartphone} title="Mobile App" desc="Exam on your phone" />
            </div>
          </div>
        </div>
      </section>

      {/* Location */}
      <section className="bg-slate-50 py-12">
        <div className="max-w-6xl mx-auto px-4 text-center">
          <h2 className="text-2xl font-bold text-slate-900 mb-4">Our Location</h2>
          <div className="rounded-xl overflow-hidden border shadow-sm">
            <iframe
              src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3564.7!2d87.99!3d26.67!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2zMjbCsDQwJzEyLjAiTiA4N8KwNTknMjQuMCJF!5e0!3m2!1sen!2snp!4v1700000000000"
              width="100%"
              height="300"
              style={{ border: 0 }}
              loading="lazy"
              title="Dream Korean Smart Class Location"
            />
          </div>
          <p className="mt-4 text-slate-600">
            <MapPin className="inline h-4 w-4 mr-1" />
            Krishithok Road, Birtamod, Jhapa, Nepal
          </p>
        </div>
      </section>
    </div>
  );
}

// ─── About Page ───────────────────────────────────────────────────────────────
function AboutPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">About Us</h1>
      <div className="prose prose-slate max-w-none">
        <p className="text-slate-600 leading-relaxed mb-4">
          DREAM KOREAN SMART CLASS कोरियन भाषा र शैक्षिक परामर्शको दिशामा केन्द्रित छ।
          कोरियामा झण्डै 12 वर्ष बसाई पूरा गरी कोरियामै कोरियन भाषा अध्ययन गरी
          TOPIK LEVEL-6 परिक्षा उत्तीर्ण गरेको प्रशिक्षकको आफ्नै INSTITUTE भएकोले
          हामी यसमा प्रतिबद्धताका साथ काम गर्ने सोचमा छौं।
        </p>
        <p className="text-slate-600 leading-relaxed mb-4">
          अहिले को स्मार्ट जमानामा स्मार्ट प्रबिधि को प्रयोग गरेर बिद्यार्थीहरु लाइ
          स्मार्ट तरिकाको अध्यापन गराईन्छ। हामी उच्चारण र ब्याकरणमा विशेष जोड दिएर
          पढाउँछौं। हफ्ता पिच्छे परिक्षा लिईन्छ र विगतका प्रश्नपत्रको बारेमा अध्यापन गराइन्छ।
        </p>
        <h2 className="text-xl font-bold text-slate-900 mt-6 mb-3">Our Features</h2>
        <ul className="space-y-2">
          {FEATURES.map((f, i) => (
            <li key={i} className="flex items-start gap-2 text-slate-700">
              <CheckCircle2 className="h-5 w-5 text-blue-600 shrink-0 mt-0.5" />
              <span>{f}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

// ─── Gallery Page ─────────────────────────────────────────────────────────────
function GalleryPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">Gallery</h1>
      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        {[1, 2, 3, 4, 5, 6].map((i) => (
          <div key={i} className="aspect-square rounded-xl bg-gradient-to-br from-blue-100 to-indigo-200 grid place-items-center">
            <Image className="h-12 w-12 text-blue-400" />
          </div>
        ))}
      </div>
      <p className="text-center text-slate-500 mt-6">Gallery photos coming soon.</p>
    </div>
  );
}

// ─── Blogs Page ──────────────────────────────────────────────────────────────
function BlogsPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">Blogs</h1>
      <Card><CardContent className="p-8 text-center text-slate-500">
        <FileText className="h-12 w-12 mx-auto mb-3 opacity-40" />
        <p>Blog posts coming soon. Stay tuned for Korean language tips and TOPIK preparation guides.</p>
      </CardContent></Card>
    </div>
  );
}

// ─── Notice Page ─────────────────────────────────────────────────────────────
function NoticePage() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">Notice</h1>
      <Card><CardContent className="p-8 text-center text-slate-500">
        <FileText className="h-12 w-12 mx-auto mb-3 opacity-40" />
        <p>No notices at this time. Check back later for important announcements.</p>
      </CardContent></Card>
    </div>
  );
}

// ─── Contact Page ────────────────────────────────────────────────────────────
function ContactPage() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <h1 className="text-3xl font-bold text-slate-900 mb-6">Contact Us</h1>
      <div className="grid sm:grid-cols-2 gap-6">
        <Card>
          <CardContent className="p-6 space-y-3">
            <div className="flex items-start gap-3">
              <MapPin className="h-5 w-5 text-blue-600 shrink-0 mt-0.5" />
              <div>
                <div className="font-semibold text-slate-900">Address</div>
                <p className="text-sm text-slate-600">Krishithok Road, Birtamod, Jhapa, Nepal</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Phone className="h-5 w-5 text-blue-600 shrink-0 mt-0.5" />
              <div>
                <div className="font-semibold text-slate-900">Phone</div>
                <p className="text-sm text-slate-600">023-591658</p>
                <p className="text-sm text-slate-600">9852677658</p>
                <p className="text-sm text-slate-600">9765308000</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-6">
            <div className="rounded-lg overflow-hidden border">
              <iframe
                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3564.7!2d87.99!3d26.67!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2zMjbCsDQwJzEyLjAiTiA4N8KwNTknMjQuMCJF!5e0!3m2!1sen!2snp!4v1700000000000"
                width="100%"
                height="200"
                style={{ border: 0 }}
                loading="lazy"
                title="Location"
              />
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

// ─── Feature Card ────────────────────────────────────────────────────────────
function FeatureCard({ icon: Icon, title, desc }: { icon: any; title: string; desc: string }) {
  return (
    <div className="bg-white rounded-xl border p-4 shadow-sm hover:shadow-md transition">
      <div className="h-10 w-10 rounded-lg bg-blue-50 grid place-items-center mb-2">
        <Icon className="h-5 w-5 text-blue-600" />
      </div>
      <div className="font-semibold text-sm text-slate-900">{title}</div>
      <div className="text-xs text-slate-500">{desc}</div>
    </div>
  );
}
