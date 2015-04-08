package com.scytl.test.erick;

import static org.junit.Assert.*;

import java.util.List;

import javax.naming.directory.InvalidAttributesException;

import org.junit.Test;

public class TestEncode {

	@Test
	public void testEncode1() throws InvalidAttributesException {
		ProtocollX prot = new ProtocollX();
		
		char[] oakBsb = new char[] { 0xC6, 0x57, 0x54, 0x95, 0x5E, 0x9E, 0x6B, 0xC6, 0x55, 0x17, 0x55, 0x52, 0x9E, 0x21 };
		char[] ok = new char[] { 0xC6, 0x57, 0x55, 0x7A, 0x7A, 0x9E, 0x21 };
		char[] err = new char[] { 0xC6, 0x52, 0xD7, 0x45, 0xD2, 0x9E, 0x21 };
		
		int[] table1 = new int[] { 30, 9, 20, 21, 10, 11, 14, 15, 18, 19, 22, 23, 26, 27, 28, 29 };
		int[] table2 = new int[] { 0xC6, 0x6b, 0x21 };
		
		char[] table1c = new char[] { 30, 9, 20, 21, 10, 11, 14, 15, 18, 19, 22, 23, 26, 27, 28, 29 };
		char[] table2c = new char[] { 0xC6, 0x6b, 0x21 };
		
		//char[] encode = prot.encodeMessage(message);
//		char[] str = prot.decodeMessage(oakBsb);
//		System.out.println(String.valueOf(str));
		
		//char[] oalLocal = prot.encodeMessage("OAK BSB".toCharArray());
		//prot.printBuffer(prot.encodeMessage("OAK BSB".toCharArray()));
		//char[] str2 = prot.decodeMessage(oalLocal);
		//System.out.println(String.valueOf(str));
		
		//prot.printBuffer(prot.encodeMessage(str));
		//System.out.println(String.valueOf(str));
		
		
		
	}
	
	private void printBuffer(int[] buff) {

		System.out.print(" - ");
		for (int baite : buff) {
			System.out.print(String.format("%02X ", baite & 0xFF) + " ");
		}
		System.out.println();
	}

	private void printBufferBin(int[] buff, String n) {

		System.out.print(" - ");
		for (int baite : buff) {
			System.out.print(String.format("%" + n + "s", Integer.toBinaryString(baite & 0xFF)).replace(' ', '0')
					+ "  ");
		}
		System.out.println();
	}

	private void printBufferNibbles(int[] buff) {
		System.out.print(" - ");
		for (int baite : buff) {
			int first = getFirstNibble(baite);
			int sec = getSecNibble(baite);
			System.out.print(String.format("%4s", Integer.toBinaryString(first)).replace(' ', '0') + " ");
			System.out.print(String.format("%4s", Integer.toBinaryString(sec)).replace(' ', '0') + " ");
		}
		System.out.println();
	}
	
	private void printPackets(List<int[]> packets, String n) {
		for (int[] cs : packets) {
			printBufferBin(cs, n);
		}
	}
	
	private int getFirstNibble(int baite) {
		return (baite & 0xFF) >> 4;
	}

	private int getSecNibble(int baite) {
		return baite & 0x0F;
	}

}
