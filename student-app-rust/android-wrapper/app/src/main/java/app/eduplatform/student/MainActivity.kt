package app.eduplatform.student

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * Main entry point for the EduPlatform student app.
 *
 * Slint's android-activity backend expects a native lib named
 * `libeduplatform_student.so` and an Activity that calls
 * `slint::android::init()` (handled by the native lib's JNI_OnLoad).
 *
 * We just provide the Activity window — Slint takes over rendering.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Full-screen, no title bar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        // Load the native Rust library
        try {
            System.loadLibrary("eduplatform_student")
        } catch (e: UnsatisfiedLinkError) {
            throw RuntimeException("Failed to load native library eduplatform_student", e)
        }
        // Slint's backend-android-activity will render into this window
        // via the android-activity Rust crate's callback.
    }
}
