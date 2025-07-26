import SwiftUI

struct MnemonicWarningView: View {
    var onConfirm: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var hasChecked = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 12) {
                Spacer()

                Text("wallet.add.warning.title".localized())
                    .font(.title2)
                    .bold()

                Text("wallet.add.warning.description".localized())
                    .multilineTextAlignment(.center)
                    .lineSpacing(8)
                    .padding()
                    .padding(.top, 12)

                Spacer()

                Toggle(isOn: $hasChecked) {
                    Text("utils.agree".localized())
                        .font(.subheadline)
                }
                .toggleStyle(iOSCheckboxToggleStyle())
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 40)
                .padding(.leading, 4)

                Button(action: {
                    onConfirm()
                }) {
                    Text("wallet.add.warning.confirm".localized())
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(hasChecked ? Color.blue : Color.gray.opacity(0.5))
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
                .disabled(!hasChecked)
            }
            .padding()
            .navigationTitle("utils.important".localized())
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.gray)
                    }
                }
            }
        }
    }
}
