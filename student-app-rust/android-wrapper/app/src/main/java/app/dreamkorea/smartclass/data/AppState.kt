package app.dreamkorea.smartclass.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import app.dreamkorea.smartclass.api.DreamKoreaApi
import app.dreamkorea.smartclass.api.User
import app.dreamkorea.smartclass.api.HomeCard
import app.dreamkorea.smartclass.api.TestItem
import app.dreamkorea.smartclass.api.Book
import app.dreamkorea.smartclass.api.VideoLesson
import app.dreamkorea.smartclass.api.AudioLesson
import app.dreamkorea.smartclass.api.QuestionBankQuestion
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AppState {
    private const val BASE_URL = "https://my-project-five-sepia.vercel.app/"
    private const val PREFS_NAME = "dreamkorea_session"
    private const val SETTINGS_PREFS = "dreamkorea_settings"
    private const val KEY_TOKEN = "token"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHONE = "phone"
    private const val KEY_ROLE = "role"

    // Settings keys
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_ANIMATIONS = "animations_enabled"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var settingsPrefs: android.content.SharedPreferences
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()
    private lateinit var baseUrl: HttpUrl

    // ─── In-memory cache (fixes back/forth reload storms) ──────────────────────
    // Each entry stores (data, timestamp). Cache is valid for CACHE_TTL_MS.
    private const val CACHE_TTL_MS = 60_000L // 1 minute
    private data class CacheEntry<T>(val data: T, val savedAt: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()
    private val cacheMutex = Mutex()

    private suspend fun <T> cached(key: String, loader: suspend () -> T): T {
        val now = System.currentTimeMillis()
        @Suppress("UNCHECKED_CAST")
        val hit = cache[key] as? CacheEntry<T>
        if (hit != null && now - hit.savedAt < CACHE_TTL_MS) {
            return hit.data
        }
        val fresh = loader()
        cache[key] = CacheEntry(fresh, System.currentTimeMillis())
        return fresh
    }

    /** Force-invalidate a cache key (call after a mutation or pull-to-refresh). */
    fun invalidateCache(key: String? = null) {
        if (key == null) cache.clear() else cache.remove(key)
    }

    // Cached API helpers — used by screens so navigating back doesn't refetch.
    suspend fun getCachedHomeCards() = cached("home_cards") {
        AppState.api.getHomeCards().cards
    }
    suspend fun getCachedTests(filter: String) = cached("tests_$filter") {
        AppState.api.getTests(filter).tests
    }
    suspend fun getCachedBooks() = cached("books") {
        AppState.api.getBooks().books
    }
    suspend fun getCachedVideos() = cached("videos") {
        AppState.api.getVideoLessons().videos
    }
    suspend fun getCachedAudio() = cached("audio") {
        AppState.api.getAudioLessons().lessons
    }
    suspend fun getCachedQuestionBank() = cached("question_bank") {
        AppState.api.getQuestionBank().questions
    }

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        settingsPrefs = context.applicationContext.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        baseUrl = BASE_URL.toHttpUrl()
        val savedToken = prefs.getString(KEY_TOKEN, null)
        if (savedToken != null && savedToken != "session_via_cookie") {
            // Restore the real session cookie from persisted token
            val cookie = Cookie.Builder()
                .name("ep_sid")
                .value(savedToken)
                .domain(baseUrl.host)
                .path("/")
                .secure()
                .httpOnly()
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
            putString(KEY_PHONE, user.phone ?: "")
            putString(KEY_ROLE, user.role)
            apply()
        }
        val cookie = Cookie.Builder()
            .name("ep_sid")
            .value(token)
            .domain(baseUrl.host)
            .path("/")
            .secure()
            .httpOnly()
            .build()
        cookieStore[baseUrl.host] = mutableListOf(cookie)
    }

    /**
     * Save only the user profile (name/email/phone/role) WITHOUT touching the
     * session token. Used after OTP login — the real ep_sid cookie is already
     * captured by the OkHttp CookieJar from the server's Set-Cookie header.
     * Calling saveSession() with a fake token would overwrite the real cookie
     * and break all authenticated requests.
     */
    fun saveUserProfile(user: User) {
        prefs.edit().apply {
            putString(KEY_NAME, user.name ?: "Student")
            putString(KEY_EMAIL, user.email)
            putString(KEY_PHONE, user.phone ?: "")
            putString(KEY_ROLE, user.role)
            // Mark that we have a session (token presence is checked by the cookie jar)
            // We store a marker so isLoggedIn() returns true.
            if (prefs.getString(KEY_TOKEN, null) == null) {
                putString(KEY_TOKEN, "session_via_cookie")
            }
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        cookieStore.clear()
    }

    fun isLoggedIn(): Boolean = prefs.getString(KEY_TOKEN, null) != null
    fun getUserName(): String = prefs.getString(KEY_NAME, "Student") ?: "Student"
    fun getUserEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getUserPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""
    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""

    val user: User?
        get() = if (isLoggedIn()) User(
            id = "",
            name = getUserName(),
            email = getUserEmail(),
            phone = if (getUserPhone().isNotEmpty()) getUserPhone() else null,
            role = prefs.getString(KEY_ROLE, "STUDENT") ?: "STUDENT"
        ) else null

    // ─── Settings ──────────────────────────────────────────────────────────────

    /** Returns the user's chosen theme color as a Color (default: Korean flag blue). */
    fun getThemeColor(): Color {
        val hex = settingsPrefs.getString(KEY_THEME_COLOR, "003478") ?: "003478"
        return try { Color(parseHex(hex)) } catch (_: Exception) { Color(0xFF003478) }
    }
    fun setThemeColor(hex: String) {
        settingsPrefs.edit().putString(KEY_THEME_COLOR, hex).apply()
    }

    fun isDarkMode(): Boolean = settingsPrefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(value: Boolean) {
        settingsPrefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
    }

    /** Returns text size multiplier: 0.85 (small), 1.0 (normal), 1.15 (large), 1.3 (extra large). */
    fun getTextSizeMultiplier(): Float = settingsPrefs.getFloat(KEY_TEXT_SIZE, 1.0f)
    fun setTextSizeMultiplier(value: Float) {
        settingsPrefs.edit().putFloat(KEY_TEXT_SIZE, value).apply()
    }

    fun areAnimationsEnabled(): Boolean = settingsPrefs.getBoolean(KEY_ANIMATIONS, true)
    fun setAnimationsEnabled(value: Boolean) {
        settingsPrefs.edit().putBoolean(KEY_ANIMATIONS, value).apply()
    }

    fun areNotificationsEnabled(): Boolean = settingsPrefs.getBoolean(KEY_NOTIFICATIONS, true)
    fun setNotificationsEnabled(value: Boolean) {
        settingsPrefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
    }

    /** Parse a 6-char hex color like "CD2E3A" into an ARGB long. */
    private fun parseHex(hex: String): Long {
        val clean = hex.removePrefix("#").uppercase()
        return ("FF$clean").toLong(16)
    }
}
