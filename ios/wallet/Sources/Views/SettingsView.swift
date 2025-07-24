import SwiftUI

struct SettingsView: View {
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("General")) {
                    Toggle("Enable Notifications", isOn: .constant(true))
                }
            }
            .navigationTitle("main.tab.settings".localized())
        }
    }
}
