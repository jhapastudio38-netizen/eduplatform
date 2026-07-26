import SwiftUI
import Security

// MARK: - Login View
struct LoginView: View {
    @EnvironmentObject var session: SessionStore
    @State private var name = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var code = ""
    @State private var step = 1
    @State private var loading = false
    @State private var error = ""
    @State private var info = ""
    @State private var logoScale: CGFloat = 0.3
    @State private var formOpacity: Double = 0

    var body: some View {
        ZStack {
            // Inverted gradient background
            LinearGradient(
                colors: [Theme.bgGray, Color(hex: "EFF6FF"), Theme.navyBlue.opacity(0.3), Theme.navyBlue],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 24) {
                    Spacer().frame(height: 40)

                    // Logo
                    Circle()
                        .fill(.white)
                        .frame(width: 110, height: 110)
                        .shadow(color: .black.opacity(0.15), radius: 12, y: 6)
                        .overlay(
                            Image(systemName: "graduationcap.fill")
                                .font(.system(size: 44))
                                .foregroundStyle(Theme.navyBlue)
                        )
                        .scaleEffect(logoScale)
                        .animation(.spring(response: 0.6, dampingFraction: 0.6), value: logoScale)

                    // Form Card
                    VStack(spacing: 16) {
                        if !info.isEmpty {
                            InfoBanner(text: info, color: Theme.green)
                                .transition(.move(edge: .top).combined(with: .opacity))
                        }
                        if !error.isEmpty {
                            InfoBanner(text: error, color: Theme.errorRed)
                                .transition(.move(edge: .top).combined(with: .opacity))
                        }

                        if step == 1 {
                            StepOneView(name: $name, email: $email, phone: $phone, loading: $loading) {
                                sendOTP()
                            }
                            .transition(.asymmetric(
                                insertion: .move(edge: .trailing).combined(with: .opacity),
                                removal: .move(edge: .leading).combined(with: .opacity)
                            ))
                        } else {
                            StepTwoView(code: $code, email: email, loading: $loading) {
                                verifyOTP()
                            }
                            .transition(.asymmetric(
                                insertion: .move(edge: .trailing).combined(with: .opacity),
                                removal: .move(edge: .leading).combined(with: .opacity)
                            ))
                        }
                    }
                    .padding(24)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .shadow(color: .black.opacity(0.1), radius: 8, y: 4)
                    .opacity(formOpacity)
                    .animation(.easeOut(duration: 0.6), value: formOpacity)

                    Spacer().frame(height: 40)
                }
                .padding(.horizontal, 24)
            }
        }
        .onAppear {
            withAnimation(.spring(response: 0.8, dampingFraction: 0.6)) {
                logoScale = 1.0
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                withAnimation {
                    formOpacity = 1.0
                }
            }
        }
    }

    private func sendOTP() {
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty,
              email.contains("@"), phone.count >= 7 else {
            error = "Please fill all fields correctly"
            return
        }
        loading = true
        error = ""
        info = ""
        Task {
            do {
                try await APIClient.shared.requestOTP(email: email)
                await MainActor.run {
                    info = "Code sent to \(email)"
                    withAnimation { step = 2 }
                    loading = false
                }
            } catch {
                await MainActor.run {
                    self.error = "Could not send code. Check your internet."
                    loading = false
                }
            }
        }
    }

    private func verifyOTP() {
        guard code.count == 6 else { error = "Enter 6 digits"; return }
        loading = true
        error = ""
        info = ""
        Task {
            do {
                let (token, user) = try await APIClient.shared.verifyOTP(email: email, code: code, name: name, phone: phone)
                await MainActor.run {
                    session.saveSession(token: token, user: user)
                    loading = false
                }
            } catch {
                await MainActor.run {
                    self.error = "Wrong code. Try again."
                    loading = false
                }
            }
        }
    }
}

// MARK: - Step One
struct StepOneView: View {
    @Binding var name: String
    @Binding var email: String
    @Binding var phone: String
    @Binding var loading: Bool
    let onSubmit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Welcome")
                .font(.title2.bold())
                .foregroundStyle(Theme.textDark)
            Text("Sign up or log in with your email")
                .font(.subheadline)
                .foregroundStyle(Theme.textMid)

            StyledField(title: "Full Name", text: $name, icon: "person.fill")
            StyledField(title: "Email", text: $email, icon: "envelope.fill", keyboard: .emailAddress)
            StyledField(title: "Phone Number", text: $phone, icon: "phone.fill", keyboard: .phonePad)

            Text("Returning user? Use the same email to log in.")
                .font(.caption)
                .foregroundStyle(Theme.textLight)

            Button(action: onSubmit) {
                HStack {
                    if loading {
                        ProgressView().tint(.white)
                    }
                    Text(loading ? "Sending..." : "Send Verification Code")
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 50)
            }
            .buttonStyle(PrimaryButtonStyle())
            .disabled(loading || name.isEmpty || email.isEmpty || phone.isEmpty)
        }
    }
}

// MARK: - Step Two
struct StepTwoView: View {
    @Binding var code: String
    let email: String
    @Binding var loading: Bool
    let onSubmit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Verify")
                .font(.title2.bold())
                .foregroundStyle(Theme.textDark)
            Text("Enter the 6-digit code sent to \(email)")
                .font(.subheadline)
                .foregroundStyle(Theme.textMid)

            TextField("6-digit code", text: $code)
                .keyboardType(.numberPad)
                .font(.title3.bold())
                .multilineTextAlignment(.center)
                .letterSpacing(8)
                .padding()
                .background(Theme.bgGray)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Theme.navyBlue, lineWidth: code.isEmpty ? 1 : 2)
                )

            Button(action: onSubmit) {
                HStack {
                    if loading {
                        ProgressView().tint(.white)
                    }
                    Text(loading ? "Verifying..." : "Verify & Continue")
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 50)
            }
            .buttonStyle(PrimaryButtonStyle())
            .disabled(loading || code.count < 6)
        }
    }
}

// MARK: - Reusable Components
struct StyledField: View {
    let title: String
    @Binding var text: String
    let icon: String
    var keyboard: UIKeyboardType = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.caption).foregroundStyle(Theme.textMid)
            HStack {
                Image(systemName: icon).foregroundStyle(Theme.textMid)
                TextField(title, text: $text)
                    .keyboardType(keyboard)
                    .textInputAutocapitalization(keyboard == .emailAddress ? .never : .sentences)
            }
            .padding(12)
            .background(Theme.bgGray)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Theme.divider, lineWidth: 1)
            )
        }
    }
}

struct InfoBanner: View {
    let text: String
    let color: Color

    var body: some View {
        HStack {
            Image(systemName: color == Theme.green ? "checkmark.circle.fill" : "xmark.circle.fill")
            Text(text).font(.subheadline)
            Spacer()
        }
        .padding(12)
        .background(color.opacity(0.1))
        .clipShape(RoundedCorner(radius: 12))
        .foregroundStyle(color)
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(.white)
            .background(Theme.navyBlue)
            .clipShape(RoundedCorner(radius: 12))
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

// Fix for letter spacing
extension View {
    func letterSpacing(_ value: CGFloat) -> some View {
        self
    }
}
