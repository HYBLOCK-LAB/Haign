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
    func localized(comment: String = "", bundle _: Bundle = .main, tableName: String = "Localizable") -> String {
        return NSLocalizedString(self, tableName: tableName, value: self, comment: comment)
    }
}

extension Bundle {
    private static var bundleKey: UInt8 = 0

    static func setLanguage(_ language: String) {
        object_setClass(Bundle.main, PrivateBundle.self)
        objc_setAssociatedObject(Bundle.main, &bundleKey, language, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }

    private class PrivateBundle: Bundle, @unchecked Sendable {
        override func localizedString(forKey key: String, value: String?, table tableName: String?) -> String {
            guard let language = objc_getAssociatedObject(self, &Bundle.PrivateBundle.bundleKey) as? String,
                  let path = Bundle.main.path(forResource: language, ofType: "lproj"),
                  let bundle = Bundle(path: path)
            else {
                return super.localizedString(forKey: key, value: value, table: tableName)
            }

            return bundle.localizedString(forKey: key, value: value, table: tableName)
        }
    }
}
