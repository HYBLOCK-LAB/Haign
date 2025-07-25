/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2022, ThothTrust Private Limited
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.haign.wallet;

import javacard.framework.Util;

/*
 * 64 bit Math in 8 bit format.
 */
public class MathUtil {

	public static void and64(byte[] a, short aOffset, byte[] b, short bOffset, byte[] result, short offset) {
		result[offset] = (byte) (a[aOffset] & b[bOffset]);
		result[(short) (offset + 1)] = (byte) (a[(short) (aOffset + 1)] & b[(short) (bOffset + 1)]);
		result[(short) (offset + 2)] = (byte) (a[(short) (aOffset + 2)] & b[(short) (bOffset + 2)]);
		result[(short) (offset + 3)] = (byte) (a[(short) (aOffset + 3)] & b[(short) (bOffset + 3)]);
		result[(short) (offset + 4)] = (byte) (a[(short) (aOffset + 4)] & b[(short) (bOffset + 4)]);
		result[(short) (offset + 5)] = (byte) (a[(short) (aOffset + 5)] & b[(short) (bOffset + 5)]);
		result[(short) (offset + 6)] = (byte) (a[(short) (aOffset + 6)] & b[(short) (bOffset + 6)]);
		result[(short) (offset + 7)] = (byte) (a[(short) (aOffset + 7)] & b[(short) (bOffset + 7)]);
	}

	public static void xor64(byte[] a, short aOffset, byte[] b, short bOffset, byte[] result, short offset) {
		result[offset] = (byte) (a[aOffset] ^ b[bOffset]);
		result[(short) (offset + 1)] = (byte) (a[(short) (aOffset + 1)] ^ b[(short) (bOffset + 1)]);
		result[(short) (offset + 2)] = (byte) (a[(short) (aOffset + 2)] ^ b[(short) (bOffset + 2)]);
		result[(short) (offset + 3)] = (byte) (a[(short) (aOffset + 3)] ^ b[(short) (bOffset + 3)]);
		result[(short) (offset + 4)] = (byte) (a[(short) (aOffset + 4)] ^ b[(short) (bOffset + 4)]);
		result[(short) (offset + 5)] = (byte) (a[(short) (aOffset + 5)] ^ b[(short) (bOffset + 5)]);
		result[(short) (offset + 6)] = (byte) (a[(short) (aOffset + 6)] ^ b[(short) (bOffset + 6)]);
		result[(short) (offset + 7)] = (byte) (a[(short) (aOffset + 7)] ^ b[(short) (bOffset + 7)]);
	}

	public static void rotl64(byte[] a, short offset, short amt, byte[] buff, short buffOffset, short sBuff1,
	                          short sBuff2, short sBuff3, short sBuff4) {
		sBuff1 = (short) (amt % 8);
		sBuff2 = (short) (amt / 8);
		buff[buffOffset] = a[offset];
		for (sBuff3 = (short) 7; sBuff3 >= (short) 0; sBuff3--) {
			buff[(short) (buffOffset + 1)] = a[sBuff3];
			sBuff4 = (short) ((short) (sBuff3 - sBuff2 + 8) % 8);
			buff[(short) (buffOffset + 2 + sBuff4)] = (byte) ((byte) (buff[(short) (buffOffset + 1)] << sBuff1)
					| (((byte) buff[buffOffset] & 0xff) >>> ((short) 8 - sBuff1)));
			buff[buffOffset] = buff[(short) (buffOffset + 1)];
		}

		Util.arrayCopyNonAtomic(buff, (short) (buffOffset + 2), a, offset, (short) 8);
	}

	public static void flip64(byte[] a, short offset) {
		a[offset] = (byte) ~a[offset];
		a[(short) (offset + 1)] = (byte) ~a[(short) (offset + 1)];
		a[(short) (offset + 2)] = (byte) ~a[(short) (offset + 2)];
		a[(short) (offset + 3)] = (byte) ~a[(short) (offset + 3)];
		a[(short) (offset + 4)] = (byte) ~a[(short) (offset + 4)];
		a[(short) (offset + 5)] = (byte) ~a[(short) (offset + 5)];
		a[(short) (offset + 6)] = (byte) ~a[(short) (offset + 6)];
		a[(short) (offset + 7)] = (byte) ~a[(short) (offset + 7)];
	}

	public static void littleEndian64(byte[] input, short inOffset, byte[] output, short outOffset, byte buff) {
		// Swap pos 0 and 7
		buff = input[(short) (inOffset + 7)];
		output[(short) (outOffset + 7)] = input[inOffset];
		output[outOffset] = buff;

		// Swap pos 1 and 6
		buff = input[(short) (inOffset + 6)];
		output[(short) (outOffset + 6)] = input[(short) (inOffset + 1)];
		output[(short) (outOffset + 1)] = buff;

		// Swap pos 2 and 5
		buff = input[(short) (inOffset + 5)];
		output[(short) (outOffset + 5)] = input[(short) (inOffset + 2)];
		output[(short) (outOffset + 2)] = buff;

		// Swap pos 3 and 4
		buff = input[(short) (inOffset + 4)];
		output[(short) (outOffset + 4)] = input[(short) (inOffset + 3)];
		output[(short) (outOffset + 3)] = buff;
	}
}