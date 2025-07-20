package com.haign.wallet;

import javacard.security.MessageDigest;

public class CryptoUtil {

	public static void doubleSHA256(byte[] in, short inOff, short inLen, byte[] out, short outOff) {
		MessageDigest sha = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
		sha.doFinal(in, inOff, inLen, out, outOff);
		sha.doFinal(out, outOff, (short) 32, out, outOff);
	}

	public static void keccak256(byte[] in, short inOff, short inLen, byte[] out, short outOff) {
		Keccak256 keccak = new Keccak256();
		keccak.reset();
		keccak.digest(in, inOff, inLen, out, outOff);
	}

	public static void sha512Half(byte[] in, short inOff, short inLen, byte[] out, short outOff) {
		MessageDigest sha = MessageDigest.getInstance(MessageDigest.ALG_SHA_512, false);
		sha.doFinal(in, inOff, inLen, out, outOff); // SHA-512: 64 bytes
		// Only use the first 32 bytes (SHA-512Half)
		// Already in 'out' so nothing more needed if used directly
	}
}
