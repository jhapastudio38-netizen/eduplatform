package app.dreamkorea.smartclass.ui

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Helper for Google Sign-In using the GoogleSignIn API.
 *
 * IMPORTANT: This requires the SHA-1 fingerprint of the signing keystore
 * to be registered in Firebase Console → Project Settings → Android app.
 *
 * SHA-1: C5:B2:2F:48:68:B1:62:AA:81:23:51:75:FE:FD:B5:49:D0:21:24:1D
 */
object GoogleSignInHelper {

    private const val WEB_CLIENT_ID = "416728228268-rs08fmuts5u4o29lp0hmgcqhtofls22o.apps.googleusercontent.com"

    fun getClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

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
                CommonStatusCodes.DEVELOPER_ERROR -> "SHA-1 not registered in Firebase. Add: C5:B2:2F:48:68:B1:62:AA:81:23:51:75:FE:FD:B5:49:D0:21:24:1D"
                12501 -> "Sign in cancelled"
                12502 -> "Sign in cancelled"
                12500 -> "Sign in failed. Check Firebase SHA-1 config."
                10 -> "SHA-1 not registered in Firebase. Add: C5:B2:2F:48:68:B1:62:AA:81:23:51:75:FE:FD:B5:49:D0:21:24:1D"
                else -> "Sign in error (code ${e.statusCode})"
            }
            GoogleSignInResult.Error(message)
        } catch (e: Exception) {
            GoogleSignInResult.Error("Sign in failed: ${e.message ?: "unknown"}")
        }
    }
}

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
