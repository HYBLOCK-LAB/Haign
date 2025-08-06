package com.haign.wallet;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacardx.apdu.ExtendedLength;

/**
 * Applet class
 *
 * @author jiseop9083
 */

public class WalletApplet extends Applet implements ExtendedLength {

	private static final byte MAX_AID_LENGTH = (byte) 16;

	// APDU instruction codes

	// 0x2x: PIN/Authentication Instructions
	private static final byte INS_VERIFY_PIN = (byte) 0x20;
	private static final byte INS_CHANGE_PIN = (byte) 0x22;
	private static final byte INS_RESET_PIN = (byte) 0x24;

	// 0x3x: Key Management Instructions
	private static final byte INS_GENERATE_KEY = (byte) 0x30;
	private static final byte INS_GET_PUBKEY = (byte) 0x32;
	private static final byte INS_GET_ALL_PUBKEY = (byte) 0x34;
	private static final byte INS_SIGN = (byte) 0x36;

	// 0x4x: Address Retrieval Instructions
	private static final byte INS_GET_ADDRESS = (byte) 0x40;
//	private static final byte INS_GET_LIST_ADDRESSES = (byte) 0x42;

	// 0x5x: Metadata Instructions
	private static final byte INS_GET_EEPROM_FREE = (byte) 0x50;

	// 0x6x: seed
	private static final byte INS_SET_MASTER_SEED = (byte) 0x60;

	// COINS
	static final byte COIN_BTC = (byte) 0x00;
	static final byte COIN_ETH = (byte) 0x3c;
	static final byte COIN_XRP = (byte) 0x90;

	private final PINManager pinManager;
	private final KeyManager keyManager;
	private final SignatureManager signatureManager;
	private final AppManager appManager;

	protected WalletApplet(byte[] bArray, short bOffset, byte bLength) {
		pinManager = new PINManager();
		keyManager = new KeyManager();
		signatureManager = new SignatureManager();
		appManager = new AppManager();
		byte aidLen = bArray[bOffset];
		if (aidLen <= 0 || aidLen > MAX_AID_LENGTH || (short) (bOffset + 1 + aidLen) > (short) bArray.length) {
			ISOException.throwIt(ISO7816.SW_DATA_INVALID);
		}
		register(bArray, ((short) (bOffset + 1)), bArray[bOffset]);
	}

	/**
	 * Installs this applet.
	 *
	 * @param bArray  the array containing installation parameters
	 * @param bOffset the starting offset in bArray
	 * @param bLength the length in bytes of the parameter data in bArray
	 */
	public static void install(byte[] bArray, short bOffset, byte bLength) {
		new WalletApplet(bArray, bOffset, bLength);
	}

	/**
	 * Processes an incoming APDU.
	 *
	 * @param apdu the incoming APDU
	 *
	 * @see APDU
	 */
	public void process(APDU apdu) {
		byte[] buffer = apdu.getBuffer();

		if (selectingApplet())
			return;

		switch (buffer[ISO7816.OFFSET_INS]) {
			case INS_VERIFY_PIN:
				// TODO maintain session in 30 seconds when sign transactions.
				pinManager.verify(apdu);
				break;
			case INS_CHANGE_PIN:
				pinManager.change(apdu);
				break;
			case INS_RESET_PIN:
				pinManager.reset(apdu);
				break;
			case INS_GENERATE_KEY:
				checkAuth();
				keyManager.generateKeyPair(apdu);
				break;
			case INS_GET_PUBKEY:
				checkAuth();
				keyManager.getPublicKey(apdu);
				break;
			case INS_SIGN:
				checkAuth();
				keyManager.loadKeyPair();
				signatureManager.sign(apdu, keyManager.getPrivateKey());
				break;
			case INS_GET_ADDRESS:
				checkAuth();
				keyManager.getAddress(apdu);
				break;
			case INS_GET_ALL_PUBKEY:
				checkAuth();
				keyManager.getAllPublicKey(apdu);
				break;
			case INS_GET_EEPROM_FREE:
				appManager.getFreeEEPROM(apdu);
				break;
			case INS_SET_MASTER_SEED:
				checkAuth();
				keyManager.setMasterKey(apdu);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	private void checkAuth() {
		if (!pinManager.isAuthenticated()) {
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}
	}
}
