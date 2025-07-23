import Foundation

public enum Instruction: UInt8, Codable {
    // MARK: PIN/Authentication (0x20–0x2F)

    case verifyPIN = 0x20 // INS_VERIFY_PIN
    case changePIN = 0x22 // INS_CHANGE_PIN
    case resetPIN = 0x24 // INS_RESET_PIN

    // MARK: Key Management (0x30–0x3F)

    case generateKey = 0x30 // INS_GENERATE_KEY
    case getPublicKey = 0x32 // INS_GET_PUBKEY
    case getAllPubKeys = 0x34 // INS_GET_ALL_PUBKEY
    case signData = 0x36 // INS_SIGN

    // MARK: Address Retrieval (0x40–0x4F)

    case getAddress = 0x40 // INS_GET_ADDRESS
    case listAddresses = 0x42 // INS_LIST_ADDRESSES

    // MARK: Metadata (0x50–0x5F)

    case getEEPROMFree = 0x50 // INS_GET_EEPROM_FREE

    // MARK: Unknown

    case unknown = 0xFF

    public init(rawValue: UInt8) {
        self = Instruction(rawValue: rawValue) ?? .unknown
    }
}
