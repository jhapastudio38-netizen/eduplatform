"use client";

import { useState, useRef } from "react";
import { Upload, X, Loader2 } from "lucide-react";
import { toast } from "sonner";

interface FastImageUploadProps {
  url: string;
  onUpload: (url: string) => void;
  onClear: () => void;
  folder?: string;
  label?: string;
  className?: string;
  previewClassName?: string;
}

/**
 * Reusable image upload with:
 * - Drag & drop
 * - Instant local preview (blob URL) before upload completes
 * - Image compression (max 800px, 80% JPEG) for faster uploads
 * - Loading spinner overlay during upload
 * - Remove button
 */
export function FastImageUpload({
  url,
  onUpload,
  onClear,
  folder = "images",
  label,
  className = "",
  previewClassName = "",
}: FastImageUploadProps) {
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [localPreview, setLocalPreview] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  async function compressImage(file: File): Promise<File> {
    if (file.size < 200_000) return file;
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          const canvas = document.createElement("canvas");
          const maxDim = 800;
          let { width, height } = img;
          if (width > maxDim || height > maxDim) {
            if (width > height) { height = (height / width) * maxDim; width = maxDim; }
            else { width = (width / height) * maxDim; height = maxDim; }
          }
          canvas.width = width; canvas.height = height;
          const ctx = canvas.getContext("2d")!;
          ctx.drawImage(img, 0, 0, width, height);
          canvas.toBlob((blob) => {
            if (blob) resolve(new File([blob], "image.jpg", { type: "image/jpeg" }));
            else resolve(file);
          }, "image/jpeg", 0.8);
        };
        img.src = e.target?.result as string;
      };
      reader.readAsDataURL(file);
    });
  }

  async function handleFile(file: File) {
    // Instant local preview
    const localUrl = URL.createObjectURL(file);
    setLocalPreview(localUrl);
    setUploading(true);
    try {
      const compressed = await compressImage(file);
      const fd = new FormData();
      fd.append("file", compressed);
      fd.append("folder", folder);
      const res = await fetch("/api/admin/file-upload", { method: "POST", body: fd });
      if (!res.ok) { const d = await res.json(); throw new Error(d.error || "Upload failed"); }
      const d = await res.json();
      onUpload(d.url);
      toast.success("Image uploaded");
    } catch (err: any) {
      toast.error(err.message || "Upload failed");
      setLocalPreview("");
    } finally {
      setUploading(false);
      setTimeout(() => URL.revokeObjectURL(localUrl), 1000);
    }
  }

  const displayUrl = uploading && localPreview ? localPreview : url;

  return (
    <div className={className}>
      {label && <label className="text-sm font-semibold mb-2 block">{label}</label>}

      {displayUrl ? (
        <div className="relative inline-block">
          <img
            src={displayUrl}
            alt="Preview"
            className={`rounded-lg border-2 border-slate-200 object-cover ${previewClassName || "w-32 h-32"}`}
          />
          {uploading && (
            <div className="absolute inset-0 bg-black/40 rounded-lg flex items-center justify-center">
              <Loader2 className="w-6 h-6 text-white animate-spin" />
            </div>
          )}
          {!uploading && (
            <button
              onClick={() => { onClear(); setLocalPreview(""); }}
              className="absolute -top-2 -right-2 w-7 h-7 bg-red-500 text-white rounded-full flex items-center justify-center text-sm font-bold hover:bg-red-600 shadow z-10"
              title="Remove"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      ) : (
        <div
          onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => {
            e.preventDefault();
            setDragOver(false);
            const f = e.dataTransfer.files?.[0];
            if (f) handleFile(f);
          }}
          onClick={() => inputRef.current?.click()}
          className={`flex flex-col items-center justify-center border-2 border-dashed rounded-lg cursor-pointer transition-all ${
            previewClassName ? "" : "w-full h-32"
          } ${
            dragOver ? "border-primary bg-primary/10 scale-[1.02]" : "border-slate-300 hover:border-primary hover:bg-slate-50"
          }`}
          style={previewClassName ? { width: "128px", height: "128px" } : {}}
        >
          <input
            ref={inputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={(e) => {
              const f = e.target.files?.[0];
              if (f) handleFile(f);
              e.target.value = "";
            }}
          />
          <Upload className={`text-slate-400 mb-1 ${previewClassName ? "w-6 h-6" : "w-7 h-7"}`} />
          <span className="text-xs text-slate-500 font-medium text-center px-2">
            {dragOver ? "Drop here" : "Drag & drop or click"}
          </span>
        </div>
      )}
    </div>
  );
}
