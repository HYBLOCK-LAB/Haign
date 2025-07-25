import SwiftUI

struct PinSettingView: View {
    @StateObject private var viewModel = PinViewModel()
    let maxDigits: Int = 8

    var body: some View {
        PinInputView(viewModel: viewModel, maxDigits: maxDigits) {
            viewModel.savePin()
        }
        .navigationTitle("settings.option.pin".localized())
    }
}
