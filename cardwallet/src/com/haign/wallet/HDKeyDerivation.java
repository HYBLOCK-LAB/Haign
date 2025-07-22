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

	public HDKeyDerivation() {
		try {
			hmacSha512 = Signature.getInstance(Signature.ALG_HMAC_SHA_512, false);
		} catch (CryptoException e) {
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
	 * Non-hardened derivation is not supported on-card.
	 */
	public void deriveChildKey(byte[] parentPrivateKey, byte[] parentChainCode, int index,
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

		Util.arrayCopyNonAtomic(childOut, (short) 0, outPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(childOut, PRIVATE_KEY_LENGTH, outChainCode, (short) 0, CHAIN_CODE_LENGTH);
	}


	/**
	 * Derive full hardened BIP‑44 path using internal master key.
	 */
	public void derivePath(int[] path, byte[] outPrivateKey, byte[] outChainCode) {
		byte[] privateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] chainCode = new byte[CHAIN_CODE_LENGTH];
		byte[] nextP = new byte[PRIVATE_KEY_LENGTH];
		byte[] nextC = new byte[CHAIN_CODE_LENGTH];
		Util.arrayCopyNonAtomic(masterPrivateKey, (short) 0, privateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(masterChainCode, (short) 0, chainCode, (short) 0, CHAIN_CODE_LENGTH);
		for (short i = 0; i < path.length; i++) {
			deriveChildKey(privateKey, chainCode, path[i], nextP, nextC);
			Util.arrayCopyNonAtomic(nextP, (short) 0, privateKey, (short) 0, PRIVATE_KEY_LENGTH);
			Util.arrayCopyNonAtomic(nextC, (short) 0, chainCode, (short) 0, CHAIN_CODE_LENGTH);
		}
		Util.arrayCopyNonAtomic(privateKey, (short) 0, outPrivateKey, (short) 0, PRIVATE_KEY_LENGTH);
		Util.arrayCopyNonAtomic(chainCode, (short) 0, outChainCode, (short) 0, CHAIN_CODE_LENGTH);
	}
}

