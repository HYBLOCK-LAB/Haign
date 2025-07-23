//
//  WalletApp.swift
//  Wallet
//
//  Created by jiseop9083 on 7/13/25.
//

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
                    MainTabView()
                }
            }
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 5.0) {
                    withAnimation(.easeOut(duration: 0.5)) {
                        showOnboarding = false
                    }
                }
            }
        }
    }
}
