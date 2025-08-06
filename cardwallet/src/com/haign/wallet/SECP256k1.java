package com.haign.wallet;

import javacard.framework.Util;
import javacard.security.KeyBuilder;

public class SECP256k1 {

	public static final short FIELD_LEN_BYTES = (short) (KeyBuilder.LENGTH_EC_FP_256 / 8);
	public static final short POINT_LEN = (short) (FIELD_LEN_BYTES * 2 + 1);

	// prime p (32B)
	public final byte[] p = new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC, (byte) 0x2F
	};

	// a=0, b=7
	public final byte[] a = new byte[FIELD_LEN_BYTES];
	public final byte[] b = new byte[FIELD_LEN_BYTES];

	// G, r
	public final byte[] G = new byte[POINT_LEN];
	public final byte[] r = new byte[FIELD_LEN_BYTES];

	// Curve constants
	private static final byte[] GX = new byte[]{
			(byte) 0x79, (byte) 0xBE, (byte) 0x66, (byte) 0x7E, (byte) 0xF9, (byte) 0xDC, (byte) 0xBB, (byte) 0xAC,
			(byte) 0x55, (byte) 0xA0, (byte) 0x62, (byte) 0x95, (byte) 0xCE, (byte) 0x87, (byte) 0x0B, (byte) 0x07,
			(byte) 0x02, (byte) 0x9B, (byte) 0xFC, (byte) 0xDB, (byte) 0x2D, (byte) 0xCE, (byte) 0x28, (byte) 0xD9,
			(byte) 0x59, (byte) 0xF2, (byte) 0x81, (byte) 0x5B, (byte) 0x16, (byte) 0xF8, (byte) 0x17, (byte) 0x98
	};
	private static final byte[] GY = new byte[]{
			(byte) 0x48, (byte) 0x3A, (byte) 0xDA, (byte) 0x77, (byte) 0x26, (byte) 0xA3, (byte) 0xC4, (byte) 0x65,
			(byte) 0x5D, (byte) 0xA4, (byte) 0xFB, (byte) 0xFC, (byte) 0x0E, (byte) 0x11, (byte) 0x08, (byte) 0xA8,
			(byte) 0xFD, (byte) 0x17, (byte) 0xB4, (byte) 0x48, (byte) 0xA6, (byte) 0x85, (byte) 0x54, (byte) 0x19,
			(byte) 0x9C, (byte) 0x47, (byte) 0xD0, (byte) 0x8F, (byte) 0xFB, (byte) 0x10, (byte) 0xD4, (byte) 0xB8
	};
	private static final byte[] ORDER = new byte[]{
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
			(byte) 0xBA, (byte) 0xAE, (byte) 0xDC, (byte) 0xE6, (byte) 0xAF, (byte) 0x48, (byte) 0xA0, (byte) 0x3B,
			(byte) 0xBF, (byte) 0xD2, (byte) 0x5E, (byte) 0x8C, (byte) 0xD0, (byte) 0x36, (byte) 0x41, (byte) 0x41,
	};

	public SECP256k1() {
		// a = 0
		// b = 7
		b[(short) (FIELD_LEN_BYTES - 1)] = 0x07;

		// G = 0x04 || GX || GY
		G[0] = 0x04;
		Util.arrayCopyNonAtomic(GX, (short) 0, G, (short) 1, FIELD_LEN_BYTES);
		Util.arrayCopyNonAtomic(GY, (short) 0, G, (short) (1 + FIELD_LEN_BYTES), FIELD_LEN_BYTES);

		// r = ORDER
		Util.arrayCopyNonAtomic(ORDER, (short) 0, r, (short) 0, FIELD_LEN_BYTES);
	}
}