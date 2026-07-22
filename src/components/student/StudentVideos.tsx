"use client";

/**
 * Student video player — YouTube videos embedded in-app.
 * Students browse video lessons by level/category and watch without
 * leaving the DreamKorea app.
 */

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Video, Play, Clock, Eye, Search, X } from "lucide-react";

interface VideoLesson {
  id: string;
  title: string;
  description?: string;
  youtubeId: string;
  thumbnailUrl?: string;
  durationMin: number;
  level?: string;
  category?: string;
  views: number;
}

export function StudentVideos() {
  const [videos, setVideos] = useState<VideoLesson[]>([]);
  const [playing, setPlaying] = useState<VideoLesson | null>(null);
  const [search, setSearch] = useState("");
  const [filterLevel, setFilterLevel] = useState("all");

  useEffect(() => {
    fetch("/api/student/video-lessons")
      .then(r => r.json())
      .then(d => setVideos(d.videos || []))
      .catch(() => {});
  }, []);

  const filtered = videos.filter(v => {
    if (search && !v.title.toLowerCase().includes(search.toLowerCase())) return false;
    if (filterLevel !== "all" && v.level !== filterLevel) return false;
    return true;
  });

  if (playing) {
    return (
      <div className="space-y-4">
        <Button variant="ghost" size="sm" onClick={() => setPlaying(null)}>
          <X className="mr-1 h-4 w-4" /> Back to videos
        </Button>
        <div className="aspect-video rounded-xl overflow-hidden bg-black">
          <iframe
            src={`https://www.youtube.com/embed/${playing.youtubeId}?rel=0&autoplay=1`}
            title={playing.title}
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
            className="w-full h-full"
          />
        </div>
        <div>
          <h2 className="text-xl font-bold">{playing.title}</h2>
          {playing.description && <p className="text-sm text-muted-foreground mt-1">{playing.description}</p>}
          <div className="flex items-center gap-3 mt-2 text-xs text-muted-foreground">
            {playing.level && <Badge variant="secondary">{playing.level}</Badge>}
            {playing.category && <Badge variant="outline">{playing.category}</Badge>}
            <span className="flex items-center gap-1"><Clock className="h-3 w-3" /> {playing.durationMin} min</span>
            <span className="flex items-center gap-1"><Eye className="h-3 w-3" /> {playing.views} views</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold">Video Lessons</h2>

      <div className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input placeholder="Search videos..." value={search} onChange={e => setSearch(e.target.value)} className="pl-9" />
        </div>
        <select className="px-3 py-2 rounded-md border bg-background text-sm" value={filterLevel} onChange={e => setFilterLevel(e.target.value)}>
          <option value="all">All Levels</option>
          <option value="Beginner">Beginner</option>
          <option value="Intermediate">Intermediate</option>
          <option value="Advanced">Advanced</option>
        </select>
      </div>

      {filtered.length === 0 ? (
        <Card><CardContent className="p-8 text-center text-muted-foreground">
          <Video className="h-10 w-10 mx-auto mb-2 opacity-40" />
          <p>No video lessons available yet.</p>
        </CardContent></Card>
      ) : (
        <div className="grid sm:grid-cols-2 gap-3">
          {filtered.map(v => (
            <Card key={v.id} className="hover:shadow-md transition cursor-pointer overflow-hidden" onClick={() => setPlaying(v)}>
              {v.thumbnailUrl && (
                <div className="relative h-32">
                  <img src={v.thumbnailUrl} alt={v.title} className="w-full h-full object-cover" />
                  <div className="absolute inset-0 bg-black/30 grid place-items-center">
                    <Play className="h-8 w-8 text-white" fill="white" />
                  </div>
                </div>
              )}
              <CardContent className="p-3">
                <div className="font-semibold text-sm truncate">{v.title}</div>
                <div className="flex items-center gap-2 mt-1">
                  {v.level && <Badge variant="secondary" className="text-xs">{v.level}</Badge>}
                  {v.category && <Badge variant="outline" className="text-xs">{v.category}</Badge>}
                </div>
                <div className="text-xs text-muted-foreground mt-1">{v.durationMin} min · {v.views} views</div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
