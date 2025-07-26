import WalletCore

enum MnemonicUtils {
    static func generateMnemonicAndSeed(passphrase: String = "") -> (mnemonic: String, seed: Data) {
        let wallet = HDWallet(strength: 256, passphrase: passphrase)
        return (wallet!.mnemonic, wallet!.seed)
    }

    static func isValid(_ mnemonic: String) -> Bool {
        Mnemonic.isValid(mnemonic: mnemonic)
    }

    static func seed(from mnemonic: String, passphrase: String = "") -> Data? {
        guard isValid(mnemonic) else { return nil }
        return HDWallet(mnemonic: mnemonic, passphrase: passphrase)?.seed
    }
}
