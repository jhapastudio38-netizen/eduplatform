package app.dreamkorea.smartclass.ui

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Helper for Google Sign-In using the traditional GoogleSignIn API.
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
     * Returns a Result object with either the token or a detailed error message.
     */
    fun getIdTokenFromResult(resultData: android.content.Intent?): GoogleSignInResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(resultData)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token != null) {
                GoogleSignInResult.Success(token)
            } else {
                GoogleSignInResult.Error("No ID token received. Check Firebase SHA-1 config.")
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                CommonStatusCodes.CANCELED -> "Sign in cancelled"
                CommonStatusCodes.NETWORK_ERROR -> "Network error. Check your connection."
                CommonStatusCodes.SIGN_IN_REQUIRED -> "Sign in required"
                CommonStatusCodes.INVALID_ACCOUNT -> "Invalid account"
                CommonStatusCodes.DEVELOPER_ERROR -> "Configuration error: SHA-1 fingerprint not registered in Firebase. Go to Firebase Console → Project Settings → Android app → Add SHA-1: C5:B2:2F:48:68:B1:62:AA:81:23:51:75:FE:FD:B5:49:D0:21:24:1D"
                12501 -> "Sign in cancelled"
                12502 -> "Sign in cancelled"
                12500 -> "Sign in failed. Check Firebase SHA-1 config."
                10 -> "Configuration error: SHA-1 not registered in Firebase. Add: C5:B2:2F:48:68:B1:62:AA:81:23:51:75:FE:FD:B5:49:D0:21:24:1D"
                else -> "Sign in error (code ${e.statusCode}): ${e.message}"
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
