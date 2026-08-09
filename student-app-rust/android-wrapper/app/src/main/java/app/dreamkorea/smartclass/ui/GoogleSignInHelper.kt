package app.dreamkorea.smartclass.ui

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Helper for Google Sign-In using Clerk's web-based OAuth flow.
 *
 * Opens a Chrome Custom Tab to Clerk's sign-in page with Google OAuth.
 * After sign-in, Clerk redirects back to the app via deep link,
 * which the app captures and sends to the backend.
 *
 * This approach does NOT require:
 * - Firebase SHA-1 fingerprint
 * - Google Play Services Auth
 * - Clerk Android SDK (which needs minSdk 26+)
 *
 * It only needs:
 * - Clerk publishable key (already set in backend)
 * - Chrome Custom Tabs (androidx.browser)
 */
object GoogleSignInHelper {

    // Clerk frontend API URL — this opens Clerk's hosted sign-in page
    private const val CLERK_SIGN_IN_URL = "https://champion-sole-99.clerk.accounts.dev/sign-in?strategy=oauth_google"

    /**
     * Opens Clerk's Google Sign-In page in a Chrome Custom Tab.
     * After the user signs in, Clerk will redirect to the app's deep link.
     */
    fun signInWithGoogle(context: Context) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(CLERK_SIGN_IN_URL))
        } catch (e: Exception) {
            // Fallback: open in browser
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(CLERK_SIGN_IN_URL))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

sealed class GoogleSignInResult {
    data class Success(val token: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
