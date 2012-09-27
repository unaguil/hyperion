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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package peer;

import java.io.IOException;
import java.util.Set;

import peer.conditionregister.ConditionRegister;
import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.message.MessageID;
import peer.peerid.PeerID;
import util.logger.Logger;
import detection.NeighborDetector;
import detection.NeighborEventsListener;
import detection.beaconDetector.BeaconDetector;

public final class ReliableBroadcastPeer extends BasicPeer implements ReliablePeer, NeighborEventsListener {

	// Default neighbor detector
	private NeighborDetector detector;
	
	//Response processor
	private ResponseProcessor responseProcessor = null;

	private final Logger logger = Logger.getLogger(ReliableBroadcastPeer.class);
	
	// used to discard already received messages
	protected final ConditionRegister<MessageID> receivedMessages = new ConditionRegister<MessageID>(CLEAN_REC_MSGS);

	// Default reception buffer length
	public static final int TRANSMISSION_TIME = 8;
	
	public static final int WAIT_TIME = 25;
	public static final int MAX_JITTER = 10;

	/**
	 * Constructor of the class. It is the default constructor which configures
	 * all internal properties.
	 */
	public ReliableBroadcastPeer(final CommProvider commProvider) {
		super(commProvider);
	}
	
	@Override
	protected synchronized void initialize() {
		super.initialize();
		receivedMessages.start();
		responseProcessor.start();
	}

	/**
	 * Gets the used neighbor detector.
	 * 
	 * @return the used neighbor detector.
	 */
	@Override
	public NeighborDetector getDetector() {
		return detector;
	}

	@Override
	public void enqueueBroadcast(final BroadcastMessage message, CommunicationLayer layer) {
		if (responseProcessor.addResponse(message, layer)) {
			try {
				final int messageSize = getSize(message);
				logger.debug("Peer " + getPeerID() + " sending " + message.getType() + " " + message.getMessageID() + " " + messageSize + " bytes");
				msgCounter.addSent(message.getClass());
			} catch (IOException e) {
				logger.error("Peer " + getPeerID() + " unable to obtain message size");
			}
		}
	}

	@Override
	protected void stopThreads() {
		// stop received messages table thread
		logger.trace("Peer " + getPeerID() + " stopping received messages thread");
		receivedMessages.stopAndWait();

		// Stop message processor
		logger.trace("Peer " + getPeerID() + " stopping message processor thread");
		responseProcessor.stopAndWait();

		super.stopThreads();
	}
	
	private boolean containsOnlyACKMessages(final BundleMessage bundleMessage) {
		for (final BroadcastMessage broadcastMessage : bundleMessage.getPayloadMessages())
			if (!(broadcastMessage instanceof ACKMessage))
				return false;
		return true;
	}

	private void processBundleMessage(final BundleMessage bundleMessage) {				
		for (final BroadcastMessage broadcastMessage : bundleMessage.getPayloadMessages())
			if (broadcastMessage instanceof ACKMessage)
				processACKMessage((ACKMessage) broadcastMessage);
		
		if (containsOnlyACKMessages(bundleMessage))
			return;
				
		//messages which does not have this node as destination are discarded
		if (!bundleMessage.getExpectedDestinations().contains(getPeerID())) {
			logger.trace("Peer " + getPeerID() +  " discarded message " + bundleMessage.getMessageID() + " because it was not intended for this peer");
			return;
		}
		
		//all received messages are responded with ACK
		sendACKMessage(bundleMessage);
		
		for (final BroadcastMessage broadcastMessage : bundleMessage.getPayloadMessages()) {
			if (!(broadcastMessage instanceof ACKMessage)) {
				//if message was not already received
				if (!receivedMessages.contains(broadcastMessage.getMessageID())) {
					// save new messages
					receivedMessages.addEntry(broadcastMessage.getMessageID());	
					msgCounter.addReceived(broadcastMessage.getClass());
					
					logger.debug("Peer " + getPeerID() + " received " + broadcastMessage.getType() + " " + broadcastMessage.getMessageID() + " from node " + broadcastMessage.getSender());
					notifyReceivingListener(broadcastMessage, System.currentTimeMillis());
				} else
					logger.trace("Peer " + getPeerID() + " discarded " + broadcastMessage + " because it was already received.");
			}
		}
	}

	private void processACKMessage(final ACKMessage ackMessage) {
		msgCounter.addReceived(ackMessage.getClass());
		logger.debug("Peer " + getPeerID() + " received " + ackMessage.getType() + " " + ackMessage.getMessageID() + " from node " + ackMessage.getSender());
		responseProcessor.addReceivedACKResponse(ackMessage);
	}
	
	private void sendACKMessage(final BundleMessage receivedBundleMessage) {
		final ACKMessage ackMessage = new ACKMessage(getPeerID(), receivedBundleMessage.getMessageID());
		logger.debug("Peer " + getPeerID() + " sending " + ackMessage.getType() + " " + ackMessage.getMessageID());
		responseProcessor.addACKMessage(ackMessage);
		msgCounter.addSent(ackMessage.getClass());
	}
	
	@Override
	protected void initializeLayers() {
		responseProcessor = new ResponseProcessor(this, msgCounter);
		
		detector = new BeaconDetector(this);
		
		super.initializeLayers();
	}
	
	@Override
	protected void receiveMessage(final BroadcastMessage message) {
		logger.trace("Peer " + getPeerID() + " processing message " + message);
		if (message instanceof BundleMessage) {
			processBundleMessage((BundleMessage)message);
		}
		else
			notifyReceivingListener(message, System.currentTimeMillis());
	}

	@Override
	public void neighborsChanged(final Set<PeerID> newNeighbors, final Set<PeerID> lostNeighbors) {
		for (MessageID messageID : receivedMessages.getEntries()) {
			if (lostNeighbors.contains(messageID.getPeer())) {
				logger.trace("Peer " + getPeerID() + " removing all messages received from neighbor " + messageID.getPeer());
				receivedMessages.remove(messageID);
			}
		}
	}
}
