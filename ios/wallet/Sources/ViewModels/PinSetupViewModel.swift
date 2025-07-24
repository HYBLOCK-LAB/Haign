import Combine
import Foundation

class PinViewModel: ObservableObject {
    @Published var pin: String = ""
    @Published var isValid: Bool = false
    @Published var isPinSet: Bool = false

    private let service: PinServiceProtocol
    private var cancellables = Set<AnyCancellable>()

    init(service: PinServiceProtocol = PinService()) {
        self.service = service
        // Check stored PIN when start application
        isPinSet = service.isPinSet()

        $pin
            .map { $0.count >= 4 && $0.count <= 8 }
            .assign(to: \ .isValid, on: self)
            .store(in: &cancellables)
    }

    func savePin() {
        service.savePin(pin)
        isPinSet = true
    }

    func verifyPin(_ input: String) -> Bool {
        return service.verifyPin(input)
    }
}
