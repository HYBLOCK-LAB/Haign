public enum StatusWord: UInt16, Codable {
    // Custom Status Words (0x6F11–0x6F15)
    case illegalValue = 0x6F11 // ILLEGAL_VALUE
    case uninitializedKey = 0x6F12 // UNINITIALIZED_KEY
    case noSuchAlgorithm = 0x6F13 // NO_SUCH_ALGORITHM
    case invalidInit = 0x6F14 // INVALID_INIT
    case illegalUse = 0x6F15 // ILLEGAL_USE

    // ISO7816 Standard Status Words
    case wrongLength = 0x6700 // SW_WRONG_LENGTH
    case securityNotSatisfied = 0x6982 // SW_SECURITY_STATUS_NOT_SATISFIED
    case fileInvalid = 0x6983 // SW_FILE_INVALID
    case dataInvalid = 0x6984 // SW_DATA_INVALID
    case conditionsNotSatisfied = 0x6985 // SW_CONDITIONS_NOT_SATISFIED
    case wrongData = 0x6A80 // SW_WRONG_DATA
    case funcNotSupported = 0x6A81 // SW_FUNC_NOT_SUPPORTED
    case fileNotFound = 0x6A82 // SW_FILE_NOT_FOUND
    case recordNotFound = 0x6A83 // SW_RECORD_NOT_FOUND
    case incorrectP1P2 = 0x6A86 // SW_INCORRECT_P1P2
    case wrongP1P2 = 0x6A88 // SW_WRONG_P1P2
    case insNotSupported = 0x6D00 // SW_INS_NOT_SUPPORTED
    case claNotSupported = 0x6E00 // SW_CLA_NOT_SUPPORTED
    case fileFull = 0x6A84 // SW_FILE_FULL

    case unknown = 0x0000 // 기타

    public init(sw1: UInt8, sw2: UInt8) {
        let combined = UInt16(sw1) << 8 | UInt16(sw2)
        self = StatusWord(rawValue: combined) ?? .unknown
    }
}
