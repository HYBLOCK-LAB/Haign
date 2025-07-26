import SwiftUI

struct WalletCreateNavigateView: View {
    enum Step {
        case warning
        case showMnemonic
        case confirmMnemonic
    }

    @State private var step: Step = .warning
    @State private var mnemonic: String = ""

    let walletType: WalletType

    var body: some View {
        NavigationStack {
            EmptyView()
                .fullScreenCover(isPresented: .constant(step == .showMnemonic)) {
                    MnemonicView { generatedMnemonic in
                        mnemonic = generatedMnemonic
                        step = .confirmMnemonic
                    }
                }
                .fullScreenCover(isPresented: .constant(step == .confirmMnemonic)) {
                    MnemonicConfirmView(walletType: walletType, correctMnemonic: mnemonic)
                }
                .fullScreenCover(isPresented: .constant(step == .warning)) {
                    MnemonicWarningView {
                        step = .showMnemonic
                    }
                }
        }
    }
}
