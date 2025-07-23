import SwiftUI

struct OnboardingView: View {
    var body: some View {
        ZStack {
            Color("LogoBackground")
                .edgesIgnoringSafeArea(.all)

            Image("OnboardingLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 200, height: 200)
                .opacity(1)
                .offset(y: -50)

            VStack(spacing: 24) {
                Spacer()

                Text("Hyblock 2025")
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)
            }
        }
        .preferredColorScheme(.light)
    }
}
