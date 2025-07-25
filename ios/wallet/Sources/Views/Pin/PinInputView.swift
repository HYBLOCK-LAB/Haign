import SwiftUI

struct PinInputView: View {
    @ObservedObject var viewModel: PinViewModel
    let maxDigits: Int
    let onComplete: () -> Void

    var body: some View {
        VStack(alignment: .center) {
            Spacer()

            VStack(alignment: .leading, spacing: 8) {
                Text(viewModel.attributedInstruction)
                    .font(.body)
                    .foregroundColor(.primary)
            }
            .padding(.top)
            .padding(.leading, 20)
            .frame(maxWidth: .infinity, alignment: .leading)

            Spacer()

            VStack(alignment: .center, spacing: 20) {
                if viewModel.pin.isEmpty {
                    Text("pin.instructions.enter_pin_range".localized())
                        .font(.body)
                        .foregroundColor(.secondary)
                }

                HStack(spacing: 15) {
                    ForEach(0 ..< viewModel.pin.count, id: \.self) { _ in
                        Circle()
                            .fill(Color.primary)
                            .frame(width: 20, height: 20)
                    }
                }
            }
            .padding(.top)

            Spacer()

            // 키패드 + 완료 버튼
            VStack(spacing: 20) {
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: 20), count: 3),
                    spacing: 20
                ) {
                    ForEach((1 ... 9).map(String.init), id: \.self) { num in
                        Button {
                            if viewModel.pin.count < maxDigits {
                                viewModel.pin.append(num)
                            }
                        } label: {
                            Text(num)
                                .font(.title)
                                .frame(width: 60, height: 60)
                        }
                    }

                    Button {
                        viewModel.pin = ""
                    } label: {
                        Text("pin.actions.clear".localized())
                            .font(.body)
                            .frame(width: 60, height: 60)
                    }

                    Button {
                        if viewModel.pin.count < maxDigits {
                            viewModel.pin.append("0")
                        }
                    } label: {
                        Text("0")
                            .font(.title)
                            .frame(width: 60, height: 60)
                    }

                    Button {
                        if !viewModel.pin.isEmpty {
                            viewModel.pin.removeLast()
                        }
                    } label: {
                        Image(systemName: "delete.left")
                            .font(.title)
                            .frame(width: 60, height: 60)
                    }
                }

                Button("pin.actions.complete".localized()) {
                    onComplete()
                }
                .buttonStyle(.borderedProminent)
                .disabled(!viewModel.isValid)
            }
            .padding(.bottom)
        }
        .padding(.horizontal)
    }
}
