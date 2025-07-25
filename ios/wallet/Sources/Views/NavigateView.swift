import SwiftUI

struct NavigateView: View {
    @StateObject private var pinViewModel = PinViewModel()

    var body: some View {
        NavigationStack {
            if pinViewModel.isPinSet {
                MainTabView().navigationBarHidden(true)
            } else {
                PinSetupView(viewModel: pinViewModel, isPinSet: $pinViewModel.isPinSet).navigationBarHidden(true)
            }
        }

        .animation(.default, value: pinViewModel.isPinSet)
    }
}

struct MyView_Previews: PreviewProvider {
    static var previews: some View {
        NavigateView()
    }
}
