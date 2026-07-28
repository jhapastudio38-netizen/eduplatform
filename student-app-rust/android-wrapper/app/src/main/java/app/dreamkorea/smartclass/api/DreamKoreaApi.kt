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
    val phone: String? = null,
    val password: String? = null
)
data class User(
    val id: String,
    val name: String?,
    val email: String,
    val phone: String?,
    val role: String
)
data class VerifyResponse(val ok: Boolean, val sessionToken: String? = null, val user: User)
data class CredentialsResponse(val ok: Boolean = false, val user: User = User("", null, "", null, "STUDENT"), val error: String? = null)
data class SimpleResponse(val ok: Boolean = false, val error: String? = null)
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

data class HomeCard(
    val id: String = "",
    val key: String = "",
    val title: String = "",
    val section: String = "test",
    val imageUrl: String? = null,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val route: String? = null
)
data class HomeCardsResponse(val cards: List<HomeCard> = emptyList(), val sections: Map<String, List<HomeCard>> = emptyMap())

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
    val questionCount: Int = 0,
    val examType: String = "REGULAR",
    val isActive: Boolean = true
)
data class TestsResponse(val tests: List<TestItem>)

// ─── Exam taking (full test detail with questions) ────────────────────────────
data class QuestionDetail(
    val id: String,
    val type: String,
    val difficulty: String,
    val stem: String,
    val title: String? = null, // optional per-question title shown at top of question
    val isFree: Boolean = false, // free questions show at top of QBank/Batch
    val options: List<String>?,
    // Legacy fields
    val imageUrl: String?,
    val audioUrl: String?,
    val audioLoop: Int = 0,
    val audioLoopDelay: Int = 0,
    // New block-based fields
    val blockType: String = "text",
    val blockNumber: Int = 0,
    val descType: String = "none",
    val descText: String? = null,
    val descImageUrl: String? = null,
    val descAudioUrl: String? = null,
    val mediaType: String = "none",
    val mediaText: String? = null,
    val mediaImageUrl: String? = null,
    val mediaAudioUrl: String? = null,
    val answerType: String = "text",
    val optionImages: List<String> = emptyList(),
    val optionAudios: List<String> = emptyList(),
    val optionBlanks: List<String> = emptyList(), // word to underline in each option
    val correctOption: Int = 0,
    val explanation: String? = null
)
data class TestItemDetail(
    val id: String,
    val order: Int,
    val points: Int,
    val question: QuestionDetail
)
data class TestDetail(
    val id: String,
    val title: String,
    val description: String?,
    val durationMin: Int,
    val isExam: Boolean,
    val passScore: Int,
    // Block flags + counts — exposed so the pre-exam info screen can show
    // "X text + Y audio questions" and the app can lay out the block grid.
    val textBlockCount: Int = 0,
    val audioBlockCount: Int = 0,
    val textBlockEnabled: Boolean = true,
    val audioBlockEnabled: Boolean = true,
    val items: List<TestItemDetail>
)
data class TestDetailResponse(val test: TestDetail)

// ─── Exam submission + review ─────────────────────────────────────────────────
data class SubmitRequest(val answers: Map<String, Any>)
data class ReviewItem(
    val questionId: String,
    val stem: String,
    val title: String? = null, // optional per-question title
    val type: String,
    val options: List<String>?,
    val optionImages: List<String> = emptyList(),
    val optionAudios: List<String> = emptyList(),
    val imageUrl: String?,
    val audioUrl: String?,
    val audioLoop: Int = 0,
    val audioLoopDelay: Int = 0,
    val userAnswer: Any?, // String or List<String>
    val correctAnswer: Any?, // String or List<String>
    val explanation: String?,
    val isCorrect: Boolean
)
data class EyeVisionRecommendation(
    val show: Boolean = false,
    val count: Int = 0,
    val reason: String = ""
)

