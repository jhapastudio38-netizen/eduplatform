/**
 * Generate PWA icons + splash screens.
 * Run: bun run scripts/gen-icons.ts
 */
import sharp from "sharp";
import { mkdir, writeFile } from "fs/promises";
import { join } from "path";

const ICONS_DIR = "/home/z/my-project/public/icons";

async function main() {
  await mkdir(ICONS_DIR, { recursive: true });

  // Master 512x512 logo — emerald gradient with graduation cap silhouette
  const logo512 = await makeLogo(512);
  await writeFile(join(ICONS_DIR, "icon-512.png"), logo512);

  // 192x192 standard
  await writeFile(join(ICONS_DIR, "icon-192.png"), await makeLogo(192));

  // 512 maskable (with padding)
  await writeFile(join(ICONS_DIR, "icon-512-maskable.png"), await makeLogo(512, true));

  // Apple touch icon (180)
  await writeFile(join(ICONS_DIR, "apple-touch-icon.png"), await makeLogo(180));

  // Favicons
  await writeFile(join(ICONS_DIR, "favicon-32.png"), await makeLogo(32));
  await writeFile(join(ICONS_DIR, "favicon-16.png"), await makeLogo(16));

  // Mobile splash screen (1080x1920)
  await writeFile(join(ICONS_DIR, "screenshot-mobile.png"), await makeSplash(1080, 1920));

  console.log("Icons generated in", ICONS_DIR);
}

async function makeLogo(size: number, maskable = false): Promise<Buffer> {
  // SVG → PNG via sharp
  const padding = maskable ? size * 0.2 : 0;
  const inner = size - padding * 2;
  const cap = graduationCapSvg(inner);
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 ${size} ${size}">
    <defs>
      <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stop-color="#10b981"/>
        <stop offset="100%" stop-color="#0d9488"/>
      </linearGradient>
    </defs>
    <rect width="${size}" height="${size}" rx="${maskable ? 0 : size * 0.18}" fill="url(#bg)"/>
    <g transform="translate(${padding}, ${padding})">
      ${cap}
    </g>
  </svg>`;
  return sharp(Buffer.from(svg)).png().toBuffer();
}

function graduationCapSvg(size: number): string {
  // Simple graduation cap silhouette
  const s = size * 0.5;
  const cx = size / 2;
  const cy = size / 2;
  return `<g transform="translate(${cx - s / 2}, ${cy - s / 2})" fill="white">
    <path d="M${s / 2} 0 L${s} ${s * 0.25} L${s / 2} ${s * 0.5} L0 ${s * 0.25} Z"/>
    <path d="M${s * 0.15} ${s * 0.35} L${s * 0.15} ${s * 0.65} Q${s / 2} ${s * 0.8} ${s * 0.85} ${s * 0.65} L${s * 0.85} ${s * 0.35} L${s / 2} ${s * 0.5} Z"/>
    <line x1="${s}" y1="${s * 0.25}" x2="${s}" y2="${s * 0.55}" stroke="white" stroke-width="${s * 0.04}"/>
    <circle cx="${s}" cy="${s * 0.6}" r="${s * 0.04}" fill="white"/>
  </g>`;
}

async function makeSplash(w: number, h: number): Promise<Buffer> {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">
    <rect width="${w}" height="${h}" fill="#10b981"/>
    <g transform="translate(${w / 2 - 200}, ${h / 2 - 250})">
      ${graduationCapSvg(400)}
    </g>
    <text x="${w / 2}" y="${h / 2 + 250}" font-family="-apple-system, sans-serif" font-size="64" font-weight="700" fill="white" text-anchor="middle">EduPlatform</text>
  </svg>`;
  return sharp(Buffer.from(svg)).png().toBuffer();
}

main().catch(console.error);
