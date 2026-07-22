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
  title: "DreamKorea SmartClass — Learn, Teach, Manage",
  description:
    "Smart learning platform for Korean education — exams, question bank, batches, courses, and live classes.",
  keywords: [
    "DreamKorea", "SmartClass", "Korean education", "exams", "question bank",
    "teachers", "students", "live class", "TOPIK", "Korean language",
  ],
  authors: [{ name: "DreamKorea SmartClass" }],
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "DreamKorea",
  },
  formatDetection: { telephone: false },
  openGraph: {
    title: "DreamKorea SmartClass",
    description: "Smart learning platform for Korean education.",
    siteName: "DreamKorea SmartClass",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "DreamKorea SmartClass",
    description: "Smart learning platform for Korean education.",
  },
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
