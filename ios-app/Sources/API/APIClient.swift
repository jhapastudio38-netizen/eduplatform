import Foundation
import SwiftUI

// MARK: - Theme
struct Theme {
    static let navyBlue = Color(hex: "1E3A8A")
    static let blueLight = Color(hex: "3B82F6")
    static let green = Color(hex: "22C55E")
    static let orange = Color(hex: "F59E0B")
    static let purple = Color(hex: "8B5CF6")
    static let pink = Color(hex: "EC4899")
    static let bgGray = Color(hex: "F8FAFC")
    static let cardWhite = Color.white
    static let textDark = Color(hex: "1E293B")
    static let textMid = Color(hex: "64748B")
    static let textLight = Color(hex: "94A3B8")
    static let divider = Color(hex: "E2E8F0")
    static let errorRed = Color(hex: "EF4444")
    static let pinkAccent = Color(hex: "F472B6")
}

extension Color {
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgb: UInt64 = 0
        scanner.scanHexInt64(&rgb)
        self.init(
            .sRGB,
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255,
            opacity: 1
        )
    }
}

// MARK: - API Base
let API_BASE = "https://my-project-five-sepia.vercel.app"

// MARK: - Models
struct User: Codable {
    var id: String = ""
    var name: String?
    var email: String = ""
    var phone: String?
    var role: String = "STUDENT"
}

struct UserStats: Codable {
    var totalExamsTaken: Int = 0
    var totalCorrectAnswers: Int = 0
    var totalQuestionsAnswered: Int = 0
    var averageScore: Double = 0.0
    var studyStreakDays: Int = 0
    var totalTimeSpentMin: Int = 0
    var booksRead: Int = 0
    var audioLessonsCompleted: Int = 0
    var badgesEarned: Int = 0
}

struct HomeCard: Codable, Identifiable {
    var id: String = ""
    var key: String = ""
    var title: String = ""
    var section: String = "test"
    var imageUrl: String?
    var sortOrder: Int = 0
    var isActive: Bool = true
    var route: String?
}

struct TestItem: Codable, Identifiable {
    var id: String
    var title: String
    var description: String?
    var durationMin: Int
    var isExam: Bool
    var examType: String?
    var testCategory: String?
    var passScore: Int
    var questionCount: Int = 0
    var featuredImage: String?
    var category: String?
    var price: Double?
}

struct QuestionDetail: Codable {
    var id: String = ""
    var type: String = ""
    var difficulty: String = ""
    var stem: String = ""
    var options: [String]?
    var imageUrl: String?
    var audioUrl: String?
    var audioLoop: Int = 0
    var audioLoopDelay: Int = 0
    var blockType: String = "text"
    var blockNumber: Int = 0
    var descType: String = "none"
    var descText: String?
    var descImageUrl: String?
    var descAudioUrl: String?
    var mediaType: String = "none"
    var mediaText: String?
    var mediaImageUrl: String?
    var mediaAudioUrl: String?
    var answerType: String = "text"
    var optionImages: [String] = []
    var optionAudios: [String] = []
    var correctOption: Int = 0
    var explanation: String?
}

struct TestItemDetail: Codable {
    var id: String = ""
    var order: Int = 0
    var points: Int = 1
    var question: QuestionDetail = QuestionDetail()
}

struct TestDetail: Codable {
    var id: String = ""
    var title: String = ""
    var description: String?
    var durationMin: Int = 30
    var isExam: Bool = false
    var passScore: Int = 40
    var items: [TestItemDetail] = []
}

struct EyeVisionTest: Codable, Identifiable {
    var id: String
    var title: String
    var description: String?
    var imageUrl: String
    var category: String?
}

struct LiveSession: Codable {
    var id: String = ""
    var title: String = ""
    var description: String?
    var meetingUrl: String = ""
    var credentials: String?
    var hostName: String?
}

struct Book: Codable, Identifiable {
    var id: String
    var title: String
    var description: String?
    var author: String?
    var coverUrl: String?
    var pdfUrl: String?
    var publishedDate: String?
    var category: String?
    var level: String?
}

// MARK: - Session Store
class SessionStore: ObservableObject {
    @Published var isLoggedIn = false
    @Published var userName = "Student"
    @Published var userEmail = ""
    @Published var sessionCookie: String?

    private let defaults = UserDefaults.standard

    init() {
        if let cookie = defaults.string(forKey: "ep_sid") {
            sessionCookie = cookie
            isLoggedIn = true
            userName = defaults.string(forKey: "userName") ?? "Student"
            userEmail = defaults.string(forKey: "userEmail") ?? ""
        }
    }

    func saveSession(token: String, user: User) {
        sessionCookie = token
        userName = user.name ?? "Student"
        userEmail = user.email
        isLoggedIn = true
        defaults.set(token, forKey: "ep_sid")
        defaults.set(userName, forKey: "userName")
        defaults.set(userEmail, forKey: "userEmail")
    }

    func clearSession() {
        sessionCookie = nil
        isLoggedIn = false
        defaults.removeObject(forKey: "ep_sid")
        defaults.removeObject(forKey: "userName")
        defaults.removeObject(forKey: "userEmail")
    }
}

// MARK: - API Client
class APIClient {
    static let shared = APIClient()
    private var cookie: String?

