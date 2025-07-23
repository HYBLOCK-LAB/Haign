
import SwiftUI

struct MyWalletView: View {
    @StateObject private var nfcViewModel = NFCViewModel()
    @State private var isShowNFCWait = false

    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("My Wallet balance: 0")
                    .font(.title2)

                Button("Connect") {
                    isShowNFCWait = true
                }
                .font(.headline)
                .padding()
                .background(Color.accentColor)
                .foregroundColor(.white)
                .cornerRadius(8)
            }
            .navigationTitle("Card Wallet")
            .sheet(isPresented: $isShowNFCWait) {
                if let cmd = APDUCommand.verifyPIN("1234") {
                    NFCWaitingView(
                        isPresented: $isShowNFCWait,
                        viewModel: nfcViewModel,
                        command: cmd
                    )
                } else {
                    Text("Invalid PIN format")
                        .onAppear { isShowNFCWait = false }
                }
            }
        }
    }
}
