struct Coin: Codable, Hashable {
    let symbol: String
    var pricePerUnit: Double // USD
    var changePercent24h: Double?

    var iconName: String {
        switch symbol.uppercased() {
        case "BTC": return "Bitcoin"
        case "ETH": return "Ethereum"
        case "XRP": return "XRP"
        default: return "DefaultCoin"
        }
    }
}
