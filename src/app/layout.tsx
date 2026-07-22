import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/toaster";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "DreamKorea SmartClass — Learn Korean Online | TOPIK Prep, Exams & Live Classes",
  description:
    "Master Korean with DreamKorea SmartClass. TOPIK preparation, live classes, interactive exams, audio lessons, video tutorials, and digital books. Learn Korean from beginner to advanced.",
  keywords: [
    "DreamKorea", "SmartClass", "Korean language learning", "TOPIK preparation",
    "Korean exam", "learn Korean online", "Korean classes", "Hangul",
    "Korean language institute", "TOPIK I", "TOPIK II", "Korean vocabulary",
    "Korean grammar", "Korean pronunciation", "online Korean course",
  ],
  authors: [{ name: "DreamKorea SmartClass" }],
  manifest: "/manifest.json",
  appleWebApp: { capable: true, statusBarStyle: "default", title: "DreamKorea" },
  formatDetection: { telephone: false },
  openGraph: {
    title: "DreamKorea SmartClass — Learn Korean Online",
    description: "TOPIK prep, live classes, interactive exams, audio & video lessons, digital books.",
    siteName: "DreamKorea SmartClass",
    type: "website",
    locale: "en_US",
  },
  twitter: {
    card: "summary_large_image",
    title: "DreamKorea SmartClass",
    description: "Learn Korean online — TOPIK prep, exams, live classes, and more.",
  },
  robots: {
    index: true,
    follow: true,
    googleBot: { index: true, follow: true, "max-image-preview": "large" },
  },
  alternates: { canonical: "https://dreamkoreasmartclass.com" },
};

export const viewport: Viewport = {
  themeColor: "#10b981",
  width: "device-width",
  initialScale: 1,
  maximumScale: 5,
  viewportFit: "cover",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <link rel="icon" href="/icons/icon-192.png" />
        <link rel="apple-touch-icon" href="/icons/icon-192.png" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="mobile-web-app-capable" content="yes" />
      </head>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-background text-foreground`}
      >
        {children}
        <Toaster />
      </body>
    </html>
  );
}
