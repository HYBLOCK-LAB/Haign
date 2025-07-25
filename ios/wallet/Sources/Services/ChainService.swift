protocol ChainServiceProtocol {
    func fetchPrices(for symbols: [String]) async throws -> [String: (price: Double, change24h: Double?)]

    func fetchBalance(address: String, symbol: String, network: NetworkType) async throws -> Double
}

struct ChainService: ChainServiceProtocol {
    func fetchPrices(for symbols: [String]) async throws -> [String: (price: Double, change24h: Double?)] {
        // TODO: replace real API
        var result: [String: (Double, Double?)] = [:]
        for s in symbols {
            result[s.uppercased()] = (price: Double.random(in: 2.5 ... 4), change24h: Double.random(in: -10 ... 10))
        }
        return result
    }

    func fetchBalance(address _: String, symbol _: String, network _: NetworkType) async throws -> Double {
        // TODO: replace real on-chain API
        return 100 // Double.random(in: 0 ... 5)
    }
}
