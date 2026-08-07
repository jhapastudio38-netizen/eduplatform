package app.dreamkorea.smartclass

import android.app.Application
import android.util.Log
import app.dreamkorea.smartclass.data.AppState
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class DreamKoreaApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        AppState.init(this)

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DreamKorea", "Uncaught exception on ${thread.name}", throwable)
            if (thread.name == "main") {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    // ─── Coil ImageLoader optimized for SPEED ──────────────────────
    // - Fast OkHttp client with short timeouts (images are small)
    // - Large memory cache (35% of app memory) for instant repeat loads
    // - Large disk cache (100MB) so images load instantly after first download
    // - No crossfade delay (instant display, no 200ms animation)
    // - Aggressive caching: cache even without cache-control headers
    override fun newImageLoader(): ImageLoader {
        // Fast OkHttp client — short connect/read timeouts for snappy image loading
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(false) // No animation delay — show image immediately
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35) // 35% of app memory for fast repeat loads
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Cache everything permanently
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
