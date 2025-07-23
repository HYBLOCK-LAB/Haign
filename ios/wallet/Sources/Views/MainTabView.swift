import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            MyWalletView()
                .tabItem {
                    Label("My Wallet", systemImage: "wallet.pass")
                }
            SignatureView()
                .tabItem {
                    Label("Signature", systemImage: "pencil.tip")
                }
            ExchangeView()
                .tabItem {
                    Label("Exchange", systemImage: "arrow.2.circlepath")
                }
            ExploreView()
                .tabItem {
                    Label("Explore", systemImage: "globe")
                }
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
        }
    }
}
