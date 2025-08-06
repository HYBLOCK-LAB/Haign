package com.haign.wallet;

import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.CryptoException;
import javacard.security.Signature;

/**
 * Utility class for BIP‑32/BIP‑44 hierarchical key derivation.
 */
public class HDKeyDerivation {
	public static final short PRIVATE_KEY_LENGTH = 32;
	public static final short CHAIN_CODE_LENGTH = 32;

	private HmacSha512 hmacSha512;
	private byte[] hmacOutput;
	private byte[] childData;
	private byte[] childOut;

	private final byte[] masterPrivateKey = new byte[PRIVATE_KEY_LENGTH];
	private final byte[] masterChainCode = new byte[CHAIN_CODE_LENGTH];
	private boolean isMasterKeySet = false;

	private static final byte[] SECP256K1_ORDER = new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE,
			(byte) 0xBA, (byte) 0xAE, (byte) 0xDC, (byte) 0xE6,
			(byte) 0xAF, (byte) 0x48, (byte) 0xA0, (byte) 0x3B,
			(byte) 0xBF, (byte) 0xD2, (byte) 0x5E, (byte) 0x8C,
			(byte) 0xD0, (byte) 0x36, (byte) 0x41, (byte) 0x41
	};

	/**
	 * Given a BIP‑44 coinType, return the matching curve order.
	 * Currently Bitcoin(0), Ethereum(60), XRP(144) → secp256k1
	 * Throw SW_DATA_INVALID for unsupported coinType.
	 */
	private byte[] getCurveOrder(byte coinType) {
		switch (coinType & 0xFF) {
			case 0:  // Bitcoin
			case 60:  // Ethereum
			case 144:  // XRP
				return SECP256K1_ORDER;
			// TODO if you want to support other curve lik ed25519...
			// case  236: return ED25519_ORDER;
			default:
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
				return null; // unreachable
		}
	}

	private void putHardenedIndex(short n, byte[] buf, short off) {
		buf[off] = (byte) 0x80;
		buf[(short) (off + 1)] = (byte) 0x00;
		Util.setShort(buf, (short) (off + 2), n);
	}

	public HDKeyDerivation() {
		try {
			hmacSha512 = HmacSha512.getInstance();
		} catch (CryptoException e) {
			// does not support HMAC‑SHA512
			ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
		}
		hmacOutput = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_DESELECT);
		childData = JCSystem.makeTransientByteArray((short) (1 + PRIVATE_KEY_LENGTH + 4), JCSystem.CLEAR_ON_DESELECT);
		childOut = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_DESELECT);
	}

	public void setMasterKey(byte[] seed, short off, short len) {
		deriveMasterKey(seed, off, len, masterPrivateKey, masterChainCode);
	}

	/**
	 * Derive master private key and chain code from seed according to BIP‑32.
	 */
	public void deriveMasterKey(byte[] seed, short seedOff, short seedLen,
	                            byte[] outPrivateKey, byte[] outChainCode) {
		byte[] keyBytes = new byte[]{ 'B', 'i', 't', 'c', 'o', 'i', 'n', ' ', 's', 'e', 'e', 'd' };
		hmacSha512.init(null, Signature.MODE_SIGN, keyBytes, (short) 0, (short) keyBytes.length);
		hmacSha512.sign(seed, seedOff, seedLen, hmacOutput, (short) 0);
		Util.arrayCopyNonAtomic(hmacOutput, (short) 0, outPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(hmacOutput, PRIVATE_KEY_LENGTH, outChainCode, (short) 0, CHAIN_CODE_LENGTH);
		isMasterKeySet = true;
	}

	/**
	 * Derive a hardened child key (private key + chain code) from parent key and chain code.
	 */
	public void deriveChildKeyHardened(byte[] parentPrivateKey, byte[] parentChainCode, byte[] index, byte coinType,
	                                   byte[] outPrivateKey, byte[] outChainCode) {
		if ((index[0] & (byte) 0x80) == 0) {
			// Only hardened derivation on-card
			ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
		}
		short pos = 0;
		// Hardened: 0x00 || parentPriv || indexBE
		childData[pos++] = (byte) 0x00;
		Util.arrayCopyNonAtomic(parentPrivateKey, (short) 0, childData, pos, PRIVATE_KEY_LENGTH);
		pos += PRIVATE_KEY_LENGTH;
		Util.arrayCopyNonAtomic(index, (short) 0, childData, pos, (short) 4);
		pos += 4;

		hmacSha512.init(null, Signature.MODE_SIGN, parentChainCode, (short) 0, CHAIN_CODE_LENGTH);
		hmacSha512.sign(childData, (short) 0, pos, childOut, (short) 0);

		// Compute IL + parentPrivateKey mod curve order
		byte[] tmp = new byte[PRIVATE_KEY_LENGTH];
		MathUtil.add32(childOut, (short) 0, parentPrivateKey, (short) 0, tmp, (short) 0);
		byte[] curveOrder = getCurveOrder(coinType);
		MathUtil.mod32(tmp, (short) 0, curveOrder, (short) 0, outPrivateKey, (short) 0);

		// Copy IR to chain code
		Util.arrayCopyNonAtomic(childOut, PRIVATE_KEY_LENGTH, outChainCode, (short) 0, CHAIN_CODE_LENGTH);
	}

	/**
	 * Non-hardened child derivation: k_i = (IL + k_parent) mod n using parent public key
	 *
	 * @param parentPublicKey  parent public key (compressed 33-byte SEC format)
	 * @param parentPrivateKey parent private key (32 bytes)
	 * @param parentChainCode  parent chain code (32 bytes)
	 * @param index            non-hardened index (0 ≤ index < HARDENED_OFFSET)
	 * @param coinType         coin type
	 * @param outPrivateKey    output private key (32 bytes)
	 * @param outChainCode     output chain code (32 bytes)
	 */
	public void deriveChildKeyNonHardened(byte[] parentPublicKey, byte[] parentPrivateKey, byte[] parentChainCode,
	                                      byte[] index, byte coinType, byte[] outPrivateKey, byte[] outChainCode) {
		if ((index[0] & (byte) 0x80) == 0) {
			// Only hardened derivation on-card
			ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
		}
		short pos = 0;
		Util.arrayCopyNonAtomic(parentPublicKey, (short) 0, childData, pos, (short) 33);
		pos += 33;
		Util.arrayCopyNonAtomic(index, (short) 0, childData, pos, (short) 4);
		pos += 4;

		hmacSha512.init(null, Signature.MODE_SIGN, parentChainCode, (short) 0, CHAIN_CODE_LENGTH);
		hmacSha512.sign(childData, (short) 0, pos, childOut, (short) 0);

		// Compute IL + parentPrivateKey mod curve order
		byte[] tmp = new byte[PRIVATE_KEY_LENGTH];
		MathUtil.add32(childOut, (short) 0, parentPrivateKey, (short) 0, tmp, (short) 0);
		byte[] order = getCurveOrder(coinType);
		MathUtil.mod32(tmp, (short) 0, order, (short) 0, outPrivateKey, (short) 0);

		// Copy IR to chain code
		Util.arrayCopyNonAtomic(childOut, PRIVATE_KEY_LENGTH, outChainCode, (short) 0, CHAIN_CODE_LENGTH);
	}

	/**
	 * Derive full BIP‑44 path with all hardened steps:
	 * m/44'/coinType'/account'/change'/addressIndex'
	 *  TODO use deriveBip44 instead of this function
	 */
	public void deriveBip44FullyHardened(
			byte coinType,
			byte account,
			byte change,
			short addressIndex,
			byte[] outPrivateKey,
			byte[] outChainCode) {
		if (!isMasterKeySet) {
			ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
		}
		byte[] currentPrivateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] currentChainCode = new byte[CHAIN_CODE_LENGTH];
		byte[] nextPrivateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] nextChainCode = new byte[CHAIN_CODE_LENGTH];
		Util.arrayCopyNonAtomic(masterPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(masterChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);

		byte[] idxBuf = new byte[4];

		// m/44'
		putHardenedIndex((short) 44, idxBuf, (short) 0);
		deriveChildKeyHardened(
				currentPrivateKey, currentChainCode,
				idxBuf,
				coinType,
				nextPrivateKey, nextChainCode
		);
		Util.arrayCopyNonAtomic(nextPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(nextChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);


		// m/44'/coinType'
		putHardenedIndex((short) (account & 0xFF), idxBuf, (short) 0);
		deriveChildKeyHardened(
				currentPrivateKey, currentChainCode,
				idxBuf,
				coinType,
				nextPrivateKey, nextChainCode
		);
		Util.arrayCopyNonAtomic(nextPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(nextChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);


		// m/44'/coinType'/account''
		putHardenedIndex((short) (account & 0xFF), idxBuf, (short) 0);

		deriveChildKeyHardened(
				currentPrivateKey, currentChainCode,
				idxBuf,
				coinType,
				nextPrivateKey, nextChainCode
		);
		Util.arrayCopyNonAtomic(nextPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(nextChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);


		// m/44'/coinType'/account'/change'
		putHardenedIndex((short) (change & 0xFF), idxBuf, (short) 0);
		deriveChildKeyHardened(
				currentPrivateKey, currentChainCode,
				idxBuf,
				coinType,
				nextPrivateKey, nextChainCode
		);
		Util.arrayCopyNonAtomic(nextPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(nextChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);


		// m/44'/coinType'/account'/change'/addressIndex'
		putHardenedIndex(addressIndex, idxBuf, (short) 0);
		deriveChildKeyHardened(
				currentPrivateKey, currentChainCode,
				idxBuf,
				coinType,
				nextPrivateKey, nextChainCode
		);

		Util.arrayCopyNonAtomic(nextPrivateKey, (short) 0, outPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(nextChainCode, (short) 0, outChainCode, (short) 0, CHAIN_CODE_LENGTH);
	}

	/**
	 * Derive full BIP-44 path: m/44'/coinType'/account'/change/addressIndex
	 */
	public void deriveBip44(
			byte coinType,
			byte account,
			byte change,
			short addressIndex,
			byte[] pubAfterAccount,
			byte[] pubAfterChange,
			byte[] outPrivateKey,
			byte[] outChainCode) {
		byte[] currentPrivateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] currentChainCode = new byte[CHAIN_CODE_LENGTH];
		Util.arrayCopyNonAtomic(masterPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(masterChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);

		// Hardened for 44', coinType', account'
//		int[] hardPath = new int[]{
//				HARDENED_OFFSET + 44,
//				HARDENED_OFFSET + (coinType & 0xFF),
//				HARDENED_OFFSET + (account & 0xFF)
//		};
//		for (int idx = 0; idx < hardPath.length; idx++) {
//			byte[] nextPrivateKey = new byte[PRIVATE_KEY_LENGTH];
//			byte[] nextChainCode = new byte[CHAIN_CODE_LENGTH];
//			deriveChildKeyHardened(currentPrivateKey, currentChainCode, hardPath[idx], coinType, nextPrivateKey, nextChainCode);
//			Util.arrayCopyNonAtomic(nextPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
//			Util.arrayCopyNonAtomic(nextChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);
//		}
		byte[] idxBuf = new byte[4];

		// m/44'
		putHardenedIndex((short) 44, idxBuf, (short) 0);
		deriveChildKeyHardened(
				currentPrivateKey, currentChainCode,
				idxBuf,
				coinType,
				currentPrivateKey, currentChainCode
		);

		// m/44'/coinType'
		putHardenedIndex((short) (coinType & 0xFF), idxBuf, (short) 0);
		deriveChildKeyHardened(
				currentPrivateKey, currentChainCode,
				idxBuf,
				coinType,
				currentPrivateKey, currentChainCode
		);

		// m/44'/coinType'/account'
		putHardenedIndex((short) (account & 0xFF), idxBuf, (short) 0);
		deriveChildKeyHardened(
				currentPrivateKey, currentChainCode,
				idxBuf,
				coinType,
				currentPrivateKey, currentChainCode
		);

		byte[] tmpPrivateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] tmpChainCode = new byte[CHAIN_CODE_LENGTH];
		byte[] tmpChange = new byte[2];
		Util.setShort(tmpChange, (short) 0, (short) (change & 0xFF));
		deriveChildKeyNonHardened(pubAfterAccount, currentPrivateKey, currentChainCode, tmpChange, coinType, tmpPrivateKey, tmpChainCode);

		byte[] tmpAddressIndex = new byte[2];
		Util.setShort(tmpChange, (short) 0, (short) (addressIndex & 0xFF));
		deriveChildKeyNonHardened(pubAfterChange, tmpPrivateKey, tmpChainCode, tmpAddressIndex, coinType, outPrivateKey, outChainCode);
	}
}

