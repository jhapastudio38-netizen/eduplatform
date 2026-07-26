import SwiftUI

// MARK: - Main Tab View
struct MainTabView: View {
    @EnvironmentObject var session: SessionStore
    @State private var selectedTab = 0

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                VStack(spacing: 0) {
                    // Top bar
                    HStack {
                        Text("DreamKorea")
                            .font(.title3.bold())
                            .foregroundStyle(Theme.textDark)
                        Spacer()
                        NavigationLink(destination: ProfileView()) {
                            Image(systemName: "person.circle.fill")
                                .font(.title2)
                                .foregroundStyle(Theme.navyBlue)
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)

                    // Content
                    switch selectedTab {
                    case 0: HomeView()
                    case 1: ExamsView()
                    case 2: ToolsView()
                    case 3: BooksView()
                    default: HomeView()
                    }
                }

                // Bottom Navigation
                HStack(spacing: 0) {
                    BottomTabItem(icon: "house.fill", label: "Home", isActive: selectedTab == 0) {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { selectedTab = 0 }
                    }
                    BottomTabItem(icon: "doc.text.fill", label: "Exams", isActive: selectedTab == 1) {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { selectedTab = 1 }
                    }
                    BottomTabItem(icon: "wrench.adjustable.fill", label: "Tools", isActive: selectedTab == 2) {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { selectedTab = 2 }
                    }
                    BottomTabItem(icon: "book.fill", label: "Books", isActive: selectedTab == 3) {
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { selectedTab = 3 }
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 8)
                .background(.ultraThinMaterial)
                .clipShape(RoundedCorner(radius: 20))
                .shadow(color: .black.opacity(0.08), radius: 12, y: 4)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)
            }
            .background(Theme.bgGray.ignoresSafeArea())
        }
    }
}

// MARK: - Bottom Tab Item
struct BottomTabItem: View {
    let icon: String
    let label: String
    let isActive: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 22))
                Text(label)
                    .font(.system(size: 10))
            }
            .foregroundStyle(isActive ? Theme.navyBlue : Theme.textLight)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(isActive ? Theme.navyBlue.opacity(0.1) : .clear)
            )
            .scaleEffect(isActive ? 1.0 : 0.9)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isActive)
        }
    }
}

// MARK: - Home View
struct HomeView: View {
    @State private var cards: [HomeCard] = []
    @State private var stats: UserStats?
    @State private var loading = true

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if loading {
                    ProgressView().tint(Theme.navyBlue).padding(.top, 40)
                } else {
                    // Hero Card
                    HeroCard(stats: stats)

                    // Quick Access
                    HStack(spacing: 12) {
                        QuickAccessButton(icon: "graduationcap.fill", label: "UBT", color: Theme.navyBlue)
                        QuickAccessButton(icon: "doc.text.fill", label: "Demo", color: Theme.green)
                        QuickAccessButton(icon: "book.fill", label: "Books", color: Theme.orange)
                        QuickAccessButton(icon: "eye.fill", label: "Eye Test", color: Theme.purple)
                    }
                    .padding(.horizontal, 16)

                    // Today's Goal
                    if let stats = stats {
                        GoalCard(stats: stats)
                            .padding(.horizontal, 16)
                    }

                    // Cards from admin
                    let testCards = cards.filter { $0.section == "test" }
                    if !testCards.isEmpty {
                        SectionHeader(title: "Free Exams")
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            ForEach(testCards) { card in
                                CardItem(card: card)
                            }
                        }
                        .padding(.horizontal, 12)
                    }

                    let resourceCards = cards.filter { $0.section == "resources" }
                    if !resourceCards.isEmpty {
                        SectionHeader(title: "Tools & Resources")
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            ForEach(resourceCards) { card in
                                CardItem(card: card)
                            }
                        }
                        .padding(.horizontal, 12)
                    }
                }
            }
            .padding(.bottom, 100)
        }
        .task {
            await loadData()
        }
    }

    private func loadData() async {
        do {
            async let cardsTask = APIClient.shared.getHomeCards()
            async let statsTask = APIClient.shared.getStats()
            let (fetchedCards, fetchedStats) = try await (cardsTask, statsTask)
            await MainActor.run {
                self.cards = fetchedCards
                self.stats = fetchedStats
                self.loading = false
            }
        } catch {
            await MainActor.run { self.loading = false }
        }
    }
}

// MARK: - Hero Card
struct HeroCard: View {
    let stats: UserStats?