data class SubmitResponse(
    val score: Int,
    val maxScore: Int,
    val graded: Boolean,
    val submissionId: String,
    val review: List<ReviewItem> = emptyList(),
    // Eye vision auto-trigger — server recommends eye vision tests based
    // on the student's mistake rate. The app reads this after submit.
    val eyeVision: EyeVisionRecommendation = EyeVisionRecommendation(),
    // Marks the exam as completed — used to show "Completed" badge on cards.
    val completed: Boolean = false
)

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

    @POST("api/auth/credentials")
    suspend fun loginCredentials(@Body body: Map<String, String>): CredentialsResponse

    // Student signup with email + password (no OTP needed)
    @POST("api/auth/signup")
    suspend fun signup(@Body body: Map<String, String>): CredentialsResponse

    // Forgot password — request a 6-digit reset code via email
    @POST("api/auth/request-reset")
    suspend fun requestReset(@Body body: Map<String, String>): SimpleResponse

    // Forgot password — verify the reset code + set a new password
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: Map<String, String>): SimpleResponse

    @POST("api/auth/set-password")
    suspend fun setPassword(@Body body: Map<String, String>): SimpleResponse

    @GET("api/auth/me")
    suspend fun getMe(): MeResponse

    @GET("api/student/stats")
    suspend fun getStats(): StatsResponse

    @GET("api/student/home-cards")
    suspend fun getHomeCards(): HomeCardsResponse

    @POST("api/auth/logout")
    suspend fun logout(): retrofit2.Response<Unit>

    @GET("api/student/subjects")
    suspend fun getSubjects(): SubjectsResponse

    @GET("api/student/subjects/{id}/chapters")
    suspend fun getChapters(@Path("id") id: String): ChaptersResponse

    @GET("api/student/chapters/{id}/lessons")
    suspend fun getLessons(@Path("id") id: String): LessonsResponse

    @GET("api/student/tests")
    suspend fun getTests(@Query("category") category: String = "all", @Query("filter") filter: String? = null): TestsResponse

    @GET("api/student/tests/{id}")
    suspend fun getTestDetail(@Path("id") id: String): TestDetailResponse

    @POST("api/student/tests/{id}/submit")
    suspend fun submitTest(@Path("id") id: String, @Body body: SubmitRequest): SubmitResponse

    @GET("api/student/books")
    suspend fun getBooks(): BooksResponse

    @GET("api/student/audio-lessons")
    suspend fun getAudioLessons(): AudioResponse

    @GET("api/student/video-lessons")
    suspend fun getVideoLessons(): VideosResponse

    @GET("api/student/qa")
    suspend fun getQA(): QAResponse

    @POST("api/student/live-rooms/join")
    suspend fun joinLiveRoom(@Body body: Map<String, String>): LiveRoomJoinWrapper

    @GET("api/student/question-bank")
    suspend fun getQuestionBank(): QuestionBankResponse

    @GET("api/student/notifications")
    suspend fun getNotifications(@Query("since") since: String? = null): NotificationsResponse

    @POST("api/student/live-sessions/join")
    suspend fun joinLiveSession(@Body body: Map<String, String>): LiveSessionJoinResponse

    @GET("api/student/eye-vision")
    suspend fun getEyeVisionTests(@Query("adaptive") adaptive: String? = null): EyeVisionResponse

    @POST("api/student/eye-vision/{testId}/check")
    @Headers("Content-Type: application/json")
    suspend fun checkEyeVisionAnswer(
        @Path("testId") testId: String,
        @Body body: Map<String, String>
    ): EyeVisionCheckResponse

    @GET("api/student/bundles")
    suspend fun getStudentBundles(@Query("kind") kind: String? = null): BundlesResponse

    @GET("api/student/qbank-combined")
    suspend fun getQBankCombined(): TestDetailResponse

    @GET("api/student/bundles/{bundleId}/combined")
    suspend fun getBundleCombined(@Path("bundleId") bundleId: String): TestDetailResponse

    @GET("api/student/completed-tests")
    suspend fun getCompletedTests(): CompletedTestsResponse

    @GET("api/student/tests/{id}/completion-status")
    suspend fun getCompletionStatus(@Path("id") id: String): CompletionStatusResponse

    @GET("api/student/subscription")
    suspend fun getSubscriptionStatus(): SubscriptionStatusResponse
}

// ─── Subscription ─────────────────────────────────────────────────────────────
data class SubscriptionStatusResponse(
    val isSubscribed: Boolean = false,
    val subscriptionType: String? = null,
    val subscribedUntil: String? = null,
)

// ─── Completion Status ────────────────────────────────────────────────────────
data class CompletionStatusResponse(
    val completed: Boolean = false,
    val canRetake: Boolean = true,
    val isSubscribed: Boolean = false,
    val submittedAt: String? = null,
    val score: Int? = null,
    val maxScore: Int? = null,
)

