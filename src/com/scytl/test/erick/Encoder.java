package com.scytl.test.erick;

import java.util.ArrayList;
import java.util.List;

public class Encoder {
	
	private static final byte SPACE_CHAR = 0x20;

	/**
	 * Encode an passed message according to protocoll X specification
	 * 
	 * @param message
	 *            Message to encode
	 * @return Encoded message
	 */
	public byte[] encodeMessage(byte[] message) {

		List<byte[]> packets = separetePackets(fixSize(message));

		List<byte[]> encodedPackets = new ArrayList<byte[]>();

		for (byte[] packet : packets) {
			byte[] encoded = new byte[packet.length * 2];
			for (byte i = 0; i < packet.length; i++) {
				encoded[i * 2] = Descriptor.encodedValue(getFirstNibble(packet[i]));
				encoded[i * 2 + 1] = Descriptor.encodedValue(getSecNibble(packet[i]));
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
		
		int packetSize = Descriptor.PACKET_SIZE;
		
		byte[] message = new byte[packetSize * packetsBytes.size()];

		for (int i = 0; i < packetsBytes.size(); i++) {
			byte[] packet = packetsBytes.get(i);
			int firstPos = i * packetSize;

			message[firstPos] = Descriptor.getStartMessage();

			for (int j = 0; j < packet.length; j++) {
				message[firstPos + j + 1] = packet[j];
			}
			message[firstPos + packetSize - 1] = Descriptor.getEndPacket();

		}

		message[message.length - 1] = Descriptor.getEndOfMessage();

		return message;
	}

	private List<byte[]> separetePackets(byte[] sizeFixed) {
		
		List<byte[]> packets = new ArrayList<byte[]>();

		for (int i = 0; i < sizeFixed.length; i++) {
			if ((i + 1) % Descriptor.SIZE_DECODED_DATA_MESSAGE == 0) {
				packets.add(getLastPacket(sizeFixed, i));
			}
		}

		return packets;
	}

	private byte[] getLastPacket(byte[] sizeFixed, int pos) {
		byte[] packet = new byte[Descriptor.SIZE_DECODED_DATA_MESSAGE];
		int j = 0;
		for (int i = pos - (Descriptor.SIZE_DECODED_DATA_MESSAGE - 1); i <= pos; i++) {
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
		
		int sizeDecoded = Descriptor.SIZE_DECODED_DATA_MESSAGE;

		if ((message.length % sizeDecoded) != 0) {

			int size = message.length;
			// The size that the message should have
			int correctSize = sizeDecoded - (size % sizeDecoded) + size;
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

	private byte getFirstNibble(int baite) {
		return (byte) ((baite & 0xFF) >> 4);
	}

	private byte getSecNibble(int baite) {
		return (byte) (baite & 0x0F);
	}

}
