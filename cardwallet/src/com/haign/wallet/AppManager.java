package com.haign.wallet;

import javacard.framework.APDU;
import javacard.framework.JCSystem;

public class AppManager {

	public AppManager() {
	}

	// instruction: [80][50][00][00][00]
	public void getFreeEEPROM(APDU apdu) {
		byte[] buffer = apdu.getBuffer();

		short[] memInfo = new short[2];
		JCSystem.getAvailableMemory(memInfo, (short) 0, JCSystem.MEMORY_TYPE_PERSISTENT);

		buffer[0] = (byte) ((memInfo[0] >> 8) & 0xFF);
		buffer[1] = (byte) (memInfo[0] & 0xFF);
		buffer[2] = (byte) ((memInfo[1] >> 8) & 0xFF);
		buffer[3] = (byte) (memInfo[1] & 0xFF);

		apdu.setOutgoing();
		apdu.setOutgoingLength((short) 4);
		apdu.sendBytes((short) 0, (short) 4);
	}
}
