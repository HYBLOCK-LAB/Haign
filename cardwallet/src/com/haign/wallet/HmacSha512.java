package com.haign.wallet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.CryptoException;
import javacard.security.HMACKey;
import javacard.security.Key;
import javacard.security.MessageDigest;

public class HmacSha512 {
	private static final byte ALG_CUSTOM_HMAC_SHA512 = (byte) 0xF0; // custom algorithm ID
	private static final short BLOCK_SIZE = 128;

	private MessageDigest sha512;
	private byte[] keyBlock;
	private byte[] iPad;
	private byte[] oPad;
	private byte[] innerHash;
	private byte[] outerHash;

	// temporary buffer for verify
	private byte[] tmpBuf;

	public HmacSha512() {
		sha512 = MessageDigest.getInstance(MessageDigest.ALG_SHA_512, false);

		keyBlock = JCSystem.makeTransientByteArray(BLOCK_SIZE, JCSystem.CLEAR_ON_RESET);
		iPad = JCSystem.makeTransientByteArray(BLOCK_SIZE, JCSystem.CLEAR_ON_RESET);
		oPad = JCSystem.makeTransientByteArray(BLOCK_SIZE, JCSystem.CLEAR_ON_RESET);
		innerHash = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_RESET);
		outerHash = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_RESET);
		tmpBuf = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_RESET);

		Util.arrayFillNonAtomic(keyBlock, (short) 0, BLOCK_SIZE, (byte) 0x00);

	}

	public void init(Key key, byte mode) throws CryptoException {
		// Expecting SecretKey with raw bytes
		if (!(key instanceof HMACKey)) {
			CryptoException.throwIt(CryptoException.ILLEGAL_VALUE);
		}
		HMACKey hmacKey = (HMACKey) key;
		byte[] raw = new byte[hmacKey.getKey(new byte[BLOCK_SIZE], (short) 0)];
		short rawLen = hmacKey.getKey(raw, (short) 0);
		Util.arrayFillNonAtomic(keyBlock, (short) 0, BLOCK_SIZE, (byte) 0x00);
		if (rawLen > BLOCK_SIZE) {
			sha512.reset();
			sha512.doFinal(raw, (short) 0, rawLen, keyBlock, (short) 0);
		} else {
			Util.arrayCopy(raw, (short) 0, keyBlock, (short) 0, rawLen);
		}
		for (short i = 0; i < BLOCK_SIZE; i++) {
			byte kb = keyBlock[i];
			iPad[i] = (byte) (kb ^ (byte) 0x36);
			oPad[i] = (byte) (kb ^ (byte) 0x5C);
		}
	}

	public void init(Key key, byte mode, byte[] keyBytes, short keyOff, short keyLen) throws CryptoException {
		Util.arrayFillNonAtomic(keyBlock, (short) 0, BLOCK_SIZE, (byte) 0x00);
		if (keyLen > BLOCK_SIZE) {
			sha512.reset();
			sha512.doFinal(keyBytes, keyOff, keyLen, keyBlock, (short) 0);
		} else {
			Util.arrayCopy(keyBytes, keyOff, keyBlock, (short) 0, keyLen);
		}
		// compute pads
		for (short i = 0; i < BLOCK_SIZE; i++) {
			byte kb = keyBlock[i];
			iPad[i] = (byte) (kb ^ (byte) 0x36);
			oPad[i] = (byte) (kb ^ (byte) 0x5C);
		}
	}


	public short sign(byte[] inBuff, short inOff, short inLen,
	                  byte[] sigBuff, short sigOff) throws CryptoException {
		sha512.reset();
		sha512.update(iPad, (short) 0, BLOCK_SIZE);
		sha512.doFinal(inBuff, inOff, inLen, innerHash, (short) 0);
		sha512.reset();
		sha512.update(oPad, (short) 0, BLOCK_SIZE);
		short hLen = sha512.doFinal(innerHash, (short) 0, (short) innerHash.length, outerHash, (short) 0);
		Util.arrayCopy(outerHash, (short) 0, sigBuff, sigOff, hLen);
		return hLen;
	}


	public boolean verify(byte[] inBuff, short inOff, short inLen,
	                      byte[] sigBuff, short sigOff, short sigLen) throws CryptoException {
		short len = sign(inBuff, inOff, inLen, tmpBuf, (short) 0);
		return Util.arrayCompare(tmpBuf, (short) 0, sigBuff, sigOff, len) == 0;
	}


	public byte getAlgorithm() {
		return ALG_CUSTOM_HMAC_SHA512;
	}


	public short getLength() {
		return 64;
	}

	/**
	 * Factory to create instance.
	 */
	public static HmacSha512 getInstance() {
		return new HmacSha512();
	}
}
