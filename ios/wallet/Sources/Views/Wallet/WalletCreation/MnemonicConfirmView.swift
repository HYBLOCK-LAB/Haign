import SwiftUI

struct MnemonicConfirmView: View {
    let walletType: WalletType
    let correctMnemonic: String

    @Environment(\.dismiss) private var dismiss

    @State private var userInput: String = ""
    @State private var showError: Bool = false

    var body: some View {
        VStack(spacing: 24) {
            Text("니모닉을 올바른 순서로 입력하세요")
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding(.top, 24)

            TextEditor(text: $userInput)
                .frame(height: 140)
                .padding()
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.4)))
                .autocapitalization(.none)
                .disableAutocorrection(true)

            if showError {
                Text("니모닉이 일치하지 않습니다. 다시 확인해주세요.")
                    .foregroundColor(.red)
                    .font(.footnote)
            }

            Spacer()

            Button("확인") {
                let normalizedInput = userInput
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                    .components(separatedBy: .whitespaces)
                    .filter { !$0.isEmpty }
                    .joined(separator: " ")

                if normalizedInput == correctMnemonic {
                    // MARK: TODO go to next step (send seeds to card wallet)

                    print("정상 입력")
                    dismiss()
                } else {
                    showError = true
                }
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.blue)
            .foregroundColor(.white)
            .cornerRadius(10)
            .padding(.horizontal)
        }
        .padding()
        .navigationTitle("니모닉 확인")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .medium))
                        .foregroundColor(.blue)
                }
            }
        }
    }
}
