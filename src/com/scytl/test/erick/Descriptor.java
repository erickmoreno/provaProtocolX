package com.scytl.test.erick;

public class Descriptor {
	
	public static final int SIZE_DECODED_DATA_MESSAGE = 4;
	public static final int PACKET_SIZE = 7;
	public static final int PACKET_DATA_SIZE = 5;
	public static byte[] table1 = new byte[] { 30, 9, 20, 21, 10, 11, 14, 15, 18, 19, 22, 23, 26, 27, 28, 29 };
	public static byte[] table2 = new byte[] { (byte) 0xC6, 0x6b, 0x21 };
	
	public static byte getEndOfMessage() {
		return table2[2];
	}
	
	public static byte getDecodedValue(byte baite) {
		
		byte ret = 0;
		
		for (int i = 0; i < table1.length; i++) {
			if (table1[i] == baite) {
				ret = (byte) i;
			}
		}
		
		return ret;
	}
	
	public static byte getStartMessage() {
		return table2[0];
	}
	
	public static byte getEndPacket() {
		return table2[1];
	}
	
	public static byte encodedValue(byte value) {
		return table1[value];
	}

}
