package com.haign.wallet;

import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Util;
import javacard.security.*;

class KeyManager {
	private static final short KEY_LENGTH = KeyBuilder.LENGTH_EC_FP_256;
	private static final byte MAX_KEYS = (byte) 5;

	// 32 bytes: scalar value (secp256k1 private key)
	private static final short PRIVATE_KEY_LENGTH = (short) 32;
	// 33 bytes: [0x02 or 0x03] + X (compressed public key)
	private static final short COMPRESSED_KEY_LENGTH = (short) 33;
	// 64 bytes: X(32) + Y(32) (secp256k1 public key)
	private static final short PUBLIC_KEY_LENGTH = (short) 64;
	// 65 bytes: [0x04] + X(32) + Y(32) (uncompressed public key)
	private static final short UNCOMPRESSED_KEY_LENGTH = (short) 65;
	private static final short UUID_LENGTH = (short) 16;


	// EEPROM storage (flattened)
	private final byte[] persistentPrivate = new byte[MAX_KEYS * PRIVATE_KEY_LENGTH];
	private final byte[] persistentPublic = new byte[MAX_KEYS * PUBLIC_KEY_LENGTH];
	private final byte[] uuidList = new byte[MAX_KEYS * UUID_LENGTH];
	private final byte[] coinTypeList = new byte[MAX_KEYS];

	private ECPrivateKey privateKey;
	private ECPublicKey publicKey;
	private final SECP256k1 curve256k1 = new SECP256k1();

	private boolean initialized = false;

	public KeyManager() {
	}

	public void lazyInit() {
		if (initialized) return;

		try {
			privateKey = (ECPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PRIVATE, KEY_LENGTH, false);
			publicKey = (ECPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PUBLIC, KEY_LENGTH, false);
			setCurveParameters();
			initialized = true;
		} catch (CryptoException e) {
			ISOException.throwIt((short) (0x6F10 | (short) (e.getReason() & 0xFF)));
		} catch (Exception e) {
			ISOException.throwIt((short) 0x6F20);
		}
	}

	public void generateKeyPair(APDU apdu) {
		lazyInit();

		byte[] buffer = apdu.getBuffer();
		byte coinType = buffer[ISO7816.OFFSET_P1];
		short dataOffset = ISO7816.OFFSET_CDATA;
		short uuidLen = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

		if (uuidLen != UUID_LENGTH) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		byte freeIndex = -1;
		for (byte i = 0; i < MAX_KEYS; i++) {
			boolean empty = (coinTypeList[i] == 0);
			short offset = (short) (i * UUID_LENGTH);
			for (byte j = 0; j < UUID_LENGTH; j++) {
				if (uuidList[(short) (offset + j)] != 0) {
					empty = false;
					break;
				}
			}
			if (empty) {
				freeIndex = i;
				break;
			}
		}

		if (freeIndex == -1) {
			ISOException.throwIt(ISO7816.SW_FILE_FULL);
		}


		try {
			KeyPair kp = new KeyPair(publicKey, privateKey);
			kp.genKeyPair();
			privateKey.getS(persistentPrivate, (short) (freeIndex * PRIVATE_KEY_LENGTH));
			publicKey.getW(persistentPublic, (short) (freeIndex * PUBLIC_KEY_LENGTH));
			Util.arrayCopyNonAtomic(buffer, dataOffset, uuidList, (short) (freeIndex * UUID_LENGTH), UUID_LENGTH);
			coinTypeList[freeIndex] = coinType;
		} catch (CryptoException e) {
			ISOException.throwIt((short) (0x6F10 | (short) (e.getReason() & 0xFF)));
		} catch (ArrayIndexOutOfBoundsException e) {
			ISOException.throwIt((short) 0x6F30);
		} catch (Exception e) {
			ISOException.throwIt((short) 0x6F20);
		}
	}

	public void loadKeyPair() {
		lazyInit();
		byte index = getRequestedIndex();
		privateKey.setS(persistentPrivate, (short) (index * PRIVATE_KEY_LENGTH), PRIVATE_KEY_LENGTH);
		publicKey.setW(persistentPublic, (short) (index * PUBLIC_KEY_LENGTH), PUBLIC_KEY_LENGTH);
	}

	// instruction: [CLA][INS][coin_type][0x00][0x10][UUID]
	public void sendPublicKey(APDU apdu) {
		lazyInit();
		byte[] buffer = apdu.getBuffer();
		byte coinType = buffer[ISO7816.OFFSET_P1];
		byte index = getRequestedIndex();

		apdu.setOutgoing();
		switch (coinType) {
			case WalletApplet.COIN_ETH:
				// Ethereum: Uncompressed public key (65 bytes, starts with 0x04)
				byte[] tmp = new byte[PUBLIC_KEY_LENGTH];
				byte[] full = new byte[UNCOMPRESSED_KEY_LENGTH];
				Util.arrayCopyNonAtomic(persistentPublic,
						(short) (index * PUBLIC_KEY_LENGTH),
						tmp, (short) 0,
						PUBLIC_KEY_LENGTH);
				full[0] = 0x04;
				Util.arrayCopyNonAtomic(tmp, (short) 0,
						full, (short) 1,
						PUBLIC_KEY_LENGTH);
				apdu.setOutgoingLength((short) full.length);
				apdu.sendBytesLong(full, (short) 0, (short) full.length);
				break;
			case WalletApplet.COIN_BTC:
			case WalletApplet.COIN_XRP:
				// BTC/XRP: compressed public key (33 bytes)
				byte[] compressed = new byte[COMPRESSED_KEY_LENGTH];
				compressPublicKey(persistentPublic, (short) (index * PUBLIC_KEY_LENGTH), compressed, (short) 0);
				apdu.setOutgoingLength((short) compressed.length);
				apdu.sendBytesLong(compressed, (short) 0, (short) compressed.length);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}
	}

