import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            MyWalletView()
                .tabItem {
                    Label("main.tab.wallet".localized(), systemImage: "wallet.pass")
                }
            SignatureView()
                .tabItem {
                    Label("main.tab.signature".localized(), systemImage: "pencil.tip")
                }
            ExchangeView()
                .tabItem {
                    Label("main.tab.exchange".localized(), systemImage: "arrow.2.circlepath")
                }
            ExploreView()
                .tabItem {
                    Label("main.tab.explore".localized(), systemImage: "globe")
                }
            SettingsView()
                .tabItem {
                    Label("main.tab.settings".localized(), systemImage: "gearshape")
                }
        }
    }
}
