import SwiftUI
import WalletCore

func wrap(words: [String], maxWordsPerLine: Int = 4) -> [[String]] {
    var lines: [[String]] = []
    var currentLine: [String] = []

    for word in words {
        currentLine.append(word)
        if currentLine.count == maxWordsPerLine {
            lines.append(currentLine)
            currentLine = []
        }
    }

    if !currentLine.isEmpty {
        lines.append(currentLine)
    }

    return lines
}

struct MnemonicView: View {
    var onNext: (String) -> Void
    @Environment(\.dismiss) var dismiss

    @State private var mnemonic: String = ""
    @State private var currentLine: Int = 0

    private let lineLabels = ["A", "B", "C", "D", "E", "F"]

    var body: some View {
        NavigationStack {
            VStack {
                Text("wallet.add.description".localized())
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .padding(.top, 20)

                VStack {
                    mnemonicLinesView()
                }
                .padding(.top, 24)
                .padding(.bottom, 24)
                Spacer()

                HStack(spacing: 16) {
                    Button("utils.button.previous".localized()) {
                        if currentLine > 0 {
                            withAnimation {
                                currentLine -= 1
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(currentLine == 0 ? Color.gray.opacity(0.2) : Color.blue)
                    .foregroundColor(currentLine == 0 ? .gray : .white)
                    .cornerRadius(10)
                    .disabled(currentLine == 0)

                    Button("utils.button.next".localized()) {
                        if currentLine < mnemonicChunks.count - 1 {
                            withAnimation {
                                currentLine += 1
                            }
                        } else {
                            onNext(mnemonic)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .padding(.horizontal)
                .padding(.top, 24)
                .padding(.bottom, 24)
            }
            .navigationTitle("wallet.add.title".localized())
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(true)
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
            .onAppear {
                let result = MnemonicUtils.generateMnemonicAndSeed()
                mnemonic = result.mnemonic
            }
        }
    }

    private var mnemonicChunks: [[String]] {
        let words = mnemonic.components(separatedBy: " ")
        return stride(from: 0, to: words.count, by: 4).map {
            Array(words[$0 ..< min($0 + 4, words.count)])
        }
    }

    private func mnemonicLinesView() -> some View {
        VStack {
            ForEach(mnemonicChunks.indices, id: \.self) { index in
                VStack(alignment: .leading, spacing: 16) {
                    Text(lineLabels[index])
                        .font(.system(size: 20, weight: index == currentLine ? .bold : .semibold))
                        .frame(maxWidth: .infinity, alignment: .center)

                    HStack(spacing: 12) {
                        ForEach(mnemonicChunks[index], id: \.self) { word in
                            Text(index == currentLine ? word : "●")
                                .font(.system(size: 16, weight: .medium, design: .monospaced))
                                .minimumScaleFactor(0.5)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 6)
                                .frame(minWidth: 0, maxWidth: .infinity)
                                .background(Color.gray.opacity(index == currentLine ? 0.15 : 0.05))
                                .cornerRadius(6)
                        }
                    }
                    .padding(.horizontal)
                }
                .padding(.vertical, 6)
            }
        }
    }
}
