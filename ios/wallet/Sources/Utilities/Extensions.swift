import Foundation

extension Data {
    // Parameter pin: pin must be 4 ~ 8 character.
    // string to convert, example: "1234" → [0x31, 0x32, 0x33, 0x34]
    init?(pin string: String) {
        guard (4 ... 8).contains(string.count) else { return nil }
        guard let utf8Data = string.data(using: .utf8) else { return nil }
        self = utf8Data
    }
}

extension String {
    func localized(bundle _: Bundle = .main, tableName: String = "Localizable") -> String {
        return NSLocalizedString(self, tableName: tableName, value: self, comment: "")
    }
}
