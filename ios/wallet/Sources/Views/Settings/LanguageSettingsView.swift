import SwiftUI

struct LanguageSettingsView: View {
    @ObservedObject private var localizationViewModel = LocalizationViewModel.shared

    let availableLanguages: [String: String] = [
        "en": "English",
        "ko": "한국어",
    ]

    var body: some View {
        Form {
            Section(header: Text("settings.language.select".localized())) {
                ForEach(availableLanguages.keys.sorted(), id: \.self) { code in
                    HStack {
                        Text(availableLanguages[code] ?? code)
                        Spacer()
                        if localizationViewModel.currentLanguage == code {
                            Image(systemName: "checkmark")
                                .foregroundColor(.blue)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        if localizationViewModel.currentLanguage != code {
                            localizationViewModel.currentLanguage = code
                            restartApp()
                        }
                    }
                }
            }
        }
        .navigationTitle(Text("settings.option.language".localized()))
    }

    private func restartApp() {
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap({ $0.windows })
            .first(where: { $0.isKeyWindow }) else { return }

        window.rootViewController = UIHostingController(rootView: NavigateView()
            .environmentObject(LocalizationViewModel.shared)
        )
        window.makeKeyAndVisible()
    }
}