    var body: some View {
        ZStack(alignment: .leading) {
            // Gradient background
            LinearGradient(
                colors: [Color(hex: "8B2252").opacity(0.75), Theme.navyBlue.opacity(0.85)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 8) {
                Text("Hello, Student")
                    .font(.title2.bold())
                    .foregroundStyle(.white)
                Text("Let's learn Korean together")
                    .font(.subheadline)
                    .foregroundStyle(.white.opacity(0.8))

                HStack(spacing: 8) {
                    Badge(text: "\(stats?.totalExamsTaken ?? 0) exams")
                    Badge(text: "\(stats?.studyStreakDays ?? 0) day streak")
                }

                if let stats = stats {
                    ProgressView(value: stats.averageScore / 100)
                        .tint(Theme.pinkAccent)
                        .scaleEffect(y: 2)
                    Text("\(String(format: "%.0f", stats.averageScore))% average score")
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.7))
                }
            }
            .padding(20)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 160)
        .clipShape(RoundedCorner(radius: 20))
        .shadow(color: .black.opacity(0.15), radius: 6, y: 3)
        .padding(.horizontal, 16)
    }
}

struct Badge: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.caption)
            .foregroundStyle(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(.white.opacity(0.2))
            .clipShape(RoundedCorner(radius: 8))
    }
}

struct QuickAccessButton: View {
    let icon: String
    let label: String
    let color: Color
    @State private var pressed = false

    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundStyle(color)
                .frame(width: 48, height: 48)
                .background(color.opacity(0.15))
                .clipShape(RoundedCorner(radius: 14))
            Text(label)
                .font(.system(size: 11))
                .foregroundStyle(Theme.textDark)
        }
        .scaleEffect(pressed ? 0.9 : 1)
        .animation(.spring(response: 0.3, dampingFraction: 0.5), value: pressed)
        .onTapGesture {
            pressed = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { pressed = false }
        }
    }
}

struct GoalCard: View {
    let stats: UserStats
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Today's Goal").font(.headline).foregroundStyle(Theme.textDark)
                Spacer()
                Text("\(stats.totalQuestionsAnswered) answered").font(.caption).foregroundStyle(Theme.textMid)
            }
            ProgressView(value: min(Double(stats.totalQuestionsAnswered), 20) / 20)
                .tint(Theme.navyBlue)
            Text("\(stats.totalQuestionsAnswered) / 20 questions")
                .font(.caption)
                .foregroundStyle(Theme.textMid)
        }
        .padding(16)
        .background(.white)
        .clipShape(RoundedCorner(radius: 14))
        .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
    }
}

struct SectionHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(.headline)
            .foregroundStyle(Theme.textDark)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 8)
    }
}

struct CardItem: View {
    let card: HomeCard
    @State private var pressed = false

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Theme.bgGray
                if let url = toAbsoluteURL(card.imageUrl), !url.isEmpty {
                    AsyncImage(url: URL(string: url)) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Image(systemName: "photo").foregroundStyle(Theme.textLight)
                    }
                } else {
                    Image(systemName: "photo").font(.title).foregroundStyle(Theme.textLight)
                }
            }
            .frame(height: 100)
            .clipped()

            Text(card.title)
                .font(.caption.bold())
                .foregroundStyle(Theme.textDark)
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .padding(8)
                .frame(maxWidth: .infinity)
        }
        .background(.white)
        .clipShape(RoundedCorner(radius: 16))
        .shadow(color: .black.opacity(0.06), radius: 2, y: 1)
        .scaleEffect(pressed ? 0.96 : 1)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: pressed)
        .onTapGesture {
            pressed = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { pressed = false }
        }
    }
}

// MARK: - Exams View
struct ExamsView: View {
    @State private var tests: [TestItem] = []
    @State private var loading = true
    @State private var selectedCategory = "all"

    let categories = [
        ("all", "All"), ("exam", "UBT"), ("demo", "Demo"),
        ("batch", "Batch"), ("chapter", "Chapter"), ("question_bank", "Q Bank")
    ]

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Exams").font(.title.bold()).foregroundStyle(Theme.textDark)
                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 8)

            // Filter tabs
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(categories, id: \.0) { key, label in
                        Text(label)
                            .font(.subheadline)
                            .fontWeight(selectedCategory == key ? .semibold : .regular)
                            .foregroundStyle(selectedCategory == key ? .white : Theme.textMid)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(selectedCategory == key ? Theme.navyBlue : .clear)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule().stroke(Theme.divider, lineWidth: selectedCategory == key ? 0 : 1)
                            )
                            .onTapGesture {
                                withAnimation(.spring(response: 0.3)) { selectedCategory = key }
                                Task { await loadTests() }
                            }
                    }
                }
                .padding(.horizontal, 16)
            }

            if loading {
                Spacer()
                ProgressView().tint(Theme.navyBlue)
                Spacer()
            } else if tests.isEmpty {
                Spacer()
                VStack(spacing: 8) {
                    Image(systemName: "doc.text").font(.largeTitle).foregroundStyle(Theme.textLight)
                    Text("Nothing here yet").font(.headline).foregroundStyle(Theme.textDark)
                    Text("Your teacher will add content soon").font(.subheadline).foregroundStyle(Theme.textMid)
                }
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(tests) { test in
                            ExamCard(test: test)
                        }
                    }
                    .padding(16)
                    .padding(.bottom, 100)
                }
            }
        }
        .task { await loadTests() }
    }

    private func loadTests() async {
        loading = true
        do {
            tests = try await APIClient.shared.getTests(category: selectedCategory)
        } catch { }
        loading = false
    }
}

