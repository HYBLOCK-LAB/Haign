import SwiftUI

struct NavigateView: View {
    @StateObject private var pinViewModel = PinViewModel()

    var body: some View {
        NavigationView {
            if pinViewModel.isPinSet {
                MainTabView()
            } else {
                PinSetupView(viewModel: pinViewModel, isPinSet: $pinViewModel.isPinSet)
            }
        }
        .navigationBarHidden(true)
        .animation(.default, value: pinViewModel.isPinSet)
    }
}
