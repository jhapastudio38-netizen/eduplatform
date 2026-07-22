package app.dreamkorea.smartclass.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.dreamkorea.smartclass.api.DreamKoreaApi
import app.dreamkorea.smartclass.api.User
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("session")

object AppState {
    private val SESSION_TOKEN = stringPreferencesKey("token")
    private val USER_NAME = stringPreferencesKey("name")
    private val USER_EMAIL = stringPreferencesKey("email")
    private val USER_ROLE = stringPreferencesKey("role")

    // Backend URL — the live AWS server
    private const val BASE_URL = "http://eduplatform-alb-606377009.ap-south-1.elb.amazonaws.com/"

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val api: DreamKoreaApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val pref = appContext.dataStore.data
                var token = ""
                // Synchronous read from DataStore
                try {
                    val prefs = kotlinx.coroutines.runBlocking { pref }
                    token = prefs[SESSION_TOKEN] ?: ""
                } catch (_: Exception) {}
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

    suspend fun saveSession(token: String, user: User) {
        appContext.dataStore.edit { prefs ->
            prefs[SESSION_TOKEN] = token
            prefs[USER_NAME] = user.name ?: "Student"
            prefs[USER_EMAIL] = user.email
            prefs[USER_ROLE] = user.role
        }
    }

    suspend fun clearSession() {
        appContext.dataStore.edit { it.clear() }
    }

    fun tokenFlow(): Flow<String?> = appContext.dataStore.data.map { it[SESSION_TOKEN] }
    fun nameFlow(): Flow<String> = appContext.dataStore.data.map { it[USER_NAME] ?: "" }
    fun emailFlow(): Flow<String> = appContext.dataStore.data.map { it[USER_EMAIL] ?: "" }
    fun roleFlow(): Flow<String> = appContext.dataStore.data.map { it[USER_ROLE] ?: "STUDENT" }
}
