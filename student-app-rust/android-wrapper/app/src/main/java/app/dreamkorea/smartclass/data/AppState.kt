package app.dreamkorea.smartclass.data

import android.content.Context
import app.dreamkorea.smartclass.api.DreamKoreaApi
import app.dreamkorea.smartclass.api.User
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

object AppState {
    private const val BASE_URL = "http://eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com/"
    private const val PREFS_NAME = "dreamkorea_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_ROLE = "role"

    private lateinit var prefs: android.content.SharedPreferences
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()
    private lateinit var baseUrl: HttpUrl

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        baseUrl = HttpUrl.parse(BASE_URL)!!
        val savedToken = prefs.getString(KEY_TOKEN, null)
        if (savedToken != null) {
            val cookie = Cookie.Builder()
                .name("ep_sid")
                .value(savedToken)
                .domain(baseUrl.host)
                .path("/")
                .build()
            cookieStore[baseUrl.host] = mutableListOf(cookie)
        }
    }

    val api: DreamKoreaApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val host = url.host
                val store = cookieStore.getOrPut(host) { mutableListOf() }
                for (cookie in cookies) {
                    store.removeAll { it.name == cookie.name }
                    store.add(cookie)
                    if (cookie.name == "ep_sid") {
                        prefs.edit().putString(KEY_TOKEN, cookie.value).apply()
                    }
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        }

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
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
        val cookie = Cookie.Builder()
            .name("ep_sid")
            .value(token)
            .domain(baseUrl.host)
            .path("/")
            .build()
        cookieStore[baseUrl.host] = mutableListOf(cookie)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        cookieStore.clear()
    }

    fun isLoggedIn(): Boolean = prefs.getString(KEY_TOKEN, null) != null
    fun getUserName(): String = prefs.getString(KEY_NAME, "Student") ?: "Student"
    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""
}
