import Foundation

protocol WalletRepositoryProtocol {
    func loadInitialWallets() async throws -> [Wallet]
    func saveWallet(_ wallet: Wallet) async throws
    func deleteWallet(id: UUID) async throws
}

struct WalletRepository: WalletRepositoryProtocol {
    private var storage: [Wallet] = [
        Wallet(name: "Card XRP", address: "rB7x1A1zP1yGZx29vU",
               network: .testnet, type: .card, balance: 100,
               coin: Coin(symbol: "XRP", pricePerUnit: 0.65, changePercent24h: -0.12)),
    ]

    func loadInitialWallets() async throws -> [Wallet] {
        storage
    }

    func saveWallet(_: Wallet) async throws {
        // MARK: TODO store on DB/Keychain/UserDefaults/CoreData
    }

    func deleteWallet(id _: UUID) async throws {
        // MARK: TODO implement
    }
}
