package com.haign.wallet;

import javacard.framework.*;
import javacard.security.*;

class KeyManager {
	private static final short KEY_LENGTH = KeyBuilder.LENGTH_EC_FP_256;
	private static final byte MAX_KEYS = (byte) 10;

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

	private HDKeyDerivation hdTree;

	public KeyManager() {
		hdTree = new HDKeyDerivation();
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
			ISOException.throwIt((short) 0x6F50);
		}
	}

	/**
	 * INS=0x60: Inject master seed and set master key in HDKeyDerivation.
	 * APDU data: [seed(64B)]
	 */
	public void setMasterKey(APDU apdu) {
		lazyInit();
		byte[] buf = apdu.getBuffer();
		short len = apdu.setIncomingAndReceive();
		if (len != 64) {
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		}
		hdTree.setMasterKey(buf, ISO7816.OFFSET_CDATA, (short) 64);
	}


	/**
	 * INS=0x61: Derive child key from stored master and path, then store.
	 * APDU data: [UUID(16)||pathLen(1)||coinType(1)||account(1)||change(1)||address_index(1)]
	 */
	public void generateKeyPair(APDU apdu) {
		lazyInit();
		byte[] buf = apdu.getBuffer();
		short off = ISO7816.OFFSET_CDATA;
		// UUID
		byte[] uuid = new byte[UUID_LENGTH];
		Util.arrayCopyNonAtomic(buf, off, uuid, (short) 0, UUID_LENGTH);
		off += UUID_LENGTH;

		// path length
		int pathLen = buf[off++] & 0xFF;
		// coinType
		byte coinType = buf[off++];
		// account
		byte account = buf[off++];
		// change
		byte change = buf[off++];
		// addressIndex
		short addressIndex = buf[off];

		byte index = getRequestedIndex(uuid, coinType);
		if (index != -1) ISOException.throwIt((short) 0x6F16);

		// find free slot
		byte slot = -1;
		for (byte i = 0; i < MAX_KEYS; i++) {
			if (coinTypeList[i] == 0) {
				slot = i;
				break;
			}
		}

		if (slot < 0) ISOException.throwIt(ISO7816.SW_FILE_FULL);
		// derive child key
		byte[] childPrivateKey = new byte[PRIVATE_KEY_LENGTH];
		byte[] childChainCode = new byte[PRIVATE_KEY_LENGTH];

		try {
			hdTree.deriveBip44FullyHardened(coinType, account, change, addressIndex, childPrivateKey, childChainCode);

			byte[] w = computePublicKey(childPrivateKey);

			// store keys
			Util.arrayCopyNonAtomic(childPrivateKey, (short) 0, persistentPrivate, (short) (slot * PRIVATE_KEY_LENGTH), PRIVATE_KEY_LENGTH);
			Util.arrayCopyNonAtomic(w, (short) 1, persistentPublic, (short) (slot * PUBLIC_KEY_LENGTH), PUBLIC_KEY_LENGTH);
			Util.arrayCopyNonAtomic(uuid, (short) 0, uuidList, (short) (slot * UUID_LENGTH), UUID_LENGTH);
			coinTypeList[slot] = coinType;
		} catch (ISOException e) {
			throw e;
		} catch (CryptoException e) {
			ISOException.throwIt((short) (0x6F10 | (short) (e.getReason() & 0xFF)));
		} catch (ArrayIndexOutOfBoundsException e) {
			ISOException.throwIt((short) 0x6F30);
		} catch (Exception e) {
			ISOException.throwIt((short) 0x6F50);
		}
	}

	// TODO refactor when update sign logic
	public void loadKeyPair() {
//		lazyInit();

//		byte index = getRequestedIndex();
//		privateKey.setS(persistentPrivate, (short) (index * PRIVATE_KEY_LENGTH), PRIVATE_KEY_LENGTH);
//		publicKey.setW(persistentPublic, (short) (index * PUBLIC_KEY_LENGTH), PUBLIC_KEY_LENGTH);
	}


	public void getAllPublicKey(APDU apdu) {
		lazyInit();

		// Calculate total length
		short totalLen = 1;
		short count = 0;
		for (byte i = 0; i < MAX_KEYS; i++) {
			if (coinTypeList[i] == 0) continue;
			count++;
			if (coinTypeList[i] == WalletApplet.COIN_ETH)
				totalLen += UNCOMPRESSED_KEY_LENGTH;
			if (coinTypeList[i] == WalletApplet.COIN_BTC || coinTypeList[i] == WalletApplet.COIN_XRP)
				totalLen += COMPRESSED_KEY_LENGTH;
			// UUID and length bit
			totalLen += UUID_LENGTH + 1;
		}

		// Prepare transient buffer for each uncompressed key
		byte[] outBuf = JCSystem.makeTransientByteArray(UNCOMPRESSED_KEY_LENGTH, JCSystem.CLEAR_ON_DESELECT);
		short outOffset = 0;
		outBuf[outOffset++] = (byte) count;
		// For each stored slot, send uncompressed public key
		for (byte index = 0; index < MAX_KEYS; index++) {
			short coinType = coinTypeList[index];
			if (coinType == 0) continue;
			// Insert length byte
			short keyLen = (coinType == WalletApplet.COIN_ETH) ? UNCOMPRESSED_KEY_LENGTH : COMPRESSED_KEY_LENGTH;
			outBuf[outOffset++] = (byte) (keyLen + UUID_LENGTH);
			// Copy UUID
			Util.arrayCopyNonAtomic(uuidList, (short) (index * UUID_LENGTH), outBuf, outOffset, UUID_LENGTH);
			outOffset += UUID_LENGTH;
			switch (coinType) {
				case WalletApplet.COIN_ETH:
					// Ethereum: Uncompressed public key (65 bytes, starts with 0x04)
					outBuf[outOffset++] = (byte) 0x04;
					Util.arrayCopyNonAtomic(persistentPublic, (short) (index * PUBLIC_KEY_LENGTH), outBuf, outOffset, PUBLIC_KEY_LENGTH);
					outOffset += PUBLIC_KEY_LENGTH;
					break;
				case WalletApplet.COIN_BTC:
				case WalletApplet.COIN_XRP:
					// BTC/XRP: compressed public key (33 bytes)
					compressPublicKey(persistentPublic, (short) (index * PUBLIC_KEY_LENGTH), outBuf, outOffset);
					break;
				default:
					ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
			}
		}
		// Send entire payload at once
		apdu.setOutgoing();
		apdu.setOutgoingLength(totalLen);
		apdu.sendBytesLong(outBuf, (short) 0, totalLen);

	}

	// instruction: [CLA][INS][coin_type][0x00][0x10][UUID]
	public void getPublicKey(APDU apdu) {
		lazyInit();
		byte[] buffer = apdu.getBuffer();
		byte coinType = buffer[ISO7816.OFFSET_P1];
		short dataOffset = ISO7816.OFFSET_CDATA;
		short uuidLen = buffer[ISO7816.OFFSET_LC];
		if (uuidLen < (short) 16)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		byte[] uuid = new byte[uuidLen];
		Util.arrayCopyNonAtomic(buffer, dataOffset, uuid, (short) 0, UUID_LENGTH);
		byte index = getRequestedIndex(uuid, coinType);
		if (index == -1) ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);

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
				Util.arrayCopyNonAtomic(tmp, (short) 0, full, (short) 1, PUBLIC_KEY_LENGTH);
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
		short dataOffset = ISO7816.OFFSET_CDATA;
		short uuidLen = buffer[ISO7816.OFFSET_LC];
		if (uuidLen < (short) 16)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		byte[] uuid = new byte[uuidLen];
		Util.arrayCopyNonAtomic(buffer, dataOffset, uuid, (short) 0, UUID_LENGTH);
		byte index = getRequestedIndex(uuid, coinType);
		if (index == -1) ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);

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

	private byte getRequestedIndex(byte[] uuid, byte coinType) {

		for (byte i = 0; i < MAX_KEYS; i++) {
			if (coinTypeList[i] != coinType) continue;
			boolean match = true;
			short offset = (short) (i * UUID_LENGTH);
			for (byte j = 0; j < UUID_LENGTH; j++) {
				if (uuidList[(short) (offset + j)] != uuid[(short) (j)]) {
					match = false;
					break;
				}
			}
			if (match)
				return i;
		}
		return (byte) -1;  // UUID not found
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

	/**
	 * Converts a raw (uncompressed) public key (X‖Y, 64 bytes) into its compressed form (33 bytes).
	 *
	 * @param uncompressed 64-byte raw public key array, stored as X‖Y
	 * @param inOffset     Offset in the uncompressed array where the public key begins
	 * @param compressed   Buffer to receive the compressed public key (must be at least 33 bytes)
	 * @param outOffset    Offset in the compressed buffer at which to write the data
	 */
	private void compressPublicKey(byte[] uncompressed, short inOffset, byte[] compressed, short outOffset) {
		byte yLastByte = uncompressed[(short) (inOffset + 63)];
		compressed[outOffset] = (byte) ((yLastByte & 0x01) == 0 ? 0x02 : 0x03);
		Util.arrayCopyNonAtomic(uncompressed, inOffset, compressed, (short) (outOffset + 1), PRIVATE_KEY_LENGTH);
	}

	/**
	 * Calculates and returns the secp256k1 public key (uncompressed, 65 bytes) for the given 32-byte private key.
	 *
	 * @param privateKeyBytes a 32-byte private scalar
	 *
	 * @return a 65-byte uncompressed public key array (0x04‖X‖Y)
	 */
	public byte[] computePublicKey(byte[] privateKeyBytes) {
		byte[] G = curve256k1.G;

		// Set private scalar
		privateKey.setS(privateKeyBytes, (short) 0, PRIVATE_KEY_LENGTH);

		// Initialize ECDH KeyAgreement
		KeyAgreement ka = KeyAgreement.getInstance(
				KeyAgreement.ALG_EC_SVDP_DH_PLAIN_XY, false);
		ka.init(privateKey);

		// Perform scalar multiplication on base point G: Q = k·G
		byte[] qW = new byte[UNCOMPRESSED_KEY_LENGTH];
		short qLen = ka.generateSecret(
				G, (short) 0, (short) G.length,  // input: 0x04‖Gx‖Gy
				qW, (short) 0                    // output buffer
		);
		// qLen == POINT_LEN
		qW[64] = (byte) qLen;
		return qW;
	}
}