struct ExamCard: View {
    let test: TestItem
    @State private var pressed = false

    var body: some View {
        HStack(spacing: 0) {
            Rectangle()
                .fill(test.isExam ? Theme.pink : Theme.navyBlue)
                .frame(width: 5)

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text(test.title).font(.headline).foregroundStyle(Theme.textDark)
                    Spacer()
                    Text(test.isExam ? "EXAM" : "PRACTICE")
                        .font(.system(size: 9).bold())
                        .foregroundStyle(test.isExam ? Theme.pink : Theme.navyBlue)
                        .padding(.horizontal, 8).padding(.vertical, 3)
                        .background((test.isExam ? Theme.pink : Theme.navyBlue).opacity(0.15))
                        .clipShape(RoundedCorner(radius: 6))
                }
                if let desc = test.description, !desc.isEmpty {
                    Text(desc).font(.caption).foregroundStyle(Theme.textMid).lineLimit(2)
                }
                HStack(spacing: 8) {
                    Label("\(test.durationMin) min", systemImage: "clock").font(.caption).foregroundStyle(Theme.navyBlue)
                    Label("Pass \(test.passScore)%", systemImage: "checkmark.circle").font(.caption).foregroundStyle(Theme.green)
                    if test.questionCount > 0 {
                        Label("\(test.questionCount) Q", systemImage: "questionmark.circle").font(.caption).foregroundStyle(Theme.purple)
                    }
                }
            }
            .padding(16)
        }
        .background(.white)
        .clipShape(RoundedCorner(radius: 16))
        .shadow(color: .black.opacity(0.06), radius: 2, y: 1)
        .scaleEffect(pressed ? 0.97 : 1)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: pressed)
        .onTapGesture {
            pressed = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { pressed = false }
        }
    }
}

// MARK: - Tools View (placeholder for Grammar/AI/Vocabulary)
struct ToolsView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("Tools").font(.title.bold()).foregroundStyle(Theme.textDark)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)

            VStack(spacing: 12) {
                ToolCard(icon: "textformat", title: "Grammar", subtitle: "Learn Korean grammar rules", color: Theme.purple)
                ToolCard(icon: "brain.head.profile", title: "AI Assistant", subtitle: "Ask questions about Korean", color: Theme.navyBlue)
                ToolCard(icon: "text.book.closed", title: "Vocabulary", subtitle: "Build your word bank", color: Theme.orange)
                ToolCard(icon: "eye.fill", title: "Eye Vision Test", subtitle: "Test your eyesight", color: Theme.green)
                ToolCard(icon: "video.badge.plus", title: "Join Live", subtitle: "Enter a session code", color: Theme.pink)
            }
            .padding(16)
            .padding(.bottom, 100)
        }
    }
}

struct ToolCard: View {
    let icon: String
    let title: String
    let subtitle: String
    let color: Color
    @State private var pressed = false

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(color)
                .frame(width: 44, height: 44)
                .background(color.opacity(0.15))
                .clipShape(RoundedCorner(radius: 12))

            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.bold()).foregroundStyle(Theme.textDark)
                Text(subtitle).font(.caption).foregroundStyle(Theme.textMid)
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(Theme.textLight)
        }
        .padding(14)
        .background(.white)
        .clipShape(RoundedCorner(radius: 14))
        .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
        .scaleEffect(pressed ? 0.97 : 1)
        .animation(.spring(response: 0.3, dampingFraction: 0.6), value: pressed)
        .onTapGesture {
            pressed = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { pressed = false }
        }
    }
}

// MARK: - Books View
struct BooksView: View {
    @State private var books: [Book] = []
    @State private var loading = true

