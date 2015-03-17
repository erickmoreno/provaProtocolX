package com.scytl.test.erick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.directory.InvalidAttributesException;

public class Decoder {
	
	private static final byte SPACE_CHAR = 0x20;
	
	/**
	 * Decode an passed message according to Protocol X
	 * 
	 * @param buff
	 *            The message to be decoded
	 * @return A decoded message
	 * @throws InvalidAttributesException
	 *             In case of a malformed message
	 */
	public byte[] decodeMessage(byte[] buff) throws InvalidAttributesException {

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
		
		int packetSize = Descriptor.PACKET_SIZE;

		// if the size of the message is incorrect
		if (buff.length % packetSize != 0) {
			throw new InvalidAttributesException(
					"Malformed message received, makes impossible to build correct packages");
		}

		List<byte[]> packets = new ArrayList<byte[]>();

		// the number of packets is defined based on the message size and default packet size
		int numPackages = buff.length / packetSize;

		for (int i = 0; i < numPackages; i++) {
			byte[] packet = new byte[Descriptor.PACKET_DATA_SIZE];
			int init = packetSize * i;
			for (int j = 0; j < Descriptor.PACKET_DATA_SIZE; j++) {
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
				decoded[i] = Descriptor.getDecodedValue(bits5[i]);
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
		byte[] message = new byte[packetRegrouped.size() * Descriptor.SIZE_DECODED_DATA_MESSAGE];

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

}
