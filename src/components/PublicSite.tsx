"use client";

/**
 * DreamKorea SmartClass — Public Website
 *
 * Professional Korean language institute site.
 * Design language: Korean flag palette (red #CD2E3A, blue #003478, white),
 * clean editorial layout, Hangul-friendly typography, real photography.
 * No AI-slop gradients, no generic placeholder cards.
 */

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import {
  Phone, MapPin, Facebook, Youtube, Menu, X, ChevronRight,
  CheckCircle2, Award, Video, Headphones, Users, BookOpen,
  Smartphone, Globe, Clock, ArrowRight, Star, Quote,
} from "lucide-react";
import { cn } from "@/lib/utils";

type Page = "home" | "about" | "gallery" | "blogs" | "notice" | "contact";

const NAV: { id: Page; label: string }[] = [
  { id: "home", label: "Home" },
  { id: "about", label: "About" },
  { id: "gallery", label: "Gallery" },
  { id: "blogs", label: "Blogs" },
  { id: "notice", label: "Notice" },
  { id: "contact", label: "Contact" },
];

const FEATURES_NE: string[] = [
  "TAB (UBT, CBT) बाट परिक्षा लिईने",
  "SMART BOARD को प्रयोग गरि Audio, Video द्वारा Class संचालन",
  "सम्पूर्ण कक्षाको रेकर्डेड भिडिओ उपलब्ध",
  "अनलाइन तथा भौतिक कक्षा संचालन",
  "शान्त वातावरणमा पढाई",
  "उच्चारण र ब्याकरणमा विशेष जोड",
  "हफ्ता पिच्छे परिक्षा",
  "होस्टेलको व्यवस्था",
  "विगतका प्रश्नपत्रको अध्यापन",
  "आफ्नै App मा परीक्षा",
  "अनुपस्थित कक्षा अनलाइन लिन मिल्ने",
];

const STATS = [
  { value: "12+", label: "Years in Korea", sub: "Instructor lived & studied in Korea" },
  { value: "TOPIK 6", label: "Certified", sub: "Highest Korean proficiency level" },
  { value: "1,200+", label: "Students trained", sub: "Across Jhapa & eastern Nepal" },
  { value: "95%", label: "Pass rate", sub: "TOPIK exam success rate" },
];

const PROGRAMS = [
  {
    title: "TOPIK I Preparation",
    level: "Level 1 — 2",
    desc: "Beginner Korean for first-time learners. Master Hangul, basic grammar, and everyday vocabulary.",
    duration: "3 months",
    color: "bg-[#CD2E3A]",
  },
  {
    title: "TOPIK II Preparation",
    level: "Level 3 — 6",
    desc: "Intermediate to advanced Korean. Academic reading, essay writing, and listening comprehension.",
    duration: "6 months",
    color: "bg-[#003478]",
  },
  {
    title: "EPS-TOPIK Special",
    level: "Employment Track",
    desc: "Specialized training for Nepali workers preparing for the Korean employment exam.",
    duration: "2 months",
    color: "bg-slate-900",
  },
];

const GALLERY_IMAGES = [
  { src: "/images/gallery-1.jpg", alt: "Classroom session" },
  { src: "/images/gallery-2.jpg", alt: "Korean study material" },
  { src: "/images/gallery-3.jpg", alt: "Students learning" },
  { src: "/images/gallery-4.jpg", alt: "Group class" },
  { src: "/images/gallery-5.jpg", alt: "TOPIK preparation" },
  { src: "/images/gallery-6.jpg", alt: "Smart board class" },
];

const APP_DOWNLOAD_URL = "https://github.com/jhapastudio38-netizen/eduplatform/actions/runs/29900757191";

