import Combine
import Foundation

class SignatureViewModel: ObservableObject {
    @Published var message: String = ""
    @Published var signature: String?

    func signMessage() {
        // TODO: CardCommunicationService.sign(message:)
        signature = "SignedDataBase64String=="
    }
}
