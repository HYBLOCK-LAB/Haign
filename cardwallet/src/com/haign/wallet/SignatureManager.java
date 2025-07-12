package com.haign.wallet;

import com.haign.crypto.CryptoUtil;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.PrivateKey;
import javacard.security.Signature;

class SignatureManager {
	private final Signature signature;

	public SignatureManager() {
		signature = Signature.getInstance(Signature.ALG_ECDSA_SHA_256, false);
	}

	public void sign(APDU apdu, PrivateKey privateKey, byte coinType) {
		byte[] buffer = apdu.getBuffer();
		short dataOffset = ISO7816.OFFSET_CDATA;
		short dataLen = apdu.setIncomingAndReceive();

		short hashLen;
		// default 32-byte buffer
		byte[] hashBuffer = JCSystem.makeTransientByteArray((short) 32, JCSystem.CLEAR_ON_RESET);

		switch (coinType) {
		case WalletApplet.COIN_BTC:
			// Bitcoin uses double SHA-256
			Util.arrayCopyNonAtomic(buffer, dataOffset, hashBuffer, (short) 0, dataLen);
			CryptoUtil.doubleSHA256(hashBuffer, (short) 0, dataLen, hashBuffer, (short) 0);
			hashLen = 32;
			break;
		case WalletApplet.COIN_ETH:
			// Ethereum uses Keccak-256 (stubbed here)
			CryptoUtil.keccak256(buffer, dataOffset, dataLen, hashBuffer, (short) 0);
			hashLen = 32;
			break;
		case WalletApplet.COIN_XRP:
			// XRPL uses SHA-512Half (first 32 bytes of SHA-512 hash)
			CryptoUtil.sha512Half(buffer, dataOffset, dataLen, hashBuffer, (short) 0);
			hashLen = 32;
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
			return;
		}

		signature.init(privateKey, Signature.MODE_SIGN);
		short sigLen = signature.sign(hashBuffer, (short) 0, hashLen, buffer, (short) 0);

		apdu.setOutgoing();
		apdu.setOutgoingLength(sigLen);
		apdu.sendBytes((short) 0, sigLen);

	}
}
