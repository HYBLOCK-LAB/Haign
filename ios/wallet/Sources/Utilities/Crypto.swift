import CommonCrypto
import Foundation

// To Use to encrypt pin
func pbkdf2(password: String, salt: Data,
            keyByteCount: Int = 32, rounds: Int = 10000) -> Data?
{
    guard let pwdData = password.data(using: .utf8) else { return nil }
    var derived = Data(repeating: 0, count: keyByteCount)

    let result = derived.withUnsafeMutableBytes { derivedBytes in
        salt.withUnsafeBytes { saltBytes in
            CCKeyDerivationPBKDF(
                CCPBKDFAlgorithm(kCCPBKDF2),
                password, pwdData.count,
                saltBytes.bindMemory(to: UInt8.self).baseAddress!, salt.count,
                CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                UInt32(rounds),
                derivedBytes.bindMemory(to: UInt8.self).baseAddress!,
                keyByteCount
            )
        }
    }
    return result == kCCSuccess ? derived : nil
}

func randomSalt(length: Int = 16) -> Data {
    var salt = Data(repeating: 0, count: length)
    _ = salt.withUnsafeMutableBytes { SecRandomCopyBytes(kSecRandomDefault, length, $0.baseAddress!) }
    return salt
}