	// instruction: [CLA][INS][coin_type][0x00][0x10][UUID]
	public void getAddress(APDU apdu) {
		lazyInit();
		byte[] buffer = apdu.getBuffer();

		byte coinType = buffer[ISO7816.OFFSET_P1];

		byte index = getRequestedIndex();
		byte[] pubKeyHash = new byte[PRIVATE_KEY_LENGTH];

		switch (coinType) {
			case WalletApplet.COIN_BTC:
				byte[] compressedBTC = new byte[PRIVATE_KEY_LENGTH];
				compressPublicKey(persistentPublic, (short) (index * PUBLIC_KEY_LENGTH), compressedBTC, (short) 0);
				MessageDigest sha256 = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
				MessageDigest ripemd160 = MessageDigest.getInstance(MessageDigest.ALG_RIPEMD160, false);
				sha256.doFinal(compressedBTC, (short) 0, COMPRESSED_KEY_LENGTH, pubKeyHash, (short) 0);
				ripemd160.doFinal(pubKeyHash, (short) 0, PRIVATE_KEY_LENGTH, pubKeyHash, (short) 0);
				break;
			case WalletApplet.COIN_ETH:
				// Ethereum use ALG_KECCAK_256 when calculate address hash,
				// but java card does not sopport ALG_KECCAK_256
				// implement ALG_KECCAK_256
				Keccak256 keccak = new Keccak256();
				keccak.reset();
				keccak.digest(persistentPublic, (short) (index * PUBLIC_KEY_LENGTH), PUBLIC_KEY_LENGTH, pubKeyHash,
						(short) 0);

//			MessageDigest keccak = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false); // placeholder
//			keccak.doFinal(persistentPublic, (short) (index * UNCOMPRESSED_KEY_LENGTH + 1), (short) 64, pubKeyHash,
//					(short) 0);
//			Util.arrayCopyNonAtomic(pubKeyHash, (short) 12, pubKeyHash, (short) 0, (short) 20);

				break;
			case WalletApplet.COIN_XRP:
				byte[] compressedXRP = new byte[COMPRESSED_KEY_LENGTH];
				compressPublicKey(persistentPublic, (short) (index * PUBLIC_KEY_LENGTH), compressedXRP, (short) 0);
				MessageDigest sha256_2 = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
				MessageDigest ripemd160_2 = MessageDigest.getInstance(MessageDigest.ALG_RIPEMD160, false);
				sha256_2.doFinal(compressedXRP, (short) 0, COMPRESSED_KEY_LENGTH, pubKeyHash, (short) 0);
				ripemd160_2.doFinal(pubKeyHash, (short) 0, PRIVATE_KEY_LENGTH, pubKeyHash, (short) 0);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
		}

		apdu.setOutgoing();
		apdu.setOutgoingLength((short) 20);
		apdu.sendBytesLong(pubKeyHash, (short) 0, (short) 20); // send 20-byte address
	}

	public PrivateKey getPrivateKey() {
		lazyInit();
		return privateKey;
	}

	private byte getRequestedIndex() {
		byte[] buffer = APDU.getCurrentAPDU().getBuffer();
		byte coinType = buffer[ISO7816.OFFSET_P1];
		short dataOffset = ISO7816.OFFSET_CDATA;
		short uuidLen = buffer[ISO7816.OFFSET_LC];

		if (uuidLen < (short) 16) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}

		for (byte i = 0; i < MAX_KEYS; i++) {
			if (coinTypeList[i] != coinType) continue;
			boolean match = true;
			short offset = (short) (i * UUID_LENGTH);
			for (byte j = 0; j < UUID_LENGTH; j++) {
				if (uuidList[(short) (offset + j)] != buffer[(short) (dataOffset + j)]) {
					match = false;
					break;
				}
			}
			if (match)
				return i;
		}
		ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND); // UUID not found
		return -1;
	}

	private void setCurveParameters() {
		// Set secp256k1 curve manually
		// These values must be updated with actual SEC256k1 parameters
		byte[] p = curve256k1.p;
		byte[] a = curve256k1.a;
		byte[] b = curve256k1.b;
		byte[] G = curve256k1.G;
		byte[] r = curve256k1.r;

		privateKey.setFieldFP(p, (short) 0, (short) p.length);
		privateKey.setA(a, (short) 0, (short) a.length);
		privateKey.setB(b, (short) 0, (short) b.length);
		privateKey.setG(G, (short) 0, (short) G.length);
		privateKey.setR(r, (short) 0, (short) r.length);
		privateKey.setK((short) 1);

		publicKey.setFieldFP(p, (short) 0, (short) p.length);
		publicKey.setA(a, (short) 0, (short) a.length);
		publicKey.setB(b, (short) 0, (short) b.length);
		publicKey.setG(G, (short) 0, (short) G.length);
		publicKey.setR(r, (short) 0, (short) r.length);
		publicKey.setK((short) 1);
	}

	private void compressPublicKey(byte[] uncompressed, short inOffset, byte[] compressed, short outOffset) {
		short xOffset = (short) (inOffset + 1); // skip prefix 0x04
		byte yLastByte = uncompressed[(short) (inOffset + 64)];
		compressed[outOffset] = (byte) ((yLastByte & 1) == 0 ? 0x02 : 0x03);
		Util.arrayCopyNonAtomic(uncompressed, xOffset, compressed, (short) (outOffset + 1), PRIVATE_KEY_LENGTH);
	}
}