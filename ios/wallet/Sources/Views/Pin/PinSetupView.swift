import SwiftUI

struct PinSetupView: View {
    @ObservedObject var viewModel: PinViewModel
    @Binding var isPinSet: Bool
    let maxDigits: Int = 8

    var body: some View {
        PinInputView(viewModel: viewModel, maxDigits: maxDigits) {
            viewModel.savePin()
            isPinSet = true
        }
    }
}
