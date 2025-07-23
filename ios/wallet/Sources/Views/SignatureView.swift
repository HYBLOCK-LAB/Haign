import SwiftUI

struct SignatureView: View {
    @StateObject private var viewModel = SignatureViewModel()

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                TextEditor(text: $viewModel.message)
                    .border(Color.secondary)
                    .frame(height: 150)

                Button("Sign") {
                    viewModel.signMessage()
                }

                if let signature = viewModel.signature {
                    ScrollView {
                        Text("Signature:\n\(signature)")
                            .font(.caption)
                            .padding()
                    }
                    .frame(maxHeight: 200)
                }
            }
            .padding()
            .navigationTitle("Signature")
        }
    }
}
