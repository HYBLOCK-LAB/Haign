package com.haign.wallet;

public class JCSEC256k1 {

	public final byte[] p;
	public final byte[] a;
	public final byte[] b;
	public final byte[] G;
	public final byte[] r;

	public JCSEC256k1() {
		p = new byte[]{ (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC, (byte) 0x2F };

		a = new byte[]{ (byte) 0x00 };

		b = new byte[]{ (byte) 0x07 };

		G = new byte[]{ (byte) 0x04, (byte) 0x79, (byte) 0xBE, (byte) 0x66, (byte) 0x7E, (byte) 0xF9, (byte) 0xDC,
				(byte) 0xBB, (byte) 0xAC, (byte) 0x55, (byte) 0xA0, (byte) 0x62, (byte) 0x95, (byte) 0xCE, (byte) 0x87,
				(byte) 0x0B, (byte) 0x07, (byte) 0x02, (byte) 0x9B, (byte) 0xFC, (byte) 0xDB, (byte) 0x2D, (byte) 0xCE,
				(byte) 0x28, (byte) 0xD9, (byte) 0x59, (byte) 0xF2, (byte) 0x81, (byte) 0x5B, (byte) 0x16, (byte) 0xF8,
				(byte) 0x17, (byte) 0x98, (byte) 0x48, (byte) 0x3A, (byte) 0xDA, (byte) 0x77, (byte) 0x26, (byte) 0xA3,
				(byte) 0xC4, (byte) 0x65, (byte) 0x5D, (byte) 0xA4, (byte) 0xFB, (byte) 0xFC, (byte) 0x0E, (byte) 0x11,
				(byte) 0x08, (byte) 0xA8, (byte) 0xFD, (byte) 0x17, (byte) 0xB4, (byte) 0x48, (byte) 0xA6, (byte) 0x85,
				(byte) 0x54, (byte) 0x19, (byte) 0x9C, (byte) 0x47, (byte) 0xD0, (byte) 0x8F, (byte) 0xFB, (byte) 0x10,
				(byte) 0xD4, (byte) 0xB8 };

		r = new byte[]{ (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFE, (byte) 0xBA, (byte) 0xAE, (byte) 0xDC,
				(byte) 0xE6, (byte) 0xAF, (byte) 0x48, (byte) 0xA0, (byte) 0x3B, (byte) 0xBF, (byte) 0xD2, (byte) 0x5E,
				(byte) 0x8C, (byte) 0xD0, (byte) 0x36, (byte) 0x41, (byte) 0x41 };
	}
}