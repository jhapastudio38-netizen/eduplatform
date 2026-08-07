package app.dreamkorea.smartclass

import android.app.Application
import android.util.Log
import app.dreamkorea.smartclass.data.AppState
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .build()
    }
}
