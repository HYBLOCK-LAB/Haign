import Foundation

public enum CoinType: UInt8, Codable {
    case bitcoin = 0x01
    case ethereum = 0x02
    case xrp = 0x03

    public init(rawValue: UInt8) {
        self = CoinType(rawValue: rawValue) ?? .bitcoin
    }
}
