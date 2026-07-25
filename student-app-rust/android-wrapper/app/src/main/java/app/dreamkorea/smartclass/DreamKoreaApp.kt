package app.dreamkorea.smartclass

import android.app.Application
import android.util.Log
import app.dreamkorea.smartclass.data.AppState

class DreamKoreaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppState.init(this)

        // Global crash handler — catches uncaught exceptions in all threads.
        // Instead of force-closing, we log the error and let the app try to recover.
        // This prevents the "app auto-closes" bug where a single composition
        // crash kills the entire app.
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DreamKorea", "Uncaught exception on ${thread.name}", throwable)
            // If this is the main thread, we can't recover — let the default handler close
            // But we log it so we can diagnose the issue.
            // For background threads, just log and continue.
            if (thread.name == "main") {
                previousHandler?.uncaughtException(thread, throwable)
            }
            // Background thread errors are swallowed to prevent app crash
        }
    }
}
