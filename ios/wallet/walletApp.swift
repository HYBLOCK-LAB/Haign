import SwiftUI

@main
struct WalletApp: App {
    @State private var showOnboarding: Bool = true

    var body: some Scene {
        WindowGroup {
            Group {
                if showOnboarding {
                    OnboardingView()
                } else {
                    NavigateView()
                }
            }
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                    withAnimation(.easeOut(duration: 0.5)) {
                        showOnboarding = false
                    }
                }
            }
        }
    }
}
