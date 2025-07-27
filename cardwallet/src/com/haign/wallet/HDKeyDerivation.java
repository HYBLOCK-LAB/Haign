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
	public static final int HARDENED_OFFSET = 0x80000000;

	private Signature hmacSha512;
	private byte[] hmacOutput;
	private byte[] childData;
	private byte[] childOut;

	private final byte[] masterPrivateKey = new byte[PRIVATE_KEY_LENGTH];
	private final byte[] masterChainCode = new byte[CHAIN_CODE_LENGTH];

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

	public HDKeyDerivation() {
		try {
			hmacSha512 = Signature.getInstance(Signature.ALG_HMAC_SHA_512, false);
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
	}

	/**
	 * Derive a hardened child key (private key + chain code) from parent key and chain code.
	 */
	public void deriveChildKeyHardened(byte[] parentPrivateKey, byte[] parentChainCode, int index, byte coinType,
	                                   byte[] outPrivateKey, byte[] outChainCode) {
		if (index < HARDENED_OFFSET) {
			// Only hardened derivation on-card
			ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
		}
		short pos = 0;
		// Hardened: 0x00 || parentPriv || indexBE
		childData[pos++] = (byte) 0x00;
		Util.arrayCopyNonAtomic(parentPrivateKey, (short) 0, childData, pos, PRIVATE_KEY_LENGTH);
		pos += PRIVATE_KEY_LENGTH;
		childData[pos++] = (byte) (index >>> 24);
		childData[pos++] = (byte) (index >>> 16);
		childData[pos++] = (byte) (index >>> 8);
		childData[pos++] = (byte) index;

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
	                                      int index, byte coinType, byte[] outPrivateKey, byte[] outChainCode) {
		if (index >= HARDENED_OFFSET) {
			ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
		}
		short pos = 0;
		Util.arrayCopyNonAtomic(parentPublicKey, (short) 0, childData, pos, (short) 33);
		pos += 33;
		childData[pos++] = (byte) (index >>> 24);
		childData[pos++] = (byte) (index >>> 16);
		childData[pos++] = (byte) (index >>> 8);
		childData[pos++] = (byte) (index);

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
			int addressIndex,
			byte[] outPrivateKey,
			byte[] outChainCode) {
		byte[] currentPrivateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] currentChainCode = new byte[CHAIN_CODE_LENGTH];
		Util.arrayCopyNonAtomic(masterPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(masterChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);
		int[] path = new int[]{
				HARDENED_OFFSET + 44,
				HARDENED_OFFSET + (coinType & 0xFF),
				HARDENED_OFFSET + (account & 0xFF),
				HARDENED_OFFSET + (change & 0xFF),
				HARDENED_OFFSET + addressIndex
		};

		for (int i = 0; i < path.length; i++) {
			byte[] nextPrivateKey = new byte[PRIVATE_KEY_LENGTH];
			byte[] nextChainCode = new byte[CHAIN_CODE_LENGTH];

			deriveChildKeyHardened(
					currentPrivateKey,
					currentChainCode,
					path[i],
					coinType,
					nextPrivateKey,
					nextChainCode
			);

			// current ← next
			Util.arrayCopyNonAtomic(nextPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
			Util.arrayCopyNonAtomic(nextChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);
		}

		Util.arrayCopyNonAtomic(currentPrivateKey, (short) 0, outPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(currentChainCode, (short) 0, outChainCode, (short) 0, CHAIN_CODE_LENGTH);
	}

	/**
	 * Derive full BIP-44 path: m/44'/coinType'/account'/change/addressIndex
	 */
	public void deriveBip44(
			byte coinType,
			byte account,
			byte change,
			int addressIndex,
			byte[] pubAfterAccount,
			byte[] pubAfterChange,
			byte[] outPrivateKey,
			byte[] outChainCode) {
		byte[] currentPrivateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] currentChainCode = new byte[CHAIN_CODE_LENGTH];
		Util.arrayCopyNonAtomic(masterPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(masterChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);

		// Hardened for 44', coinType', account'
		int[] hardPath = new int[]{
				HARDENED_OFFSET + 44,
				HARDENED_OFFSET + (coinType & 0xFF),
				HARDENED_OFFSET + (account & 0xFF)
		};
		for (int idx = 0; idx < hardPath.length; idx++) {
			byte[] nextPrivateKey = new byte[PRIVATE_KEY_LENGTH];
			byte[] nextChainCode = new byte[CHAIN_CODE_LENGTH];
			deriveChildKeyHardened(currentPrivateKey, currentChainCode, hardPath[idx], coinType, nextPrivateKey, nextChainCode);
			Util.arrayCopyNonAtomic(nextPrivateKey, (short) 0, currentPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
			Util.arrayCopyNonAtomic(nextChainCode, (short) 0, currentChainCode, (short) 0, CHAIN_CODE_LENGTH);
		}

		byte[] tmpPrivateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] tmpChainCode = new byte[CHAIN_CODE_LENGTH];
		deriveChildKeyNonHardened(pubAfterAccount, currentPrivateKey, currentChainCode, change & 0xFF, coinType, tmpPrivateKey, tmpChainCode);

		deriveChildKeyNonHardened(pubAfterChange, tmpPrivateKey, tmpChainCode, addressIndex, coinType, outPrivateKey, outChainCode);
	}
}

