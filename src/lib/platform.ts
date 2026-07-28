// Platform branding configuration
// Each platform has different name, colors, pricing, but shares the same
// database, backend code, and authentication system.

export const PLATFORM_CONFIG = {
  // Default (DreamKorea SmartClass)
  dreamkorea: {
    name: "DreamKorea SmartClass",
    shortName: "DreamKorea",
    primaryColor: "#003478",
    accentColor: "#3B82F6",
    tagline: "Learn Korean Online",
    monthlyNpr: 0, // free
    yearlyNpr: 0,
    isDefault: true,
  },
  koreanpro: {
    name: "KoreanPro Basic",
    shortName: "KoreanPro",
    primaryColor: "#10B981",
    accentColor: "#34D399",
    tagline: "Affordable Korean Learning",
    monthlyNpr: 299,
    yearlyNpr: 2999,
    isDefault: false,
  },
  topikmaster: {
    name: "TopikMaster Standard",
    shortName: "TopikMaster",
    primaryColor: "#3B82F6",
    accentColor: "#60A5FA",
    tagline: "TOPIK Prep Made Easy",
    monthlyNpr: 599,
    yearlyNpr: 5999,
    isDefault: false,
  },
  klearn: {
    name: "KLearn Pro",
    shortName: "KLearn",
    primaryColor: "#8B5CF6",
    accentColor: "#A78BFA",
    tagline: "All-Rounder Korean Mastery",
    monthlyNpr: 999,
    yearlyNpr: 9999,
    isDefault: false,
  },
  hangulhub: {
    name: "HanGulHub Ultimate",
    shortName: "HanGulHub",
    primaryColor: "#F59E0B",
    accentColor: "#FBBF24",
    tagline: "Premium Korean Learning",
    monthlyNpr: 1499,
    yearlyNpr: 14999,
    isDefault: false,
  },
};

export type PlatformKey = keyof typeof PLATFORM_CONFIG;

export function getPlatform(): PlatformKey {
  if (typeof window === "undefined") return "dreamkorea";
  const host = window.location.hostname;
  if (host.includes("koreanpro")) return "koreanpro";
  if (host.includes("topikmaster")) return "topikmaster";
  if (host.includes("klearn")) return "klearn";
  if (host.includes("hangulhub")) return "hangulhub";
  return "dreamkorea";
}

export function getPlatformConfig() {
  return PLATFORM_CONFIG[getPlatform()];
}
