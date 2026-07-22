package app.dreamkorea.smartclass.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

// ─── Request/Response models ──────────────────────────────────────────────────

data class OtpRequest(val contact: String)
data class OtpResponse(val ok: Boolean, val channel: String, val devCode: String?)

data class VerifyRequest(
    val contact: String,
    val code: String,
    val role: String = "STUDENT",
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null
)
data class User(
    val id: String,
    val name: String?,
    val email: String,
    val phone: String?,
    val role: String
)
data class VerifyResponse(val ok: Boolean, val user: User)
data class MeResponse(val user: User?)

data class HomeStats(
    val lessonsCompleted: Int = 0,
    val testsTaken: Int = 0,
    val qaAsked: Int = 0,
    val streak: Int = 0
)
data class HomeResponse(val lessonsCompleted: Int = 0, val testsTaken: Int = 0, val qaAsked: Int = 0, val streak: Int = 0)

data class UserStats(
    val totalExamsTaken: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val averageScore: Double = 0.0,
    val studyStreakDays: Int = 0,
    val totalTimeSpentMin: Int = 0,
    val booksRead: Int = 0,
    val audioLessonsCompleted: Int = 0,
    val badgesEarned: Int = 0
)
data class StatsResponse(val stats: UserStats = UserStats())

data class Subject(val id: String, val name: String, val slug: String, val description: String?)
data class SubjectsResponse(val subjects: List<Subject>)

data class Chapter(val id: String, val title: String, val description: String?, val order: Int)
data class ChaptersResponse(val chapters: List<Chapter>)

data class Lesson(val id: String, val title: String, val type: String, val durationMin: Int, val order: Int)
data class LessonsResponse(val lessons: List<Lesson>)

data class TestItem(
    val id: String,
    val title: String,
    val description: String?,
    val durationMin: Int,
    val isExam: Boolean,
    val passScore: Int,
    val questionCount: Int = 0
)
data class TestsResponse(val tests: List<TestItem>)

data class Book(
    val id: String,
    val title: String,
    val slug: String,
    val description: String?,
    val author: String?,
    val coverUrl: String?,
    val pdfUrl: String?,
    val pageCount: Int?,
    val category: String?,
    val level: String?,
    val downloads: Int = 0
)
data class BooksResponse(val books: List<Book>)

data class AudioLesson(
    val id: String,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val durationSec: Int,
    val level: String?,
    val category: String?,
    val plays: Int = 0
)
data class AudioResponse(val lessons: List<AudioLesson>)

data class VideoLesson(
    val id: String,
    val title: String,
    val description: String?,
    val youtubeId: String,
    val thumbnailUrl: String?,
    val durationMin: Int,
    val level: String?,
    val category: String?,
    val views: Int = 0
)
data class VideosResponse(val videos: List<VideoLesson>)

data class QAQuestion(
    val id: String,
    val title: String,
    val body: String,
    val answersCount: Int = 0,
    val createdAt: String
)
data class QAResponse(val questions: List<QAQuestion>)

// ─── Retrofit API interface ───────────────────────────────────────────────────

interface DreamKoreaApi {
    @POST("api/auth/request-otp")
    suspend fun requestOtp(@Body body: OtpRequest): OtpResponse

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyRequest): VerifyResponse

    @GET("api/auth/me")
    suspend fun getMe(): MeResponse

    @GET("api/student/stats")
    suspend fun getStats(): StatsResponse

    @POST("api/auth/logout")
    suspend fun logout(): retrofit2.Response<Unit>

    @GET("api/student/subjects")
    suspend fun getSubjects(): SubjectsResponse

    @GET("api/student/subjects/{id}/chapters")
    suspend fun getChapters(@Path("id") id: String): ChaptersResponse

    @GET("api/student/chapters/{id}/lessons")
    suspend fun getLessons(@Path("id") id: String): LessonsResponse

    @GET("api/student/tests")
    suspend fun getTests(): TestsResponse

    @GET("api/student/books")
    suspend fun getBooks(): BooksResponse

    @GET("api/student/audio-lessons")
    suspend fun getAudioLessons(): AudioResponse

    @GET("api/student/video-lessons")
    suspend fun getVideoLessons(): VideosResponse

    @GET("api/student/qa")
    suspend fun getQA(): QAResponse
}
