import SwiftUI

struct NFCWaitingView: View {
    @Binding var isPresented: Bool
    @ObservedObject var viewModel: NFCViewModel
    let command: APDUCommand

    var body: some View {
        ZStack {
            Color(.systemBackground)
                .edgesIgnoringSafeArea(.all)

            VStack {
                Spacer()

                Text("Waiting for NFC Tag")
                    .font(.headline)
                    .padding()

                ProgressView("")
                    .progressViewStyle(CircularProgressViewStyle(tint: .accentColor))
                    .scaleEffect(1.5)
                    .padding()

                Spacer()

                Button("Cancel") {
                    isPresented = false
                }
                .font(.body)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Color.secondary.opacity(0.2))
                .cornerRadius(8)
                .padding(.bottom, 40)
            }
        }
        .onAppear {
            viewModel.startScanning(command: command)
        }
        .onDisappear {
            viewModel.stopScanning()
        }
    }
}