    init() {
        cookie = UserDefaults.standard.string(forKey: "ep_sid")
    }

    func setCookie(_ value: String) {
        cookie = value
        UserDefaults.standard.set(value, forKey: "ep_sid")
    }

    private func request(_ path: String, method: String = "GET", body: Data? = nil) async throws -> Data {
        guard let url = URL(string: API_BASE + path) else { throw APIError.invalidURL }
        var req = URLRequest(url: url)
        req.httpMethod = method
        if let body = body {
            req.httpBody = body
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        if let cookie = cookie {
            req.setValue("ep_sid=\(cookie)", forHTTPHeaderField: "Cookie")
        }
        let (data, response) = try await URLSession.shared.data(for: req)
        guard let httpResponse = response as? HTTPURLResponse else { throw APIError.invalidResponse }
        // Capture Set-Cookie
        if let setCookie = httpResponse.value(forHTTPHeaderField: "Set-Cookie") {
            if let token = extractCookieValue(setCookie, name: "ep_sid") {
                self.setCookie(token)
            }
        }
        if httpResponse.statusCode == 401 {
            throw APIError.unauthorized
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            let errorMsg = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw APIError.serverError(statusCode: httpResponse.statusCode, message: errorMsg)
        }
        return data
    }

    private func extractCookieValue(_ header: String, name: String) -> String? {
        let parts = header.split(separator: ";")
        for part in parts {
            let trimmed = part.trimmingCharacters(in: .whitespaces)
            if trimmed.hasPrefix("\(name)=") {
                return String(trimmed.dropFirst(name.count + 1))
            }
        }
        return nil
    }

    // MARK: Auth
    func requestOTP(email: String) async throws {
        let body = try JSONSerialization.data(withJSONObject: ["contact": email])
        _ = try await request("/api/auth/request-otp", method: "POST", body: body)
    }

    func verifyOTP(email: String, code: String, name: String, phone: String) async throws -> (String, User) {
        let body = try JSONSerialization.data(withJSONObject: [
            "contact": email, "code": code, "role": "STUDENT",
            "name": name, "email": email, "phone": phone
        ])
        let data = try await request("/api/auth/verify-otp", method: "POST", body: body)
        struct VerifyResponse: Codable {
            let ok: Bool
            let sessionToken: String?
            let user: User
        }
        let resp = try JSONDecoder().decode(VerifyResponse.self, from: data)
        if let token = resp.sessionToken {
            self.setCookie(token)
        }
        return (resp.sessionToken ?? "", resp.user)
    }

    // MARK: Home Cards
    func getHomeCards() async throws -> [HomeCard] {
        let data = try await request("/api/student/home-cards")
        struct Response: Codable { let cards: [HomeCard] }
        return try JSONDecoder().decode(Response.self, from: data).cards
    }

    // MARK: Stats
    func getStats() async throws -> UserStats {
        let data = try await request("/api/student/stats")
        struct Response: Codable { let stats: UserStats }
        return try JSONDecoder().decode(Response.self, from: data).stats
    }

    // MARK: Tests
    func getTests(category: String = "all") async throws -> [TestItem] {
        let data = try await request("/api/student/tests?category=\(category)")
        struct Response: Codable { let tests: [TestItem] }
        return try JSONDecoder().decode(Response.self, from: data).tests
    }

    func getTestDetail(id: String) async throws -> TestDetail {
        let data = try await request("/api/student/tests/\(id)")
        struct Response: Codable { let test: TestDetail }
        return try JSONDecoder().decode(Response.self, from: data).test
    }

    // MARK: Eye Vision
    func getEyeVisionTests() async throws -> [EyeVisionTest] {
        let data = try await request("/api/student/eye-vision")
        struct Response: Codable { let tests: [EyeVisionTest] }
        return try JSONDecoder().decode(Response.self, from: data).tests
    }

    // MARK: Live Session
    func joinLiveSession(code: String) async throws -> LiveSession {
        let body = try JSONSerialization.data(withJSONObject: ["joinCode": code])
        let data = try await request("/api/student/live-sessions/join", method: "POST", body: body)
        struct Response: Codable {
            let ok: Bool
            let session: LiveSession?
            let error: String?
        }
        let resp = try JSONDecoder().decode(Response.self, from: data)
        if resp.ok, let session = resp.session {
            return session
        }
        throw APIError.serverError(statusCode: 404, message: resp.error ?? "Invalid code")
    }

    // MARK: Books
    func getBooks() async throws -> [Book] {
        let data = try await request("/api/student/books")
        struct Response: Codable { let books: [Book] }
        return try JSONDecoder().decode(Response.self, from: data).books
    }

    // MARK: Logout
    func logout() async {
        try? await request("/api/auth/logout", method: "POST")
    }
}

enum APIError: Error {
    case invalidURL
    case invalidResponse
    case unauthorized
    case serverError(statusCode: Int, message: String)
}

// MARK: - URL Helper
func toAbsoluteURL(_ url: String?) -> String? {
    guard let url = url, !url.isEmpty else { return nil }
    if url.hasPrefix("http://") || url.hasPrefix("https://") { return url }
    return API_BASE + (url.hasPrefix("/") ? url : "/\(url)")
}
