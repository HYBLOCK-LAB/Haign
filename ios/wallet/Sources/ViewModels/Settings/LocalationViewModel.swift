import Foundation

class LocalizationViewModel: ObservableObject {
    static let shared = LocalizationViewModel()

    @Published var currentLanguage: String = UserDefaults.standard.string(forKey: "appLanguage") ?? Locale.current.language.languageCode?.identifier ?? "en" {
        didSet {
            UserDefaults.standard.set(currentLanguage, forKey: "appLanguage")
            Bundle.setLanguage(currentLanguage)
        }
    }
}