    var body: some View {
        VStack(spacing: 0) {
            Text("Books")
                .font(.title.bold())
                .foregroundStyle(Theme.textDark)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 16)
                .padding(.bottom, 8)

            if loading {
                Spacer()
                ProgressView().tint(Theme.navyBlue)
                Spacer()
            } else if books.isEmpty {
                Spacer()
                VStack(spacing: 8) {
                    Image(systemName: "book").font(.largeTitle).foregroundStyle(Theme.textLight)
                    Text("No books yet").font(.headline).foregroundStyle(Theme.textDark)
                }
                Spacer()
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(books) { book in
                            BookCard(book: book)
                        }
                    }
                    .padding(16)
                    .padding(.bottom, 100)
                }
            }
        }
        .task {
            do { books = try await APIClient.shared.getBooks() } catch { }
            loading = false
        }
    }
}

struct BookCard: View {
    let book: Book

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Theme.navyBlue
                Image(systemName: "book.fill").foregroundStyle(.white)
            }
            .frame(width: 54, height: 72)
            .clipShape(RoundedCorner(radius: 8))

            VStack(alignment: .leading, spacing: 4) {
                Text(book.title).font(.subheadline.bold()).foregroundStyle(Theme.textDark).lineLimit(2)
                if let author = book.author { Text("by \(author)").font(.caption).foregroundStyle(Theme.textMid) }
                HStack(spacing: 6) {
                    if let cat = book.category {
                        Text(cat).font(.system(size: 9).bold()).foregroundStyle(Theme.navyBlue)
                            .padding(.horizontal, 6).padding(.vertical, 2)
                            .background(Theme.navyBlue.opacity(0.1)).clipShape(RoundedCorner(radius: 4))
                    }
                    if let level = book.level {
                        Text(level).font(.system(size: 9).bold()).foregroundStyle(Theme.purple)
                            .padding(.horizontal, 6).padding(.vertical, 2)
                            .background(Theme.purple.opacity(0.1)).clipShape(RoundedCorner(radius: 4))
                    }
                }
            }
            Spacer()
        }
        .padding(12)
        .background(.white)
        .clipShape(RoundedCorner(radius: 14))
        .shadow(color: .black.opacity(0.05), radius: 2, y: 1)
    }
}

// MARK: - Profile View
struct ProfileView: View {
    @EnvironmentObject var session: SessionStore
    @State private var stats: UserStats?

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Profile header
                VStack(spacing: 8) {
                    Circle()
                        .fill(Theme.navyBlue)
                        .frame(width: 72, height: 72)
                        .overlay(Text(String(session.userName.prefix(2)).uppercased()).font(.title2.bold()).foregroundStyle(.white))
                    Text(session.userName).font(.title3.bold()).foregroundStyle(Theme.textDark)
                    Text("Student").font(.subheadline).foregroundStyle(Theme.textMid)
                }
                .padding(20)
                .background(.white)
                .clipShape(RoundedCorner(radius: 16))
                .shadow(color: .black.opacity(0.05), radius: 2, y: 1)

                // Stats
                VStack(alignment: .leading, spacing: 12) {
                    Text("Your Progress").font(.headline).foregroundStyle(Theme.textDark)
                    HStack {
                        StatItem(value: "\(stats?.totalExamsTaken ?? 0)", label: "Exams")
                        StatItem(value: String(format: "%.0f%%", stats?.averageScore ?? 0), label: "Avg")
                        StatItem(value: "\(stats?.studyStreakDays ?? 0)", label: "Streak")
                        StatItem(value: "\(stats?.badgesEarned ?? 0)", label: "Badges")
                    }
                }
                .padding(16)
                .background(.white)
                .clipShape(RoundedCorner(radius: 14))
                .shadow(color: .black.opacity(0.05), radius: 2, y: 1)

                // Logout
                Button(role: .destructive) {
                    APIClient.shared.logout()
                    session.clearSession()
                } label: {
                    HStack {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                        Text("Sign out")
                    }
                    .frame(maxWidth: .infinity).frame(height: 48)
                }
                .background(Theme.errorRed.opacity(0.1))
                .clipShape(RoundedCorner(radius: 12))
                .foregroundStyle(Theme.errorRed)
            }
            .padding(16)
        }
        .background(Theme.bgGray.ignoresSafeArea())
        .task {
            do { stats = try await APIClient.shared.getStats() } catch { }
        }
    }
}

struct StatItem: View {
    let value: String
    let label: String
    var body: some View {
        VStack(spacing: 2) {
            Text(value).font(.title3.bold()).foregroundStyle(Theme.navyBlue)
            Text(label).font(.caption).foregroundStyle(Theme.textMid)
        }
        .frame(maxWidth: .infinity)
    }
}
