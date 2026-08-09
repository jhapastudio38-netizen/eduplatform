package app.dreamkorea.smartclass.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Helper for Google Sign-In using Clerk's REST API directly.
 *
 * No Clerk Android SDK needed — just plain HTTP calls to Clerk's backend.
 * This works with any minSdk and any build toolchain.
 *
 * Flow:
 * 1. Create a Clerk sign-in with Google OAuth strategy
 * 2. Get the external verification redirect URL from Clerk
 * 3. Open it in Chrome Custom Tab
 * 4. User signs in with Google
 * 5. Google redirects back to Clerk, Clerk redirects to dreamkorea://auth-callback
 * 6. App captures the redirect and polls Clerk for the session
 */
object GoogleSignInHelper {

    private const val CLERK_FRONTEND_API = "https://champion-sole-99.clerk.accounts.dev"
    private const val CLERK_PUBLISHABLE_KEY = "pk_test_Y2hhbXBpb24tc29sZS05OS5jbGVyay5hY2NvdW50cy5kZXYk"
    private const val REDIRECT_URL = "dreamkorea://auth-callback"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Starts Google Sign-In via Clerk.
     * Creates a Clerk sign-in, gets the Google OAuth URL, and opens it in browser.
     * After the user completes sign-in, they'll be redirected back to the app.
     */
    fun signInWithGoogle(context: Context, callback: (GoogleSignInResult) -> Unit) {
        Thread {
            try {
                // Step 1: Create a sign-in with Google OAuth strategy
                val jsonBody = JsonObject().apply {
                    addProperty("strategy", "oauth_google")
                    addProperty("redirect_url", REDIRECT_URL)
                }.toString()

                val request = Request.Builder()
                    .url("$CLERK_FRONTEND_API/v1/client/sign_ins")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", CLERK_PUBLISHABLE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    callback(GoogleSignInResult.Error("Could not start Google sign-in (${response.code})"))
                    return@Thread
                }

                // Step 2: Extract the external verification redirect URL
                val json = gson.fromJson(responseBody, JsonObject::class.java)
                val responseObj = json.getAsJsonObject("response")

                // Find the first factor verification URL
                val firstFactorVerification = responseObj?.getAsJsonObject("first_factor_verification")
                val externalUrl = firstFactorVerification?.getAsJsonPrimitive("external_verification_redirect_url")?.asString

                if (externalUrl != null) {
                    // Step 3: Open the Google OAuth URL in Chrome Custom Tab
                    val customTabsIntent = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                    customTabsIntent.launchUrl(context, Uri.parse(externalUrl))
                    callback(GoogleSignInResult.Pending)
                } else {
                    callback(GoogleSignInResult.Error("No Google OAuth URL received from Clerk"))
                }
            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Clerk sign-in failed", e)
                callback(GoogleSignInResult.Error("Sign-in failed: ${e.message ?: "unknown"}"))
            }
        }.start()
    }

    /**
     * After the user returns from the browser, check if they're now signed in.
     * Polls Clerk's client API to see if a session was created.
     */
    fun checkSession(callback: (GoogleSignInResult) -> Unit) {
        Thread {
            try {
                val request = Request.Builder()
                    .url("$CLERK_FRONTEND_API/v1/client")
                    .get()
                    .addHeader("Authorization", CLERK_PUBLISHABLE_KEY)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    callback(GoogleSignInResult.Error("Could not check session"))
                    return@Thread
                }

                val json = gson.fromJson(responseBody, JsonObject::class.java)
                val responseObj = json.getAsJsonObject("response")
                val lastActiveSession = responseObj?.getAsJsonObject("last_active_session")

                if (lastActiveSession != null) {
                    val sessionToken = lastActiveSession.getAsJsonPrimitive("token")?.asString
                    if (sessionToken != null) {
                        callback(GoogleSignInResult.Success(sessionToken))
                    } else {
                        callback(GoogleSignInResult.Error("Session exists but no token"))
                    }
                } else {
                    callback(GoogleSignInResult.Error("No active session"))
                }
            } catch (e: Exception) {
                callback(GoogleSignInResult.Error("Session check failed: ${e.message ?: "unknown"}"))
            }
        }.start()
    }
}

sealed class GoogleSignInResult {
    data object Pending : GoogleSignInResult()
    data class Success(val token: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
}
