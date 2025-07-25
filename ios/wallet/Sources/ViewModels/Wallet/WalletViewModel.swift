import Combine
import SwiftUI

@MainActor
final class WalletViewModel: ObservableObject {
    // MARK: - Published

    @Published private(set) var wallets: [Wallet] = []
    @Published private(set) var isLoading: Bool = false
    @Published private(set) var errorMessage: String?

    private(set) var walletMap: [UUID: Wallet] = [:]

    private let chainService: ChainServiceProtocol
    private let repository: WalletRepositoryProtocol

    init(
        chainService: ChainService = ChainService(),
        repository: WalletRepository = WalletRepository()
    ) {
        self.chainService = chainService
        self.repository = repository
    }

    func load() {
        Task {
            await loadWallets()
            await refreshAll()
        }
    }

    func filteredWallets(for type: WalletType) -> [Wallet] {
        wallets.filter { $0.type == type }
    }

    func wallet(by id: UUID) -> Wallet? {
        walletMap[id]
    }

    func addWallet(_ wallet: Wallet) {
        wallets.append(wallet)
        walletMap[wallet.id] = wallet
        Task {
            try? await repository.saveWallet(wallet)
        }
    }

    func deleteWallet(id: UUID) {
        wallets.removeAll { $0.id == id }
        walletMap[id] = nil
        Task {
            try? await repository.deleteWallet(id: id)
        }
    }

    func refreshAll() async {
        isLoading = true
        defer { isLoading = false }

        do {
            // price
            let symbols = Set(wallets.map { $0.coin.symbol.uppercased() })
            let priceDict = try await chainService.fetchPrices(for: Array(symbols))

            var updated: [Wallet] = []
            for var wallet in wallets {
                // update price
                if let (price, change) = priceDict[wallet.coin.symbol.uppercased()] {
                    wallet.coin.pricePerUnit = price
                    wallet.coin.changePercent24h = change
                }

                // update balance
                let newBalance = try await chainService.fetchBalance(address: wallet.address,
                                                                     symbol: wallet.coin.symbol,
                                                                     network: wallet.network)
                wallet.balance = newBalance

                updated.append(wallet)
            }

            setWallets(updated)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    // MARK: - Private

    private func loadWallets() async {
        isLoading = true
        defer { isLoading = false }
        do {
            let stored = try await repository.loadInitialWallets()
            setWallets(stored)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func setWallets(_ new: [Wallet]) {
        wallets = new
        walletMap = Dictionary(uniqueKeysWithValues: new.map { ($0.id, $0) })
    }
}
