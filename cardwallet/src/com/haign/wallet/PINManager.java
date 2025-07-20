package com.haign.wallet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;

class PINManager {
	private static final byte PIN_TRY_LIMIT = (byte) 5;
	private static final byte PIN_MIN_LENGTH = (byte) 4;
	private static final byte PIN_MAX_LENGTH = (byte) 8;
	private final OwnerPIN pin;

	public PINManager() {
		pin = new OwnerPIN(PIN_TRY_LIMIT, PIN_MAX_LENGTH);
		byte[] defaultPin = { (byte) '1', (byte) '2', (byte) '3', (byte) '4' };
		pin.update(defaultPin, (short) 0, (byte) defaultPin.length);
	}

	public void verify(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		byte lc = buffer[ISO7816.OFFSET_LC];
		if (!pin.check(buffer, ISO7816.OFFSET_CDATA, lc)) {
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}
	}

	// instruction: [oldPinLen][oldPin][newPinLen][newPin]
	public void change(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		byte lc = buffer[ISO7816.OFFSET_LC];

		short dataOffset = ISO7816.OFFSET_CDATA;

		byte oldPinLen = buffer[dataOffset];
		byte newPinLen = buffer[(short) (dataOffset + 1 + oldPinLen)];

		if (oldPinLen < PIN_MIN_LENGTH || oldPinLen > PIN_MAX_LENGTH ||
				newPinLen < PIN_MIN_LENGTH || newPinLen > PIN_MAX_LENGTH) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		if (!pin.check(buffer, (short) (dataOffset + 1), oldPinLen)) {
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}

		pin.update(buffer, (short) (dataOffset + 2 + oldPinLen), newPinLen);
	}

	// TODO add admin Authentication
	public void reset(APDU apdu) {
		byte[] defaultPin = { (byte) '1', (byte) '2', (byte) '3', (byte) '4' };
		pin.update(defaultPin, (short) 0, (byte) defaultPin.length);
	}

	public boolean isAuthenticated() {
		return pin.isValidated();
	}
}
