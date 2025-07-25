import SwiftUI

struct SettingsView: View {
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("settings.general".localized())) {
                    NavigationLink(destination: LanguageSettingsView()) {
                        Label("settings.option.language".localized(), systemImage: "globe")
                    }
                    NavigationLink(destination: CurrencySettingsView()) {
                        Label("settings.option.currency".localized(), systemImage: "dollarsign.circle")
                    }
                    Toggle("settings.option.notification".localized(), isOn: .constant(true))
                }
                Section(header: Text("settings.security".localized())) {
                    NavigationLink(destination: PinSettingView()) {
                        Label("settings.option.pin".localized(), systemImage: "lock")
                    }
                    Toggle("settings.option.face".localized(), isOn: .constant(true))
                }
            }
            .navigationTitle("main.tab.settings".localized())
        }
    }
}
