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
  title: "EduPlatform — Learn, Teach, Manage",
  description:
    "A unified education platform for students, teachers, and admins. Learn, take tests, go live, and manage content.",
  keywords: [
    "education", "learning", "online classes", "tests", "exams",
    "teachers", "students", "live class", "EduPlatform",
  ],
  authors: [{ name: "EduPlatform" }],
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "EduPlatform",
  },
  formatDetection: { telephone: false },
  openGraph: {
    title: "EduPlatform — Learn, Teach, Manage",
    description: "A unified education platform for students, teachers, and admins.",
    siteName: "EduPlatform",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "EduPlatform",
    description: "A unified education platform for students, teachers, and admins.",
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