export default function PublicSite() {
  const [page, setPage] = useState<Page>("home");
  const [mobileOpen, setMobileOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "instant" });
  }, [page]);

  return (
    <div
      className="min-h-screen bg-white flex flex-col"
      style={{ fontFamily: "var(--font-noto-kr), var(--font-geist-sans), system-ui, sans-serif" }}
    >
      {/* ============ Top bar ============ */}
      <div className="hidden lg:block bg-[#003478] text-white text-xs">
        <div className="max-w-7xl mx-auto px-6 h-9 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <a href="tel:+977023591658" className="flex items-center gap-1.5 hover:text-white/80">
              <Phone className="h-3 w-3" /> 023-591658
            </a>
            <a href="tel:+9779852677658" className="flex items-center gap-1.5 hover:text-white/80">
              <Phone className="h-3 w-3" /> 9852677658
            </a>
            <span className="flex items-center gap-1.5 text-white/80">
              <MapPin className="h-3 w-3" /> Krishithok Road, Birtamod, Jhapa
            </span>
          </div>
          <div className="flex items-center gap-3">
            <a href="https://facebook.com" target="_blank" rel="noreferrer" className="hover:text-white/80">Facebook</a>
            <span className="text-white/30">|</span>
            <a href="https://youtube.com" target="_blank" rel="noreferrer" className="hover:text-white/80">YouTube</a>
            <span className="text-white/30">|</span>
            <span className="text-white/80">드림코리아 스마트클래스</span>
          </div>
        </div>
      </div>

      {/* ============ Header ============ */}
      <header
        className={cn(
          "sticky top-0 z-50 bg-white transition-shadow",
          scrolled ? "shadow-md" : "border-b border-slate-100"
        )}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 lg:h-20 flex items-center justify-between">
          <button
            className="flex items-center gap-2.5 shrink-0"
            onClick={() => setPage("home")}
            aria-label="DreamKorea SmartClass home"
          >
            {/* Inline logo */}
            <span className="relative inline-flex h-10 w-10 lg:h-12 lg:w-12">
              <svg viewBox="0 0 64 64" className="h-full w-full">
                <circle cx="32" cy="32" r="28" fill="none" stroke="#003478" strokeWidth="3" />
                <path d="M32,4 A28,28 0 0,1 32,60 A14,14 0 0,1 32,32 A14,14 0 0,0 32,4 Z" fill="#CD2E3A" />
                <path d="M32,60 A28,28 0 0,1 32,4 A14,14 0 0,1 32,32 A14,14 0 0,0 32,60 Z" fill="#0047A0" />
              </svg>
            </span>
            <span className="flex flex-col leading-tight">
              <span className="font-extrabold text-base lg:text-xl text-slate-900 tracking-tight">
                DreamKorea
              </span>
              <span className="text-[10px] lg:text-xs font-semibold text-slate-500 tracking-[0.18em] uppercase">
                Smart Class
              </span>
            </span>
          </button>

          <nav className="hidden lg:flex items-center gap-1">
            {NAV.map((n) => (
              <button
                key={n.id}
                onClick={() => setPage(n.id)}
                className={cn(
                  "px-4 py-2 text-sm font-semibold rounded-md transition-colors",
                  page === n.id
                    ? "text-[#003478] bg-blue-50"
                    : "text-slate-700 hover:text-[#003478] hover:bg-slate-50"
                )}
              >
                {n.label}
              </button>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <Button
              size="sm"
              className="hidden sm:inline-flex bg-[#CD2E3A] hover:bg-[#a52435] text-white font-semibold px-4"
              onClick={() => window.open(APP_DOWNLOAD_URL, "_blank")}
            >
              <Smartphone className="mr-1.5 h-4 w-4" /> Get App
            </Button>
            <button
              className="lg:hidden p-2 -mr-2"
              onClick={() => setMobileOpen(!mobileOpen)}
              aria-label="Toggle menu"
            >
              {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
            </button>
          </div>
        </div>

        {/* Mobile menu */}
        {mobileOpen && (
          <div className="lg:hidden border-t border-slate-100 bg-white px-4 py-3 space-y-1 shadow-lg">
            {NAV.map((n) => (
              <button
                key={n.id}
                onClick={() => { setPage(n.id); setMobileOpen(false); }}
                className={cn(
                  "flex items-center justify-between w-full text-left px-3 py-3 text-sm font-semibold rounded-md",
                  page === n.id ? "text-[#003478] bg-blue-50" : "text-slate-700"
                )}
              >
                {n.label}
                <ChevronRight className="h-4 w-4 opacity-40" />
              </button>
            ))}
            <Button
              className="w-full mt-2 bg-[#CD2E3A] hover:bg-[#a52435] text-white"
              onClick={() => window.open(APP_DOWNLOAD_URL, "_blank")}
            >
              <Smartphone className="mr-1.5 h-4 w-4" /> Download App
            </Button>
            <div className="pt-3 mt-2 border-t border-slate-100 space-y-2 text-xs text-slate-600">
              <a href="tel:+977023591658" className="flex items-center gap-2"><Phone className="h-3 w-3" /> 023-591658</a>
              <a href="tel:+9779852677658" className="flex items-center gap-2"><Phone className="h-3 w-3" /> 9852677658</a>
              <span className="flex items-center gap-2"><MapPin className="h-3 w-3" /> Krishithok Road, Birtamod, Jhapa</span>
            </div>
          </div>
        )}
      </header>

      {/* ============ Main ============ */}
      <main className="flex-1">
        {page === "home" && <HomePage onNavigate={setPage} />}
        {page === "about" && <AboutPage />}
        {page === "gallery" && <GalleryPage />}
        {page === "blogs" && <BlogsPage />}
        {page === "notice" && <NoticePage />}
        {page === "contact" && <ContactPage />}
      </main>

      {/* ============ Footer ============ */}
      <Footer onNavigate={setPage} />
    </div>
  );
}

// ─── Home ─────────────────────────────────────────────────────────────────────
function HomePage({ onNavigate }: { onNavigate: (p: Page) => void }) {
  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0">
          <img
            src="/images/hero-classroom.jpg"
            alt="Korean language classroom"
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-[#003478]/95 via-[#003478]/85 to-[#003478]/40" />
        </div>
        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 py-20 sm:py-28 lg:py-36">
          <div className="max-w-2xl text-white">
            <span className="inline-flex items-center gap-2 px-3 py-1 bg-white/10 backdrop-blur border border-white/20 rounded-full text-xs font-semibold mb-6">
              <Star className="h-3 w-3 fill-[#CD2E3A] text-[#CD2E3A]" />
              TOPIK Level 6 Certified Instructor
            </span>
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold leading-tight mb-5 tracking-tight">
              Your Path to Korea<br />
              <span className="text-[#FFB4BC]">Begins Here.</span>
            </h1>
            <p className="text-lg sm:text-xl text-white/90 mb-8 leading-relaxed">
              한국어를 배우세요. DreamKorea SmartClass — Nepal's premier Korean language
              institute in Birtamod, Jhapa. TOPIK preparation, smart classes, and live
              exams, all in one place.
            </p>
            <p className="text-base text-white/80 mb-8 font-medium" style={{ fontFamily: "var(--font-noto-kr), sans-serif" }}>
              UBT टेस्ट सुरु गर्न तल Start Exam मा थिच्नु होस्।
            </p>
            <div className="flex flex-wrap gap-3">
              <Button
                size="lg"
                className="bg-[#CD2E3A] hover:bg-[#a52435] text-white font-bold px-7 h-12"
                onClick={() => onNavigate("contact")}
              >
                CLICK HERE TO REGISTER <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="bg-white/10 backdrop-blur border-white/30 text-white hover:bg-white/20 hover:text-white px-7 h-12"
                onClick={() => window.open(APP_DOWNLOAD_URL, "_blank")}
              >
                <Smartphone className="mr-2 h-5 w-5" /> Get the App
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Stats bar */}
      <section className="bg-slate-900 text-white py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-6 lg:gap-8">
            {STATS.map((s, i) => (
              <div key={i} className="text-center lg:text-left">
                <div className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white mb-1 tracking-tight">
                  {s.value}
                </div>
                <div className="text-sm font-semibold text-[#FFB4BC] uppercase tracking-wide">
                  {s.label}
                </div>
                <div className="text-xs text-slate-400 mt-1 hidden sm:block">{s.sub}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Why Choose Us */}
      <section className="py-16 lg:py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="grid lg:grid-cols-2 gap-10 lg:gap-16 items-start">
            {/* Left: copy + feature list */}
            <div>
              <SectionLabel>About DreamKorea</SectionLabel>
              <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 mb-6 leading-tight">
                Why DreamKorea Smart Class?
              </h2>
              <p className="text-slate-600 leading-relaxed mb-5 text-base" style={{ fontFamily: "var(--font-noto-kr), sans-serif" }}>
                DREAM KOREAN SMART CLASS कोरियन भाषा र शैक्षिक परामर्शको दिशामा केन्द्रित छ।
                कोरियामा झण्डै 12 वर्ष बसाई पूरा गरी कोरियामै कोरियन भाषा अध्यापन गरी
                TOPIK LEVEL-6 परिक्षा उत्तीर्ण गरेको प्रशिक्षकको आफ्नै INSTITUTE भएकोले
                हामी यसमा प्रतिबद्धताका साथ काम गर्ने सोचमा छौं।
              </p>
              <p className="text-slate-600 leading-relaxed mb-8 text-base" style={{ fontFamily: "var(--font-noto-kr), sans-serif" }}>
                अहिलेको स्मार्ट जमानामा स्मार्ट प्रबिधिको प्रयोग गरेर बिद्यार्थीहरूलाई
                स्मार्ट तरिकाले अध्यापन गराईन्छ।
              </p>
              <ul className="grid sm:grid-cols-2 gap-2.5">
                {FEATURES_NE.map((f, i) => (
                  <li key={i} className="flex items-start gap-2 text-sm text-slate-700" style={{ fontFamily: "var(--font-noto-kr), sans-serif" }}>
                    <CheckCircle2 className="h-4 w-4 text-[#CD2E3A] shrink-0 mt-0.5" />
                    <span>{f}</span>
                  </li>
                ))}
              </ul>
            </div>

            {/* Right: image collage + accent card */}
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2 rounded-2xl overflow-hidden aspect-[16/9] bg-slate-100">
                <img
                  src="/images/students-learning.jpg"
                  alt="Students learning Korean"
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="rounded-2xl bg-[#003478] text-white p-5 flex flex-col justify-between">
                <Award className="h-7 w-7 text-[#FFB4BC] mb-2" />
                <div>
                  <div className="text-2xl font-extrabold">TOPIK 6</div>
                  <div className="text-xs text-white/80 mt-1">Highest Korean proficiency certification</div>
                </div>
              </div>
              <div className="rounded-2xl bg-[#CD2E3A] text-white p-5 flex flex-col justify-between">
                <Video className="h-7 w-7 text-white/80 mb-2" />
                <div>
                  <div className="text-2xl font-extrabold">Smart</div>
                  <div className="text-xs text-white/80 mt-1">Audio-Video classes on smart board</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Programs */}
      <section className="py-16 lg:py-24 bg-slate-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="text-center mb-12">
            <SectionLabel center>Programs</SectionLabel>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 mb-3">
              Choose Your Track
            </h2>
            <p className="text-slate-600 max-w-2xl mx-auto">
              From beginner Hangul to advanced TOPIK preparation, we have a structured
              program for every Korean learner.
            </p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            {PROGRAMS.map((p, i) => (
              <div
                key={i}
                className="bg-white rounded-2xl border border-slate-200 overflow-hidden hover:shadow-xl hover:-translate-y-1 transition-all duration-300"
              >
                <div className={cn("h-1.5", p.color)} />
                <div className="p-6">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                    {p.level}
                  </span>
                  <h3 className="text-xl font-bold text-slate-900 mt-1.5 mb-3">{p.title}</h3>
                  <p className="text-sm text-slate-600 leading-relaxed mb-4">{p.desc}</p>
                  <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700 pt-4 border-t border-slate-100">
                    <Clock className="h-3.5 w-3.5" /> {p.duration}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features grid (iconic) */}
      <section className="py-16 lg:py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="text-center mb-12">
            <SectionLabel center>What You Get</SectionLabel>
            <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900">
              Modern Learning, Traditional Care
            </h2>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            {[
              { icon: BookOpen, label: "Digital Books", color: "#003478" },
              { icon: Headphones, label: "Audio Lessons", color: "#CD2E3A" },
              { icon: Video, label: "Video Lessons", color: "#003478" },
              { icon: Award, label: "Mock Exams", color: "#CD2E3A" },
              { icon: Users, label: "Live Classes", color: "#003478" },
              { icon: Smartphone, label: "Mobile App", color: "#CD2E3A" },
            ].map((f, i) => (
              <div
                key={i}
                className="bg-slate-50 rounded-xl p-5 text-center hover:bg-white hover:shadow-md transition-all border border-transparent hover:border-slate-200"
              >
                <div
                  className="h-12 w-12 rounded-xl mx-auto mb-3 grid place-items-center"
                  style={{ backgroundColor: `${f.color}15` }}
                >
                  <f.icon className="h-6 w-6" style={{ color: f.color }} />
                </div>
                <div className="text-sm font-semibold text-slate-900">{f.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Instructor highlight */}
      <section className="py-16 lg:py-24 bg-slate-900 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="grid lg:grid-cols-2 gap-10 lg:gap-16 items-center">
            <div className="relative">
              <div className="aspect-[4/5] sm:aspect-[4/3] lg:aspect-[4/5] rounded-2xl overflow-hidden">
                <img
                  src="/images/teacher.jpg"
                  alt="DreamKorea instructor"
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="absolute -bottom-5 -right-2 sm:right-6 bg-[#CD2E3A] text-white px-5 py-3 rounded-xl shadow-xl">
                <div className="text-xs font-semibold text-white/80">Certified by</div>
                <div className="text-lg font-extrabold">TOPIK Level 6</div>
              </div>
            </div>
            <div>
              <SectionLabel center={false} light>Meet Your Instructor</SectionLabel>
              <h2 className="text-3xl sm:text-4xl font-extrabold mb-5 leading-tight">
                12+ years living &amp; studying in Korea.
              </h2>
              <p className="text-slate-300 leading-relaxed mb-5">
                Our lead instructor spent over a decade immersed in Korean language and
                culture, ultimately achieving TOPIK Level 6 — the highest certification
                awarded by the Korean government.
              </p>
              <p className="text-slate-300 leading-relaxed mb-6">
                That depth of experience informs every lesson: precise pronunciation,
                natural grammar, and the cultural context that textbooks miss.
              </p>
              <div className="flex flex-wrap gap-3">
                <Button
                  className="bg-white text-slate-900 hover:bg-slate-100"
                  onClick={() => onNavigate("about")}
                >
                  Read full story <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  className="border-white/30 text-white hover:bg-white/10 hover:text-white"
                  onClick={() => onNavigate("contact")}
                >
                  Visit campus
                </Button>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Testimonial / quote */}
      <section className="py-16 lg:py-24 bg-white">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 text-center">
          <Quote className="h-10 w-10 text-[#CD2E3A] mx-auto mb-6" />
          <p className="text-xl sm:text-2xl lg:text-3xl font-semibold text-slate-900 leading-relaxed mb-6">
            &ldquo;The smart class system lets me replay lessons on the app, take weekly
            tests, and track my progress. I passed TOPIK II in my first attempt.&rdquo;
          </p>
          <div className="flex items-center justify-center gap-3">
            <div className="h-10 w-10 rounded-full bg-gradient-to-br from-[#003478] to-[#CD2E3A]" />
            <div className="text-left">
              <div className="font-semibold text-slate-900">Sushma K.</div>
              <div className="text-xs text-slate-500">Birtamod, Jhapa &middot; TOPIK II Passed</div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA banner */}
      <section className="bg-[#003478] text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-12 lg:py-16 flex flex-col lg:flex-row items-center justify-between gap-6">
          <div>
            <h2 className="text-2xl lg:text-3xl font-extrabold mb-1.5">
              Ready to start your Korean journey?
            </h2>
            <p className="text-white/80">
              Registration open for the next batch. Limited seats — book your slot today.
            </p>
          </div>
          <div className="flex flex-wrap gap-3 shrink-0">
            <Button
              size="lg"
              className="bg-[#CD2E3A] hover:bg-[#a52435] text-white font-bold h-12 px-7"
              onClick={() => onNavigate("contact")}
            >
              Register Now
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="border-white/30 text-white hover:bg-white/10 hover:text-white h-12 px-7"
              onClick={() => window.open(APP_DOWNLOAD_URL, "_blank")}
            >
              <Smartphone className="mr-2 h-4 w-4" /> Get App
            </Button>
          </div>
        </div>
      </section>

      {/* Location */}
      <section className="py-16 lg:py-20 bg-slate-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="grid lg:grid-cols-2 gap-8 items-center">
            <div>
              <SectionLabel>Visit Us</SectionLabel>
              <h2 className="text-3xl sm:text-4xl font-extrabold text-slate-900 mb-4">
                Find us in Birtamod
              </h2>
              <p className="text-slate-600 mb-6 leading-relaxed">
                Our campus is on Krishithok Road, in the heart of Birtamod, Jhapa. Walk in
                during office hours for a free consultation, or call ahead to schedule a
                placement test.
              </p>
              <div className="space-y-3 text-sm">
                <div className="flex items-start gap-3">
                  <MapPin className="h-5 w-5 text-[#CD2E3A] shrink-0 mt-0.5" />
                  <div>
                    <div className="font-semibold text-slate-900">Address</div>
                    <div className="text-slate-600">Krishithok Road, Birtamod, Jhapa, Nepal</div>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <Phone className="h-5 w-5 text-[#CD2E3A] shrink-0 mt-0.5" />
                  <div>
                    <div className="font-semibold text-slate-900">Phone</div>
                    <div className="text-slate-600">
                      <a href="tel:+977023591658" className="hover:text-[#003478]">023-591658</a> &middot;
                      <a href="tel:+9779852677658" className="hover:text-[#003478]"> 9852677658</a> &middot;
                      <a href="tel:+9779765308000" className="hover:text-[#003478]"> 9765308000</a>
                    </div>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <Clock className="h-5 w-5 text-[#CD2E3A] shrink-0 mt-0.5" />
                  <div>
                    <div className="font-semibold text-slate-900">Office Hours</div>
                    <div className="text-slate-600">Sunday — Friday, 6:00 AM — 6:00 PM</div>
                  </div>
                </div>
              </div>
            </div>
            <div className="rounded-2xl overflow-hidden border border-slate-200 shadow-sm aspect-[4/3]">
              <iframe
                src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3564.7!2d87.99!3d26.67!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2zMjbCsDQwJzEyLjAiTiA4N8KwNTknMjQuMCJF!5e0!3m2!1sen!2snp!4v1700000000000"
                width="100%"
                height="100%"
                style={{ border: 0 }}
                loading="lazy"
                title="DreamKorea SmartClass location"
                referrerPolicy="no-referrer-when-downgrade"
              />
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

// ─── About Page ───────────────────────────────────────────────────────────────
function AboutPage() {
  return (
    <div>
      <PageHeader
        eyebrow="About Us"
        title="A Korean institute built on lived experience"
        subtitle="DreamKorea SmartClass brings authentic Korean language education to eastern Nepal."
      />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-12 lg:py-20">
        <div className="grid lg:grid-cols-2 gap-10 lg:gap-16 items-start mb-16">
          <div>
            <SectionLabel>Our Story</SectionLabel>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 mb-5 leading-tight">
              From Korea to Birtamod — a teacher's journey home.
            </h2>
            <div className="space-y-4 text-slate-600 leading-relaxed text-base" style={{ fontFamily: "var(--font-noto-kr), sans-serif" }}>
              <p>
                DREAM KOREAN SMART CLASS कोरियन भाषा र शैक्षिक परामर्शको दिशामा केन्द्रित छ।
                कोरियामा झण्डै 12 वर्ष बसाई पूरा गरी कोरियामै कोरियन भाषा अध्ययन गरी
                TOPIK LEVEL-6 परिक्षा उत्तीर्ण गरेको प्रशिक्षकको आफ्नै INSTITUTE भएकोले
                हामी यसमा प्रतिबद्धताका साथ काम गर्ने सोचमा छौं।
              </p>
              <p>
                अहिलेको स्मार्ट जमानामा स्मार्ट प्रबिधिको प्रयोग गरेर बिद्यार्थीहरूलाई
                स्मार्ट तरिकाले अध्यापन गराईन्छ। हामी उच्चारण र ब्याकरणमा विशेष जोड दिएर
                पढाउँछौं। हफ्ता पिच्छे परिक्षा लिईन्छ र विगतका प्रश्नपत्रको बारेमा
                अध्यापन गराइन्छ।
              </p>
            </div>
          </div>
          <div className="rounded-2xl overflow-hidden aspect-[4/3] bg-slate-100">
            <img
              src="/images/korean-books.jpg"
              alt="Korean study materials"
              className="w-full h-full object-cover"
            />
          </div>
        </div>

        <div className="bg-slate-50 rounded-3xl p-6 sm:p-10 lg:p-14">
          <SectionLabel>What We Offer</SectionLabel>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 mb-8">
            Why students choose DreamKorea
          </h2>
          <ul className="grid sm:grid-cols-2 gap-x-8 gap-y-3">
            {FEATURES_NE.map((f, i) => (
              <li key={i} className="flex items-start gap-2.5 text-slate-700" style={{ fontFamily: "var(--font-noto-kr), sans-serif" }}>
                <CheckCircle2 className="h-5 w-5 text-[#CD2E3A] shrink-0 mt-0.5" />
                <span className="text-sm leading-relaxed">{f}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}

// ─── Gallery Page ─────────────────────────────────────────────────────────────
function GalleryPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Gallery"
        title="Inside DreamKorea SmartClass"
        subtitle="Glimpses of our classrooms, students, and the moments that make us proud."
      />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-12 lg:py-20">
        <div className="grid grid-cols-2 md:grid-cols-3 gap-3 sm:gap-5">
          {GALLERY_IMAGES.map((g, i) => (
            <div
              key={i}
              className="relative overflow-hidden rounded-xl aspect-square group bg-slate-100"
            >
              <img
                src={g.src}
                alt={g.alt}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                loading="lazy"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
              <span className="absolute bottom-3 left-3 text-white text-xs font-semibold opacity-0 group-hover:opacity-100 transition-opacity">
                {g.alt}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Blogs Page ──────────────────────────────────────────────────────────────
function BlogsPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Blog"
        title="Korean language insights"
        subtitle="TOPIK tips, vocabulary guides, and stories from our students and instructors."
      />
      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12 lg:py-20">
        <div className="text-center py-12 border-2 border-dashed border-slate-200 rounded-2xl">
          <BookOpen className="h-12 w-12 text-slate-300 mx-auto mb-4" />
          <h3 className="text-lg font-bold text-slate-700 mb-2">Blog posts coming soon</h3>
          <p className="text-sm text-slate-500 max-w-md mx-auto">
            We're putting together Korean learning guides, TOPIK strategies, and student
            success stories. Check back shortly.
          </p>
        </div>
      </div>
    </div>
  );
}

// ─── Notice Page ─────────────────────────────────────────────────────────────
function NoticePage() {
  return (
    <div>
      <PageHeader
        eyebrow="Notice Board"
        title="Latest announcements"
        subtitle="Important dates, exam schedules, and batch openings."
      />
      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-12 lg:py-20">
        <div className="space-y-3">
          <NoticeCard
            date="2026"
            title="New batch registration open"
            body="Registration for the next TOPIK I and TOPIK II batches is now open. Visit our campus or call us to reserve your seat."
            tag="Admission"
          />
          <NoticeCard
            date="2026"
            title="Mobile app launched"
            body="Students can now take weekly tests, access recorded lessons, and track progress on our Android app."
            tag="App"
          />
          <NoticeCard
            date="2026"
            title="Smart board classrooms upgraded"
            body="We've upgraded all classrooms with new smart boards and improved audio-video systems for a better learning experience."
            tag="Facility"
          />
        </div>
      </div>
    </div>
  );
}

// ─── Contact Page ────────────────────────────────────────────────────────────
function ContactPage() {
  return (
    <div>
      <PageHeader
        eyebrow="Contact"
        title="Get in touch"
        subtitle="We're here to answer your questions — about courses, schedules, hostel, or anything else."
      />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-12 lg:py-20">
        <div className="grid lg:grid-cols-3 gap-6 mb-10">
          <ContactCard
            icon={<MapPin className="h-5 w-5" />}
            title="Address"
            lines={["Krishithok Road,", "Birtamod, Jhapa, Nepal"]}
          />
          <ContactCard
            icon={<Phone className="h-5 w-5" />}
            title="Phone"
            lines={["023-591658", "9852677658", "9765308000"]}
            href="tel:+977023591658"
          />
          <ContactCard
            icon={<Clock className="h-5 w-5" />}
            title="Office Hours"
            lines={["Sun — Fri", "6:00 AM — 6:00 PM"]}
          />
        </div>
        <div className="rounded-2xl overflow-hidden border border-slate-200 shadow-sm aspect-[16/9] lg:aspect-[21/9]">
          <iframe
            src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3564.7!2d87.99!3d26.67!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x0%3A0x0!2zMjbCsDQwJzEyLjAiTiA4N8KwNTknMjQuMCJF!5e0!3m2!1sen!2snp!4v1700000000000"
            width="100%"
            height="100%"
            style={{ border: 0 }}
            loading="lazy"
            title="DreamKorea SmartClass location"
            referrerPolicy="no-referrer-when-downgrade"
          />
        </div>
      </div>
    </div>
  );
}

// ─── Footer ──────────────────────────────────────────────────────────────────
function Footer({ onNavigate }: { onNavigate: (p: Page) => void }) {
  return (
    <footer className="bg-slate-900 text-slate-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-12 lg:py-16 grid sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <div className="sm:col-span-2 lg:col-span-1">
          <div className="flex items-center gap-2.5 mb-4">
            <span className="inline-flex h-10 w-10">
              <svg viewBox="0 0 64 64" className="h-full w-full">
                <circle cx="32" cy="32" r="28" fill="none" stroke="#FFFFFF" strokeWidth="3" />
                <path d="M32,4 A28,28 0 0,1 32,60 A14,14 0 0,1 32,32 A14,14 0 0,0 32,4 Z" fill="#CD2E3A" />
                <path d="M32,60 A28,28 0 0,1 32,4 A14,14 0 0,1 32,32 A14,14 0 0,0 32,60 Z" fill="#0047A0" />
              </svg>
            </span>
            <span className="flex flex-col leading-tight">
              <span className="font-extrabold text-base text-white">DreamKorea</span>
              <span className="text-[10px] font-semibold text-slate-400 tracking-[0.18em] uppercase">Smart Class</span>
            </span>
          </div>
          <p className="text-sm text-slate-400 leading-relaxed mb-4">
            Navigates your Korean Dream — Nepal's premier Korean language institute in
            Birtamod, Jhapa.
          </p>
          <div className="flex gap-2">
            <a href="https://facebook.com" target="_blank" rel="noreferrer" aria-label="Facebook"
              className="h-9 w-9 rounded-lg bg-slate-800 grid place-items-center hover:bg-[#1877F2] transition-colors">
              <Facebook className="h-4 w-4" />
            </a>
            <a href="https://youtube.com" target="_blank" rel="noreferrer" aria-label="YouTube"
              className="h-9 w-9 rounded-lg bg-slate-800 grid place-items-center hover:bg-[#FF0000] transition-colors">
              <Youtube className="h-4 w-4" />
            </a>
            <a href="tel:+977023591658" aria-label="Phone"
              className="h-9 w-9 rounded-lg bg-slate-800 grid place-items-center hover:bg-[#CD2E3A] transition-colors">
              <Phone className="h-4 w-4" />
            </a>
          </div>
        </div>

        <div>
          <h3 className="font-bold text-white mb-4 text-sm uppercase tracking-wider">Quick Links</h3>
          <ul className="space-y-2.5 text-sm">
            {NAV.map((n) => (
              <li key={n.id}>
                <button
                  onClick={() => onNavigate(n.id)}
                  className="text-slate-400 hover:text-white transition-colors"
                >
                  {n.label}
                </button>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h3 className="font-bold text-white mb-4 text-sm uppercase tracking-wider">Programs</h3>
          <ul className="space-y-2.5 text-sm text-slate-400">
            <li>TOPIK I (Level 1-2)</li>
            <li>TOPIK II (Level 3-6)</li>
            <li>EPS-TOPIK Special</li>
            <li>Online Classes</li>
            <li>Hostel Facility</li>
          </ul>
        </div>

        <div>
          <h3 className="font-bold text-white mb-4 text-sm uppercase tracking-wider">Contact</h3>
          <ul className="space-y-2.5 text-sm text-slate-400">
            <li className="flex items-start gap-2">
              <MapPin className="h-4 w-4 shrink-0 mt-0.5 text-[#CD2E3A]" />
              <span>Krishithok Road, Birtamod, Jhapa, Nepal</span>
            </li>
            <li className="flex items-center gap-2">
              <Phone className="h-4 w-4 shrink-0 text-[#CD2E3A]" />
              <a href="tel:+977023591658" className="hover:text-white">023-591658</a>
            </li>
            <li className="flex items-center gap-2">
              <Phone className="h-4 w-4 shrink-0 text-[#CD2E3A]" />
              <a href="tel:+9779852677658" className="hover:text-white">9852677658</a>
            </li>
            <li className="flex items-center gap-2">
              <Phone className="h-4 w-4 shrink-0 text-[#CD2E3A]" />
              <a href="tel:+9779765308000" className="hover:text-white">9765308000</a>
            </li>
            <li className="flex items-center gap-2">
              <Globe className="h-4 w-4 shrink-0 text-[#CD2E3A]" />
              <a href="https://dreamkoreasmartclass.com" className="hover:text-white">dreamkoreasmartclass.com</a>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t border-slate-800">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-5 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
          <div>
            &copy; {new Date().getFullYear()} DreamKorea SmartClass. All rights reserved.
          </div>
          <div className="flex items-center gap-2" style={{ fontFamily: "var(--font-noto-kr), sans-serif" }}>
            <span>드림코리아 스마트클래스</span>
            <span className="text-slate-700">&middot;</span>
            <span>Birtamod, Jhapa, Nepal</span>
          </div>
        </div>
      </div>
    </footer>
  );
}

// ─── Small components ─────────────────────────────────────────────────────────
function SectionLabel({ children, center = false, light = false }: { children: React.ReactNode; center?: boolean; light?: boolean }) {
  return (
    <div className={cn("flex items-center gap-2 mb-3", center && "justify-center")}>
      <span className="h-px w-6 bg-[#CD2E3A]" />
      <span className={cn("text-xs font-bold uppercase tracking-[0.18em]", light ? "text-[#FFB4BC]" : "text-[#CD2E3A]")}>
        {children}
      </span>
      <span className="h-px w-6 bg-[#CD2E3A]" />
    </div>
  );
}

function PageHeader({ eyebrow, title, subtitle }: { eyebrow: string; title: string; subtitle: string }) {
  return (
    <section className="bg-[#003478] text-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-16 lg:py-24">
        <SectionLabel light>{eyebrow}</SectionLabel>
        <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold mb-3 tracking-tight">
          {title}
        </h1>
        <p className="text-base sm:text-lg text-white/80 max-w-2xl">{subtitle}</p>
      </div>
    </section>
  );
}

function NoticeCard({ date, title, body, tag }: { date: string; title: string; body: string; tag: string }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between mb-2">
        <span className="inline-flex px-2.5 py-0.5 text-xs font-bold rounded-full bg-[#CD2E3A]/10 text-[#CD2E3A]">
          {tag}
        </span>
        <span className="text-xs text-slate-500">{date}</span>
      </div>
      <h3 className="font-bold text-slate-900 mb-1.5">{title}</h3>
      <p className="text-sm text-slate-600 leading-relaxed">{body}</p>
    </div>
  );
}

function ContactCard({ icon, title, lines, href }: { icon: React.ReactNode; title: string; lines: string[]; href?: string }) {
  const inner = (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 h-full hover:shadow-md transition-shadow">
      <div className="h-10 w-10 rounded-lg bg-[#003478] grid place-items-center text-white mb-4">
        {icon}
      </div>
      <div className="font-bold text-slate-900 mb-2">{title}</div>
      <div className="space-y-0.5 text-sm text-slate-600">
        {lines.map((l, i) => <div key={i}>{l}</div>)}
      </div>
    </div>
  );
  return href ? <a href={href} className="block h-full">{inner}</a> : inner;
}
