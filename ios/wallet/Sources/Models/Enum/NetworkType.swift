enum NetworkType: String, CaseIterable, Identifiable, Codable {
    case mainnet
    case testnet

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .mainnet:
            return "Main Net"
        case .testnet:
            return "Test Net"
        }
    }
}
