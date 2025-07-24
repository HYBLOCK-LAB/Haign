import SwiftUI

struct PinSetupView: View {
    @ObservedObject var viewModel: PinViewModel
    @Binding var isPinSet: Bool
    let maxDigits: Int = 8

    var body: some View {
        VStack(alignment: .center) {
            Spacer()

            // Instruction and PIN Viewer
            VStack(alignment: .leading, spacing: 8) {
                Text("pin.instructions.part1".localized())
                    .font(.body)
                    .foregroundColor(.primary)

                Text("pin.instructions.part2".localized())
                    .font(.body)
                    .bold()
                    .foregroundColor(.primary)
                    + Text("pin.instructions.part3".localized())
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

            // Keypad and Confirm fixed at bottom
            VStack(spacing: 20) {
                LazyVGrid(
                    columns: Array(repeating: GridItem(.flexible(), spacing: 20), count: 3),
                    spacing: 20
                ) {
                    // Digits 1-9
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

                    // Clear all button
                    Button {
                        viewModel.pin = ""
                    } label: {
                        Text("pin.actions.clear".localized())
                            .font(.body)
                            .frame(width: 60, height: 60)
                    }

                    // Zero button
                    Button {
                        if viewModel.pin.count < maxDigits {
                            viewModel.pin.append("0")
                        }
                    } label: {
                        Text("0")
                            .font(.title)
                            .frame(width: 60, height: 60)
                    }

                    // Delete last digit
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
                    viewModel.savePin()
                    isPinSet = true
                }
                .buttonStyle(.borderedProminent)
                .disabled(!viewModel.isValid)
            }
            .padding(.bottom)
        }
        .padding(.horizontal)
    }
}
