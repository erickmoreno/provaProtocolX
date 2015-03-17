package com.scytl.test.erick;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;

import javax.naming.directory.InvalidAttributesException;

public class ProtocollX {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length < 2) {
				throw new InvalidParameterException(
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

			Decoder decoder = new Decoder();
			Encoder encoder = new Encoder();

			byte[] buff = readServerMessage(in);
			// Decoding received message
			byte[] decodedMessage = decoder.decodeMessage(buff);
			// creating the default response
			byte[] response = invert(decodedMessage);

			byte[] encResponse = encoder.encodeMessage(response);

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
		
		byte eom = Descriptor.getEndOfMessage();

		byte[] buff = new byte[7];

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (true) {
			in.read(buff);

			baos.write(buff, 0, 7);

			if (buff[buff.length - 1] == eom) {
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
		byte[] ack = new Decoder().decodeMessage(readServerMessage(in));
		char[] toPrint = new char[ack.length];
		for (int i = 0; i < ack.length; i++) {
			toPrint[i] = (char) ack[i];
		}
		return toPrint;
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

}
