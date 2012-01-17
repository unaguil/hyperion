package peer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import peer.message.BroadcastMessage;
import util.WaitableThread;
import util.logger.Logger;

/**
 * This class is used to implement the message receiving thread.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
final class ReceivingThread extends WaitableThread {

	private final BasicPeer peer;

	private final Logger logger = Logger.getLogger(ReceivingThread.class);

	public ReceivingThread(final BasicPeer peer) {
		this.peer = peer;
	}

	@Override
	public void run() {
		// Reception thread main loop
		while (!Thread.interrupted()) {
			byte[] data = null;

			try {
				data = peer.getCommProvider().receiveData();
			} catch (final IOException e) {
				logger.error("Peer " + peer.getPeerID() + " receiving data error. " + e.getMessage());
			}

			try {
				if (data != null) {					
					final byte[] uncompressed = uncompress(data);
					final BroadcastMessage message = (BroadcastMessage) getObject(uncompressed);
					if (peer.getCommProvider().isValid(message)) {
						peer.processReceivedMessage(message);
						logger.trace("Peer " + peer.getPeerID() + " message processed");
					}
				}
			} catch (final IOException e) {
				logger.error("Peer " + peer.getPeerID() + " problem deserializing received data. " + e.getMessage());
			} catch (final ClassNotFoundException e) {
				logger.error("Peer " + peer.getPeerID() + " problem deserializing received data. " + e.getMessage());
			}
		}

		logger.trace("Peer " + peer.getPeerID() + " receiving thread finalized");
		this.threadFinished();
	}

	// Creates an object from its byte array representation
	private Object getObject(final byte[] bytes) throws IOException, ClassNotFoundException {
		final ByteArrayInputStream bios = new ByteArrayInputStream(bytes);
		final ObjectInput in = new ObjectInputStream(bios);
		return in.readObject();
	}
	
	private byte[] uncompress(byte[] data) {
		// Create the decompressor and give it the data to compress
		Inflater decompressor = new Inflater();
		decompressor.setInput(data);

		// Create an expandable byte array to hold the decompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);

		// Decompress the data
		byte[] buf = new byte[1024];
		while (!decompressor.finished()) {
		    try {
		        int count = decompressor.inflate(buf);
		        bos.write(buf, 0, count);
		    } catch (DataFormatException e) {
		    }
		}
		try {
		    bos.close();
		} catch (IOException e) {
		}

		// Get the decompressed data
		return bos.toByteArray();
	}
}
