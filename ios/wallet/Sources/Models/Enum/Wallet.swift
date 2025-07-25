import Foundation

enum WalletType: String, CaseIterable, Identifiable, Codable {
    case app
    case card

    var id: String { rawValue }

    var localizedName: String {
        switch self {
        case .app:
            return "wallet.type.app".localized()
        case .card:
            return "wallet.type.card".localized()
        }
    }
}

struct Wallet: Identifiable, Codable, Hashable {
    let id: UUID
    var name: String
    var address: String
    var network: NetworkType
    var type: WalletType
    var balance: Double
    var coin: Coin

    init(
        id: UUID = UUID(),
        name: String,
        address: String,
        network: NetworkType,
        type: WalletType,
        balance: Double,
        coin: Coin
    ) {
        self.id = id
        self.name = name
        self.address = address
        self.network = network
        self.type = type
        self.balance = balance
        self.coin = coin
    }

    var totalValueUSD: Double {
        balance * coin.pricePerUnit
    }
}
