package peer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import org.apache.log4j.Logger;

import peer.message.BroadcastMessage;
import util.WaitableThread;

/**
 * This class is used to implement the message receiving thread.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
class ReceivingThread extends WaitableThread {

	private final BasicPeer peer;
	
	private final Logger logger = Logger.getLogger(ReceivingThread.class);

	public ReceivingThread(final BasicPeer peer) {
		this.peer = peer; 
	}

	@Override
	public void run() {
		// Reception thread main loop
		while (!Thread.interrupted())			
			try {
				byte[] data = peer.getPeerBehavior().receiveData();
				
				if (data != null) {				
					final BroadcastMessage message = (BroadcastMessage) getObject(data);	
					peer.processReceivedPacket(message);
				}
			} catch (final IOException e) {
				e.printStackTrace();
				logger.error("Peer " + peer.getPeerID() + " problem deserializing received data. " + e.getMessage());
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
				logger.error("Peer " + peer.getPeerID() + " problem deserializing received data. " + e.getMessage());
			}
		
			logger.trace("Peer " + peer.getPeerID() + " receiving loop exited");

		this.threadFinished();
	}
	
	// Creates an object from its byte array representation
	private Object getObject(final byte[] bytes) throws IOException, ClassNotFoundException {
		final ByteArrayInputStream bios = new ByteArrayInputStream(bytes);
		final ObjectInput in = new ObjectInputStream(bios);
		return in.readObject();
	}
}
