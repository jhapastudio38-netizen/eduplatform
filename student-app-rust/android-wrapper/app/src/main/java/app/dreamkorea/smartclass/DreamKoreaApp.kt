package app.dreamkorea.smartclass

import android.app.Application
import android.util.Log
import app.dreamkorea.smartclass.data.AppState
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

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

    // ─── Coil ImageLoader with disk + memory caching ──────────────────────
    // Uses a custom OkHttp client that fixes wrong Content-Type headers.
    // Some image servers (WordPress) return text/html for .jpg files,
    // which prevents Coil from decoding them. This interceptor overrides
    // the Content-Type based on the URL file extension.
    override fun newImageLoader(): ImageLoader {
        // Custom OkHttp client with Content-Type fix interceptor
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(ContentTypeFixInterceptor())
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .crossfade(200)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of app memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Cache even without cache-control headers
            .build()
    }
}

/**
 * Interceptor that fixes wrong Content-Type headers.
 * Some servers (e.g. WordPress) return "text/html" for image files (.jpg, .png, etc).
 * Coil checks Content-Type and refuses to decode if it's not an image type.
 * This interceptor overrides the Content-Type based on the URL file extension.
 */
class ContentTypeFixInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val url = request.url.toString().lowercase()
        val contentType = response.header("Content-Type") ?: ""

        // If the URL ends with an image extension but Content-Type is not image/*,
        // override it to the correct image type
        if (contentType.contains("text/html") || contentType.contains("application/octet-stream")) {
            val newContentType = when {
                url.endsWith(".jpg") || url.endsWith(".jpeg") -> "image/jpeg"
                url.endsWith(".png") -> "image/png"
                url.endsWith(".gif") -> "image/gif"
                url.endsWith(".webp") -> "image/webp"
                url.endsWith(".svg") -> "image/svg+xml"
                url.endsWith(".bmp") -> "image/bmp"
                else -> null
            }
            if (newContentType != null) {
                // Rebuild the response with the correct Content-Type
                val newHeaders = response.headers.newBuilder()
                    .set("Content-Type", newContentType)
                    .build()
                return response.newBuilder()
                    .headers(newHeaders)
                    .build()
            }
        }

        return response
    }
}
