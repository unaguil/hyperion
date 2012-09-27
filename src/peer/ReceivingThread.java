/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package peer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import peer.message.BroadcastMessage;
import peer.message.MessageReceivedListener;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.messagecounter.MessageCounter;
import util.WaitableThread;
import util.logger.Logger;

/**
 * This class is used to implement the message receiving thread.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
final class ReceivingThread extends WaitableThread {

	private final ReceivedProcessor receivedProcessor;

	private final Logger logger = Logger.getLogger(ReceivingThread.class);
	private final BasicPeer peer;
	private final MessageCounter msgCounter;
	
	// Sets the message hearing listener
	private MessageReceivedListener hearListener = null;
	

	public ReceivingThread(final ReceivedProcessor receivedProcessor, final BasicPeer peer, final MessageCounter msgCounter) {
		this.receivedProcessor = receivedProcessor;
		this.peer = peer;
		this.msgCounter = msgCounter;
	}
	
	public void setHearListener(final MessageReceivedListener hearListener) {
		this.hearListener = hearListener;
	}
	
	private void notifyHearListener(final BroadcastMessage message, final long receptionTime) {
		if (hearListener != null)
			hearListener.messageReceived(message, receptionTime);
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
					final BroadcastMessage message = getBroadcastMessage(data);
					if (peer.getCommProvider().isValid(message)) {
						// messages are only processed if node is initialized
						logger.debug("Peer " + peer.getPeerID() + " received packet " + message + " from node " + message.getSender());
						msgCounter.addReceivedPacket(message.getClass());
						// Notify hear listeners indicating that a message was received
						notifyHearListener(message, System.currentTimeMillis());
						msgCounter.addReceived(message.getClass());
						receivedProcessor.enqueuReceivedMessage(message);
						logger.trace("Peer " + peer.getPeerID() + " received message enqueued");
					}
				}
			} catch (final IOException e) {
				logger.error("Peer " + peer.getPeerID() + " problem deserializing received data. " + e.getMessage());
			} catch (final UnsupportedTypeException e) {
				logger.error("Peer " + peer.getPeerID() + " problem deserializing received data. " + e.getMessage());
			}
		}

		logger.trace("Peer " + peer.getPeerID() + " receiving thread finalized");
		this.threadFinished();
	}

	// Creates an object from its byte array representation
	private BroadcastMessage getBroadcastMessage(final byte[] bytes) throws IOException, UnsupportedTypeException {
		final ByteArrayInputStream bios = new ByteArrayInputStream(bytes);
		final ObjectInputStream in = new ObjectInputStream(bios);
		final BroadcastMessage message = MessageTypes.readBroadcastMessage(in);
		return message;
	}
}
