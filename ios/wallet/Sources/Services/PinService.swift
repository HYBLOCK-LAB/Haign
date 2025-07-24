import Foundation

protocol PinServiceProtocol {
    func savePin(_ pin: String)
    func verifyPin(_ input: String) -> Bool
    func isPinSet() -> Bool
}

class PinService: PinServiceProtocol {
    private let service = "walletPin"
    private let account = "userPin"

    func savePin(_ pin: String) {
        let salt = randomSalt()
        guard let hash = pbkdf2(password: pin, salt: salt) else { return }
        let combined = salt + hash
        KeychainHelper.shared.save(combined, service: service, account: account)
    }

    func verifyPin(_ input: String) -> Bool {
        guard let combined = KeychainHelper.shared.read(service: service, account: account) else {
            return false
        }
        let salt = combined.prefix(16)
        let storedHash = combined.suffix(from: 16)
        guard let newHash = pbkdf2(password: input, salt: salt, keyByteCount: storedHash.count) else {
            return false
        }
        return newHash == storedHash
    }

    func isPinSet() -> Bool {
        return KeychainHelper.shared.read(service: service, account: account) != nil
    }
}
