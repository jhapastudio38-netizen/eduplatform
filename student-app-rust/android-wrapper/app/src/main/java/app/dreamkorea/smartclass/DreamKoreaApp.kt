package app.dreamkorea.smartclass

import android.app.Application
import android.util.Log
import app.dreamkorea.smartclass.data.AppState
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient

class DreamKoreaApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        AppState.init(this)

        // Global crash handler — catches uncaught exceptions in all threads.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DreamKorea", "Uncaught exception on ${thread.name}", throwable)
            if (thread.name == "main") {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    // ─── Coil ImageLoader — optimized for fast image display (FIX-3) ────────
    // • 25% of app memory for in-memory cache
    // • 100MB disk cache (bumped from 50MB)
    // • respectCacheHeaders(false) — cache even when the server doesn't send
    //   cache-control headers (most admin-uploaded images fall into this)
    // • No crossfade — images appear instantly without a fade transition
    // • OkHttp client with 10s connect / 15s read timeout for slow networks
    override fun newImageLoader(): ImageLoader {
        // OkHttp client with sane timeouts for slow mobile networks.
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of app memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB disk cache (FIX-3: bumped from 50MB)
                    .build()
            }
            .respectCacheHeaders(false) // Cache even without cache-control headers
            // No crossfade — faster display (FIX-3: removed .crossfade(true)/.crossfade(200))
            .build()
    }
}
