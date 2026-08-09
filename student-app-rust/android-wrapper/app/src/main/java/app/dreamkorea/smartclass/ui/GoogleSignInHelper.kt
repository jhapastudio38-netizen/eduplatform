package app.dreamkorea.smartclass.ui

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Helper for Google Sign-In using Clerk hosted on our Vercel backend.
 *
 * Opens the Vercel-hosted Clerk sign-in page in Chrome Custom Tab.
 * After sign-in, the backend redirects to dreamkorea://auth-callback
 * with the user data.
 *
 * This approach:
 * - No Clerk Android SDK needed
 * - No Firebase SHA-1 needed
 * - Uses Clerk's hosted SignIn component on our Next.js backend
 * - Works with any minSdk
 */
object GoogleSignInHelper {

    // Our Vercel-hosted Clerk sign-in page
    private const val SIGN_IN_URL = "https://my-project-five-sepia.vercel.app/sign-in"

    /**
     * Opens the Clerk sign-in page in Chrome Custom Tab.
     * After sign-in, user will be redirected back to the app.
     */
    fun signInWithGoogle(context: Context) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(SIGN_IN_URL))
        } catch (e: Exception) {
            // Fallback: open in default browser
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                Uri.parse(SIGN_IN_URL)
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

sealed class GoogleSignInResult {
    data object Pending : GoogleSignInResult()
    data class Success(val token: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
