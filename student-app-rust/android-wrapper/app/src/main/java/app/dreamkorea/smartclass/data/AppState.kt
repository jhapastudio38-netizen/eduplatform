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
    private const val KEY_EXAM_HORIZONTAL = "exam_horizontal_mode"
    private const val KEY_ANIMATIONS = "animations_enabled"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var settingsPrefs: android.content.SharedPreferences
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()
    private lateinit var baseUrl: HttpUrl

    // ─── In-memory cache (fixes back/forth reload storms) ──────────────────────
    // Each entry stores (data, timestamp). Cache is valid for CACHE_TTL_MS.
    // Reduced to 10 sec for near-real-time updates — admin changes show up
    // in the app within 10 seconds without manual refresh.
    // For screens that need INSTANT data, use cachedFresh() which always fetches.
    private const val CACHE_TTL_MS = 10_000L // 10 seconds — near real-time
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

    /**
     * Always fetches fresh data from the network — bypasses the cache entirely.
     * Use this on screens where real-time data matters (Books, Exams, QBank,
     * Packages). Updates the cache as a side-effect so subsequent calls to
     * getCachedNow() return the fresh data.
     */
    suspend fun <T> cachedFresh(key: String, loader: suspend () -> T): T {
        val fresh = loader()
        cache[key] = CacheEntry(fresh, System.currentTimeMillis())
        return fresh
    }

    /**
     * Returns cached data IMMEDIATELY if available (even if stale), without
     * making any API call. Returns null if no cache exists.
     * Use this for instant screen loads — then call refreshXxx() in the
     * background to update the data silently.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCachedNow(key: String): T? {
        val hit = cache[key] as? CacheEntry<T>
        return hit?.data
    }

    /** Force-invalidate a cache key (call after a mutation or pull-to-refresh). */
    fun invalidateCache(key: String? = null) {
        if (key == null) cache.clear() else cache.remove(key)
    }

    /** Invalidate ALL cached data — use when the user navigates to a new
     *  major section or manually refreshes. Ensures the next API call
     *  fetches fresh data from the server. */
    fun invalidateAll() {
        cache.clear()
    }

    // ─── Cache keys (public, so screens can use getCachedNow) ──────────────────
    const val KEY_HOME_CARDS = "home_cards"
    const val KEY_BOOKS = "books"
    const val KEY_VIDEOS = "videos"
    const val KEY_AUDIO = "audio"
    const val KEY_QUESTION_BANK = "question_bank"
    fun keyTests(filter: String) = "tests_$filter"

    // Cached API helpers — used by screens so navigating back doesn't refetch.
    suspend fun getCachedHomeCards() = cached(KEY_HOME_CARDS) {
        AppState.api.getHomeCards().cards
    }
    suspend fun getCachedTests(filter: String) = cached(keyTests(filter)) {
        AppState.api.getTests(category = filter).tests
    }
    suspend fun getCachedBooks() = cached(KEY_BOOKS) {
        AppState.api.getBooks().books
    }
    suspend fun getCachedVideos() = cached(KEY_VIDEOS) {
        AppState.api.getVideoLessons().videos
    }
    suspend fun getCachedAudio() = cached(KEY_AUDIO) {
        AppState.api.getAudioLessons().lessons
    }
    suspend fun getCachedQuestionBank() = cached(KEY_QUESTION_BANK) {
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
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
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

    fun saveSessionToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
        // Set the cookie in the OkHttp cookie jar so API calls are authenticated
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

    /**
     * Exam layout mode — independent of text size.
     *  • false (default) = Vertical (question on top, options below) — best for phones
     *  • true            = Horizontal (question on left, options on right) — best for tablets
     *                      and landscape phones where you want to see both at once
     */
    fun isExamHorizontalMode(): Boolean = settingsPrefs.getBoolean(KEY_EXAM_HORIZONTAL, false)
    fun setExamHorizontalMode(value: Boolean) {
        settingsPrefs.edit().putBoolean(KEY_EXAM_HORIZONTAL, value).apply()
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
