package app.dreamkorea.smartclass.ui

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Helper for Google Sign-In.
 * The Web Client ID is from Google Cloud Console (OAuth 2.0 Client ID).
 */
object GoogleSignInHelper {

    // Web Client ID from Google Cloud Console
    private const val WEB_CLIENT_ID = "416728228268-rs08fmuts5u4o29lp0hmgcqhtofls22o.apps.googleusercontent.com"

    /**
     * Get the GoogleSignInClient configured for our app.
     */
    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Extract the ID token from a GoogleSignInAccount result.
     * Returns null if sign-in failed or was cancelled.
     */
    fun getIdTokenFromResult(resultData: android.content.Intent?): String? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(resultData)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            account.idToken
        } catch (e: ApiException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
