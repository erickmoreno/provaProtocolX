package com.scytl.test.erick;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.directory.InvalidAttributesException;

public class ProtocollX {

	private static final int SIZE_DECODED_DATA_MESSAGE = 4;
	private static final int PACKET_SIZE = 7;
	private static final int PACKET_DATA_SIZE = 5;
	private static final byte SPACE_CHAR = 0x20;
	private byte[] table1 = new byte[] { 30, 9, 20, 21, 10, 11, 14, 15, 18, 19, 22, 23, 26, 27, 28, 29 };
	private byte[] table2 = new byte[] { (byte)0xC6, 0x6b, 0x21 };

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length < 2) {
				throw new InvalidAttributesException(
						"You must especify a host(ip) and port to comunicate. Ex: 177.71.195.77 50015");
			}
			String ip = args[0];
			Integer port = Integer.valueOf(args[1]);

			new ProtocollX().runProtocollReceiveRespond(ip, port);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidAttributesException e) {
			e.printStackTrace();
		}
	}



	public void runProtocollReceiveRespond(String host, int port) throws UnknownHostException, IOException,
			InvalidAttributesException {
		Socket socket = new Socket(host, port);
		socket.setSoTimeout(2 * 1000);

		try {
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();

			byte[] buff = readServerMessage(in);
			// Decoding received message
			byte[] decodedMessage = decodeMessage(buff);
			// creating the default response
			byte[] response = invert(decodedMessage);

			byte[] encResponse = encodeMessage(response);

			out.write(encResponse);
			
			char[] ack = waitForTheAck(in);
			System.out.println(String.valueOf(ack));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			socket.close();
		}

	}
	
	/**
	 * Reads a message from the passed inputstrem until a EOT char is found.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private byte[] readServerMessage(InputStream in) throws IOException {

		byte[] buff = new byte[7];
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (true) {
			in.read(buff);
			
			baos.write(buff,0,7);

			if (buff[buff.length - 1] == table2[2]) {
				break;
			}
		}

		return baos.toByteArray();
	}

	/**
	 * Method that waits for the answer from the passed input stream
	 * 
	 * @param in
	 * @return String that represents the answer
	 * @throws InvalidAttributesException
	 * @throws IOException
	 */
	private char[] waitForTheAck(InputStream in) throws InvalidAttributesException, IOException {
		byte[] ack = decodeMessage(readServerMessage(in));
		char[] toPrint = new char[ack.length];
		for (int i = 0; i < ack.length; i++) {
			toPrint[i] = (char)ack[i];
		}
		return toPrint;
	}

	/**
	 * Encode an passed message according to protocoll X specification
	 * 
	 * @param message
	 *            Message to encode
	 * @return Encoded message
	 */
	protected byte[] encodeMessage(byte[] message) {

		List<byte[]> packets = separetePackets(fixSize(message));

		List<byte[]> encodedPackets = new ArrayList<byte[]>();

		for (byte[] packet : packets) {
			byte[] encoded = new byte[packet.length * 2];
			for (byte i = 0; i < packet.length; i++) {
				encoded[i * 2] = table1[getFirstNibble(packet[i])];
				encoded[i * 2 + 1] = table1[getSecNibble(packet[i])];
			}
			encodedPackets.add(encoded);
		}

		List<byte[]> packetsBytes = from5to8bits(encodedPackets);

		return mountMessage(packetsBytes);
	}

	/**
	 * 
	 * @param packetsBytes
	 *            A list of packets to concatenate into a message acording
	 *            Protocoll X rules
	 * @return A valid Protocoll X message
	 */
	private byte[] mountMessage(List<byte[]> packetsBytes) {
		byte[] message = new byte[PACKET_SIZE * packetsBytes.size()];

		for (int i = 0; i < packetsBytes.size(); i++) {
			byte[] packet = packetsBytes.get(i);
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

	private List<byte[]> separetePackets(byte[] sizeFixed) {

		List<byte[]> packets = new ArrayList<byte[]>();

		for (int i = 0; i < sizeFixed.length; i++) {
			if ((i + 1) % SIZE_DECODED_DATA_MESSAGE == 0) {
				packets.add(getLastPacket(sizeFixed, i));
			}
		}

		return packets;
	}

	private byte[] getLastPacket(byte[] sizeFixed, int pos) {
		byte[] packet = new byte[SIZE_DECODED_DATA_MESSAGE];
		int j = 0;
		for (int i = pos - (SIZE_DECODED_DATA_MESSAGE -1); i <= pos; i++) {
			packet[j] = sizeFixed[i];
			j++;
		}
		return packet;
	}

	/**
	 * If the message length is not a multiple of 4, fill the message with
	 * spaces
	 * 
	 * @param message
	 * @return
	 */
	private byte[] fixSize(byte[] message) {

		if ((message.length % SIZE_DECODED_DATA_MESSAGE) != 0) {
	
			int size = message.length;
			// The size that the message should have
			int correctSize = SIZE_DECODED_DATA_MESSAGE - (size % SIZE_DECODED_DATA_MESSAGE) + size;
			byte[] fixed = new byte[correctSize];
	
			for (int i = 0; i < correctSize; i++) {
				if (i < size) {
					fixed[i] = message[i];
				} else {
					// Filling with spaces
					fixed[i] = SPACE_CHAR;
				}
			}
			return fixed;
		}
		
		return message;

	}

	/**
	 * Inverts a message
	 * 
	 * @param decodedMessage
	 *            The message do be inverted
	 * @return
	 */
	private byte[] invert(byte[] decodedMessage) {
		byte[] inverted = new byte[decodedMessage.length];
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
	protected byte[] decodeMessage(byte[] buff) throws InvalidAttributesException {

		List<byte[]> packets = from8to5bits(getPackets(buff));
		
		packets = decodePacks(packets);
		packets = regroupPacks(packets);

		byte[] decodedMessage = concatMessage(packets);

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
	private List<byte[]> getPackets(byte[] buff) throws InvalidAttributesException {

		// if the size of the message is incorrect
		if (buff.length % PACKET_SIZE != 0) {
			throw new InvalidAttributesException(
					"Malformed message received, makes impossible to build correct packages");
		}

		List<byte[]> packets = new ArrayList<byte[]>();

		// the number of packets is defined based on the message size and default packet size
		int numPackages = buff.length / PACKET_SIZE;

		for (int i = 0; i < numPackages; i++) {
			byte[] packet = new byte[PACKET_DATA_SIZE];
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
	private List<byte[]> from8to5bits(List<byte[]> packets) {

		List<byte[]> packets5bits = new ArrayList<byte[]>();

		for (byte[] packet : packets) {

			// Lots of magic numbers =/
			// TODO FIGURE OUT THE FORMULA TO THIS TRANSFORMATION
			byte[] packet5 = new byte[8];

			packet5[0] = (byte) ((packet[0] & 0xF8) >> 3);
			packet5[1] = (byte) (((packet[0] & 0x07) << 2) | (packet[1] & 0xC0) >> 6);
			packet5[2] = (byte) ((packet[1] & 0x3E) >> 1);
			packet5[3] = (byte) (((packet[1] & 0x01) << 4) | (packet[2] & 0xF0) >> 4);
			packet5[4] = (byte) (((packet[2] & 0x0F) << 1) | (packet[3] & 0x80) >> 7);
			packet5[5] = (byte) ((packet[3] & 0x7C) >> 2);
			packet5[6] = (byte) (((packet[3] & 0x03) << 3) | (packet[4] & 0xE0) >> 5);
			packet5[7] = (byte) (packet[4] & 0x1F);

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
	private List<byte[]> from5to8bits(List<byte[]> packets) {

		List<byte[]> packets8bits = new ArrayList<byte[]>();

		for (byte[] packet : packets) {

			// Lots of magic numbers =/
			// TODO FIGURE OUT THE FORMULA TO THIS TRANSFORMATION
			byte[] packet8bits = new byte[5];

			packet8bits[0] = (byte) ((packet[0] << 3) | ((packet[1] & 0x1C) >> 2));
			packet8bits[1] = (byte) ((packet[1] << 6) | (packet[2] << 1) | ((packet[3] & 0x10) >> 4));
			packet8bits[2] = (byte) (((packet[3] & 0x0F) << 4) | ((packet[4] & 0X1E) >> 1));
			packet8bits[3] = (byte) (((packet[4] & 0x01) << 7) | (packet[5] << 2) | ((packet[6] & 0x18) >> 3));
			packet8bits[4] = (byte) (((packet[6] & 0x07) << 5) | packet[7]);

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
	private List<byte[]> decodePacks(List<byte[]> packet5bits) {

		List<byte[]> decodedPacks = new ArrayList<byte[]>();

		for (byte[] bits5 : packet5bits) {

			byte[] decoded = new byte[bits5.length];
			for (int i = 0; i < bits5.length; i++) {
				decoded[i] = getDecodedValue(bits5[i]);
			}
			decodedPacks.add(decoded);
		}
		return decodedPacks;
	}
	
	private byte getDecodedValue(byte baite) {
		
		byte ret = 0;
		
		for (int i = 0; i < table1.length; i++) {
			if (table1[i] == baite) {
				ret = (byte) i;
			}
		}
		
		return ret;
	}

	/**
	 * Lazy method to trim a message
	 * 
	 * @param message
	 *            the message to me trimmed
	 * @return A message without unnecessary spaces
	 */
	private byte[] trim(byte[] message) {
		
		int i = message.length - 1;
		int newLenght = message.length;
		
		while (message[i] == SPACE_CHAR) {
			newLenght--;
			i--;
		}
		
		return Arrays.copyOf(message, newLenght);
	}

	/**
	 * Converts a list of packets into a message. Has a semantic similar to a
	 * concatenation
	 * 
	 * @param packetRegrouped
	 * @return
	 */
	private byte[] concatMessage(List<byte[]> packetRegrouped) {
		byte[] message = new byte[packetRegrouped.size() * SIZE_DECODED_DATA_MESSAGE];

		for (int i = 0; i < packetRegrouped.size(); i++) {
			for (int j = 0; j < packetRegrouped.get(i).length; j++) {
				message[i * packetRegrouped.get(i).length + j] = packetRegrouped.get(i)[j];
			}
		}

		return message;
	}

	/**
	 * Regroup message from 4bits decoded pieces to 8bits
	 * 
	 * @param packetDecoded
	 * @return
	 */
	private List<byte[]> regroupPacks(List<byte[]> packetDecoded) {

		List<byte[]> regrouped = new ArrayList<byte[]>();

		for (byte[] decoded : packetDecoded) {
			byte[] grouped = new byte[decoded.length / 2];

			for (int i = 0; i < decoded.length; i = i + 2) {
				grouped[i / 2] = (byte) (decoded[i] << 4 | decoded[i + 1]);
			}
			regrouped.add(grouped);
		}
		return regrouped;
	}

	private byte getFirstNibble(int baite) {
		return (byte) ((baite & 0xFF) >> 4);
	}

	private byte getSecNibble(int baite) {
		return (byte) (baite & 0x0F);
	}
	
	private void printBuffer(byte[] buff) {

		System.out.print(" - ");
		for (byte baite : buff) {
			System.out.print(String.format("%02X ", baite & 0xFF) + " ");
		}
		System.out.println();
	}
	
	private void printBufferBin(byte[] buff, String n) {

		System.out.print(" - ");
		for (int baite : buff) {
			System.out.print(String.format("%" + n + "s", Integer.toBinaryString(baite & 0xFF)).replace(' ', '0')
					+ " ");
		}
		System.out.println();
	}
	
	private void printPackets(List<byte[]> packets, String n) {
		for (byte[] cs : packets) {
			printBufferBin(cs, n);
		}
	}
	
	private void printPacketsHex(List<byte[]> packets, String n) {
		for (byte[] cs : packets) {
			printBuffer(cs);
		}
	}

}
