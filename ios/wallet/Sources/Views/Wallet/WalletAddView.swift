import SwiftUI

struct WalletAddView: View {
    let walletType: WalletType
    @Environment(\.dismiss) var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                // 예시 입력 폼
                TextField("지갑 이름", text: .constant(""))
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.top)

                Spacer()
            }
            .padding()
            .navigationTitle("\(walletType.rawValue) 추가하기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(.blue)
                    }
                }
            }
        }
    }
}