// ─── Completed Tests ──────────────────────────────────────────────────────────
// Maps testId (including combined IDs like "qbank-combined" or "bundle-{id}")
// to submission info, so the app can show a "Completed" badge.
data class CompletedTestInfo(
    val submittedAt: String = "",
    val score: Int? = null,
    val maxScore: Int? = null
)
data class CompletedTestsResponse(
    val completed: Map<String, CompletedTestInfo> = emptyMap(),
    val total: Int = 0
)

// ─── Notifications ────────────────────────────────────────────────────────────
data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val category: String = "general",
    val createdAt: String
)
data class NotificationsResponse(val notifications: List<AppNotification> = emptyList())

// ─── Live Session Join ───────────────────────────────────────────────────────
data class LiveSessionData(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val meetingUrl: String = "",
    val credentials: String? = null,
    val hostName: String? = null
)
data class LiveSessionJoinResponse(
    val ok: Boolean = false,
    val session: LiveSessionData? = null,
    val error: String? = null
)

// ─── Eye Vision ──────────────────────────────────────────────────────────────
data class EyeVisionTestItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String,
    val category: String? = null,
    val level: Int = 1
)
data class EyeVisionStats(
    val totalAttempts: Int = 0,
    val correctAttempts: Int = 0,
    val accuracy: Int = 0,
    val consecutiveCorrect: Int = 0
)
data class EyeVisionResponse(
    val tests: List<EyeVisionTestItem> = emptyList(),
    val level: Int = 1,
    val recommendedLevel: Int = 1,
    val stats: EyeVisionStats = EyeVisionStats()
)
data class EyeVisionCheckResponse(
    val correct: Boolean = false,
    val correctAnswer: String = "",
    val level: Int = 1,
    val nextLevel: Int = 1,
    val leveledUp: Boolean = false,
    val leveledDown: Boolean = false,
    val consecutiveCorrect: Int = 0,
    val consecutiveWrong: Int = 0,
    val stats: EyeVisionStats = EyeVisionStats()
)

// ─── Question Bank ────────────────────────────────────────────────────────────
data class QuestionBankQuestion(
    val id: String,
    val type: String,
    val difficulty: String,
    val stem: String,
    val options: List<String>? = null,
    val correctAnswer: Any? = null, // String or List<String>
    val explanation: String? = null,
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val audioLoop: Int = 0,
    val audioLoopDelay: Int = 0,
    val category: String = "General"
)
data class QuestionBankResponse(
    val questions: List<QuestionBankQuestion> = emptyList(),
    val categories: List<String> = emptyList(),
    val total: Int = 0
)

data class LiveRoomJoinWrapper(val room: LiveRoomData? = null, val error: String? = null)
data class LiveRoomData(
    val id: String = "",
    val roomCode: String = "",
    val title: String = "",
    val description: String? = null,
    val hostId: String = "",
    val isLive: Boolean = false,
    val audioOnly: Boolean = true,
    val maxStudents: Int = 50
)
data class LiveRoomJoinResponse(
    val ok: Boolean = false,
    val title: String = "",
    val description: String? = null,
    val hostName: String? = null,
    val attendeeCount: Int = 0,
    val error: String? = null
)

// ─── Question Bank / Batch / Exam / Chapter packages ─────────────────────────
// A bundle is a curated collection of tests the student can browse as a
// single package. Each bundle has a kind (qbank, batch, exam, chapter) and
// a list of tests inside it.
data class BundleTestSummary(
    val id: String,
    val title: String,
    val testCategory: String? = null,
    val examType: String = "REGULAR",
    val durationMin: Int = 0,
    val passScore: Int = 40,
    val featuredImage: String? = null
)
data class BundleItem(
    val sortOrder: Int = 0,
    val test: BundleTestSummary
)
data class BundleSummary(
    val id: String,
    val title: String,
    val slug: String,
    val description: String? = null,
    val kind: String = "qbank",
    val coverUrl: String? = null,
    val price: Int = 0,
    val createdAt: String = "",
    val items: List<BundleItem> = emptyList()
)
data class BundlesResponse(val bundles: List<BundleSummary> = emptyList())
