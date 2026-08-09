package app.dreamkorea.smartclass.ui

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

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
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Extract the ID token from a GoogleSignInAccount result.
     * Returns a Result object with either the token or an error message.
     */
    fun getIdTokenFromResult(resultData: android.content.Intent?): GoogleSignInResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(resultData)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token != null) {
                GoogleSignInResult.Success(token)
            } else {
                GoogleSignInResult.Error("No ID token received from Google")
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                CommonStatusCodes.CANCELED -> "Sign in cancelled"
                CommonStatusCodes.NETWORK_ERROR -> "Network error. Check your connection."
                CommonStatusCodes.SIGN_IN_REQUIRED -> "Sign in required"
                CommonStatusCodes.INVALID_ACCOUNT -> "Invalid account"
                CommonStatusCodes.DEVELOPER_ERROR -> "Configuration error. Contact support."
                12501 -> "Sign in cancelled"
                12502 -> "Sign in cancelled"
                12500 -> "Sign in failed. Try again."
                else -> "Sign in error (code ${e.statusCode})"
            }
            GoogleSignInResult.Error(message)
        } catch (e: Exception) {
            GoogleSignInResult.Error("Sign in failed: ${e.message ?: "unknown error"}")
        }
    }
}

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
