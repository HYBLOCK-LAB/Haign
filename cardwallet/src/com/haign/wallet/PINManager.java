package com.haign.wallet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.OwnerPIN;

class PINManager {
	private static final byte PIN_TRY_LIMIT = (byte) 5;
	private static final byte MAX_PIN_SIZE = (byte) 8;
	private final OwnerPIN pin;

	public PINManager() {
		pin = new OwnerPIN(PIN_TRY_LIMIT, MAX_PIN_SIZE);
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

	public boolean isAuthenticated() {
		return pin.isValidated();
	}
}
