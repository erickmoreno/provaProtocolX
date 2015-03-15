package com.scytl.test.erick;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.InvalidAttributesException;

public class ProtocollX {

	private static final int PACKET_SIZE = 7;
	private static final int PACKET_DATA_SIZE = 5;
	private int[] table1 = new int[] { 30, 9, 20, 21, 10, 11, 14, 15, 18, 19, 22, 23, 26, 27, 28, 29 };
	private int[] table2 = new int[] { 0xC6, 0x6b, 0x21 };

	private Map<Integer, Integer> invertTable1 = initializeInverseTable();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length < 2) {
				throw new InvalidAttributesException("You must especify a host(ip) and port to comunicate. Ex: 177.71.195.77 50015");
			}
			String ip = args[0];
			Integer port = Integer.valueOf(args[1]);
			System.out.println("Running protocol X test");
			new ProtocollX().runProtocollReceiveRespond(ip, port);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidAttributesException e) {
			e.printStackTrace();
		}
	}

	private Map<Integer, Integer> initializeInverseTable() {
		Map<Integer, Integer> invTable = new HashMap<Integer, Integer>(15);
		invTable.put(30, 0);
		invTable.put(9, 1);
		invTable.put(20, 2);
		invTable.put(21, 3);
		invTable.put(10, 4);
		invTable.put(11, 5);
		invTable.put(14, 6);
		invTable.put(15, 7);
		invTable.put(18, 8);
		invTable.put(19, 9);
		invTable.put(22, 10);
		invTable.put(23, 11);
		invTable.put(26, 12);
		invTable.put(27, 13);
		invTable.put(28, 14);
		invTable.put(29, 15);

		return invTable;
	}

	private void runProtocollReceiveRespond(String host, int port) throws UnknownHostException, IOException,
			InvalidAttributesException {
		//Socket socket = new Socket(host, port);

		//BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		//PrintStream out = new PrintStream(socket.getOutputStream(), true);

		//char[] buff = readServerMessage(in);
		// char[] buff = new char[] { 0xC6, 0x57, 0x54, 0x95, 0x5E, 0x9E, 0x6B, 0xC6, 0x55, 0x17, 0x55, 0x52, 0x9E, 0x21 };
		//char[] buff = new char[] { 0xC6, 0x57, 0x55, 0x7A, 0x7A, 0x9E, 0x21 };
		char[] buff = new char[] { 0xC6, 0x52, 0xD7, 0x45, 0xD2, 0x9E, 0x21 };
		// Decoding received message
		char[] decodedMessage = decodeMessage(buff);
		System.out.println(String.valueOf(decodedMessage));
		char[] response = invert(decodedMessage);

		int[] encResponse = encodeMessage(response);
		
		//out.print(encResponse);
		
		//String ack = waitForTheAck(in);
		//System.out.println(ack);
		
		//socket.close();

	}

	/**
	 * Method that waits for the answer from the passed input stream 
	 * @param in
	 * @return String that represents the answer
	 * @throws InvalidAttributesException
	 * @throws IOException
	 */
	private String waitForTheAck(BufferedReader in) throws InvalidAttributesException, IOException {
		return String.valueOf(decodeMessage(readServerMessage(in)));
	}

	/**
	 * Encode an passed message according to protocoll X specification
	 * 
	 * @param message
	 *            Message to encode
	 * @return Encoded message
	 */
	private int[] encodeMessage(char[] message) {

		List<char[]> packets = separetePackets(fixSize(message));

		List<int[]> encodedPackets = new ArrayList<int[]>();

		for (char[] packet : packets) {
			int[] encoded = new int[packet.length * 2];
			for (int i = 0; i < packet.length; i++) {
				encoded[i * 2] = table1[getFirstNibble(packet[i])];
				encoded[i * 2 + 1] = table1[getSecNibble(packet[i])];
			}
			encodedPackets.add(encoded);
		}

		List<int[]> packetsBytes = from5to8bits(encodedPackets);

		return mountMessage(packetsBytes);
	}

	/**
	 * 
	 * @param packetsBytes
	 *            A list of packets to concatenate into a message acording
	 *            Protocoll X rules
	 * @return A valid Protocoll X message
	 */
	private int[] mountMessage(List<int[]> packetsBytes) {
		int[] message = new int[(2 + 5) * packetsBytes.size()];

		for (int i = 0; i < packetsBytes.size(); i++) {
			int[] packet = packetsBytes.get(i);
			int firstPos = i * PACKET_SIZE;

			message[firstPos] = table2[0];

			for (int j = 0; j < packet.length; j++) {
				message[firstPos + j + 1] = packet[j];
			}
			message[firstPos + PACKET_SIZE - 1] = table2[1];

		}

		message[message.length - 1] = table2[2];

		return message;
	}

	private List<char[]> separetePackets(char[] sizeFixed) {

		List<char[]> packets = new ArrayList<char[]>();

		for (int i = 0; i < sizeFixed.length; i++) {
			if ((i + 1) % 4 == 0) {
				packets.add(getLastFourChars(sizeFixed, i));
			}
		}

		return packets;
	}

	private char[] getLastFourChars(char[] sizeFixed, int pos) {
		char[] packet = new char[4];
		int j = 0;
		for (int i = pos - 3; i <= pos; i++) {
			packet[j] = sizeFixed[i];
			j++;
		}
		return packet;
	}

	private char[] fixSize(char[] message) {

		int size = message.length;
		int correctSize = 4 - (size % 4) + size;
		char[] fixed = new char[correctSize];

		for (int i = 0; i < correctSize; i++) {
			if (i < size) {
				fixed[i] = message[i];
			} else {
				fixed[i] = 0x20;
			}
		}

		return fixed;
	}

	private char[] invert(char[] decodedMessage) {
		char[] inverted = new char[decodedMessage.length];
		for (int i = decodedMessage.length; i > 0; i--) {
			inverted[decodedMessage.length - i] = decodedMessage[i - 1];
		}
		return inverted;
	}

	/**
	 * Decode an passed message according to Protocol X
	 * 
	 * @param buff
	 *            The message to be decoded
	 * @return A decoded message
	 * @throws InvalidAttributesException
	 *             In case of a malformed message
	 */
	private char[] decodeMessage(char[] buff) throws InvalidAttributesException {

		List<int[]> packets = from8to5bits(getPackets(buff));
		packets = decodePacks(packets);
		packets = regroupPacks(packets);

		char[] decodedMessage = convertMessage(packets);

		return trim(decodedMessage);
	}

	/**
	 * Convert an protocol X message in a list of packets
	 * 
	 * @param buff
	 *            The message to be converted into packets
	 * @return a list of packets
	 * @throws InvalidAttributesException
	 */
	private List<int[]> getPackets(char[] buff) throws InvalidAttributesException {

		// if the size of the message is incorrect
		if (buff.length % PACKET_SIZE != 0) {
			throw new InvalidAttributesException(
					"Malformed message received, makes impossible to build correct packages");
		}

		List<int[]> packets = new ArrayList<int[]>();

		// the number of packets is defined based on the message size and
		// default packet size
		int numPackages = buff.length / PACKET_SIZE;

		for (int i = 0; i < numPackages; i++) {
			int[] packet = new int[PACKET_DATA_SIZE];
			int init = PACKET_SIZE * i;
			for (int j = 0; j < PACKET_DATA_SIZE; j++) {
				packet[j] = buff[init + j + 1];
			}
			packets.add(packet);
		}

		return packets;
	}

	/**
	 * Regroup the packets from 8bits value to 5 bits
	 * 
	 * @param packets
	 *            List of packets with 8bits to regroup into 5bits
	 * @return List of packets regrouped
	 */
	private List<int[]> from8to5bits(List<int[]> packets) {

		List<int[]> packets5bits = new ArrayList<int[]>();

		for (int[] packet : packets) {

			// TODO FIGURE OUT THE FORMULA TO THIS TRANSFORMATION
			int[] packet5 = new int[8];

			packet5[0] = (packet[0] & 0xF8) >> 3;
			packet5[1] = ((packet[0] & 0x07) << 2) | (packet[1] & 0xC0) >> 6;
			packet5[2] = (packet[1] & 0x3E) >> 1;
			packet5[3] = ((packet[1] & 0x01) << 4) | (packet[2] & 0xF0) >> 4;
			packet5[4] = ((packet[2] & 0x0F) << 1) | (packet[3] & 0x80) >> 7;
			packet5[5] = (packet[3] & 0x7C) >> 2;
			packet5[6] = ((packet[3] & 0x03) << 3) | (packet[4] & 0xE0) >> 5;
			packet5[7] = packet[4] & 0x1F;

			packets5bits.add(packet5);
		}

		return packets5bits;
	}

	/**
	 * Regroup the packets from 5 bits value to 8 bits
	 * 
	 * @param packets
	 *            List of packets with 5bits to regroup into 8bits
	 * @return List of packets regrouped
	 */
	private List<int[]> from5to8bits(List<int[]> packets) {

		List<int[]> packets8bits = new ArrayList<int[]>();

		for (int[] packet : packets) {

			// TODO FIGURE OUT THE FORMULA TO THIS TRANSFORMATION
			int[] packet8bits = new int[5];

			packet8bits[0] = (packet[0] << 3) | ((packet[1] & 0x1C) >> 2);
			packet8bits[1] = (packet[1] << 6) | (packet[2] << 1) | ((packet[3] & 0x10) >> 4);
			packet8bits[2] = ((packet[3] & 0x0F) << 4) | ((packet[4] & 0X1E) >> 1);
			packet8bits[3] = ((packet[4] & 0x10) << 7) | (packet[5] << 2) | ((packet[6] & 0x18) >> 3);
			packet8bits[4] = ((packet[6] & 0x07) << 5) | packet[7];

			packets8bits.add(packet8bits);
		}

		return packets8bits;
	}

	/**
	 * Method that decode a list of packets according to the values specified by
	 * protocol X and saved at invertTable1.
	 * 
	 * @param packet5bits
	 * @return
	 */
	private List<int[]> decodePacks(List<int[]> packet5bits) {

		List<int[]> decodedPacks = new ArrayList<int[]>();

		for (int[] bits5 : packet5bits) {

			int[] decoded = new int[bits5.length];
			for (int i = 0; i < bits5.length; i++) {
				decoded[i] = invertTable1.get(bits5[i]);
			}
			decodedPacks.add(decoded);
		}
		return decodedPacks;
	}

	/**
	 * Lazy method to trim a message
	 * 
	 * @param message
	 *            the message to me trimmed
	 * @return A message without unnecessary spaces
	 */
	private char[] trim(char[] message) {
		return String.valueOf(message).trim().toCharArray();
	}

	private char[] convertMessage(List<int[]> packetRegrouped) {
		char[] message = new char[packetRegrouped.size() * 4];

		for (int i = 0; i < packetRegrouped.size(); i++) {
			for (int j = 0; j < packetRegrouped.get(i).length; j++) {
				// TODO Should take care about encoding with bigger numbers
				message[i * packetRegrouped.get(i).length + j] = (char) packetRegrouped.get(i)[j];
			}
		}

		return message;
	}

	private List<int[]> regroupPacks(List<int[]> packetDecoded) {

		List<int[]> regrouped = new ArrayList<int[]>();

		for (int[] decoded : packetDecoded) {
			int[] grouped = new int[decoded.length / 2];

			for (int i = 0; i < decoded.length; i = i + 2) {
				grouped[i / 2] = decoded[i] << 4 | decoded[i + 1];
			}
			regrouped.add(grouped);
		}
		return regrouped;
	}

	private int getFirstNibble(int baite) {
		return (baite & 0xFF) >> 4;
	}

	private int getSecNibble(int baite) {
		return baite & 0x0F;
	}

	private void printPackets(List<int[]> packets, String n) {
		for (int[] cs : packets) {
			printBufferBin(cs, n);
		}
	}

	private char[] readServerMessage(BufferedReader in) throws IOException {
		StringBuffer line = new StringBuffer();

		while (true) {
			int value = in.read();
			line.append((int) value);

			if (value == table2[2]) {
				break;
			}
		}

		return line.toString().toCharArray();
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
}
