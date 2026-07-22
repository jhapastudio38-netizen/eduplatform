package app.dreamkorea.smartclass.data

import android.content.Context
import app.dreamkorea.smartclass.api.DreamKoreaApi
import app.dreamkorea.smartclass.api.User
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppState {
    private const val BASE_URL = "http://eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com/"
    private const val PREFS_NAME = "dreamkorea_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val api: DreamKoreaApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = prefs.getString(KEY_TOKEN, "") ?: ""
                val req = chain.request().newBuilder()
                if (token.isNotEmpty()) {
                    req.addHeader("Cookie", "ep_sid=$token")
                }
                chain.proceed(req.build())
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DreamKoreaApi::class.java)
    }

    fun saveSession(token: String, user: User) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_NAME, user.name ?: "Student")
            putString(KEY_EMAIL, user.email)
            putString(KEY_ROLE, user.role)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getString(KEY_TOKEN, "")?.isNotEmpty() == true
    fun getUserName(): String = prefs.getString(KEY_NAME, "Student") ?: "Student"
    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""
}
