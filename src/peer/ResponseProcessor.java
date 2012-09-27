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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.messagecounter.MessageCounter;
import util.WaitableThread;
import util.logger.Logger;

/**
 * This class implements a message processor used for decoupling it from the
 * receiving thread. This enables to continue receiving messages when the a
 * waiting action is being perform (i.e reliableBroadcast)
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
final class ResponseProcessor extends WaitableThread {

	private static final int MAX_MESSAGES = 50;

	// the communication peer
	private final ReliableBroadcastPeer peer;
	
	private final Logger logger = Logger.getLogger(ResponseProcessor.class);

	private final List<BroadcastMessage> waitingResponses = new ArrayList<BroadcastMessage>();
	
	private final Set<ACKMessage> waitingACKMessages = new HashSet<ACKMessage>(); 

	private final Random r = new Random();
	
	private final MessageCounter msgCounter;
		
	private ReliableBroadcast reliableBroadcast = null;
	private final Object mutex = new Object();

	/**
	 * Constructor of the message processor
	 * 
	 * @param peer
	 *            the communication peer
	 */
	public ResponseProcessor(final ReliableBroadcastPeer peer, final MessageCounter msgCounter) {
		this.peer = peer;
		this.msgCounter = msgCounter;
	}

	@Override
	public void run() {		
		while (!Thread.interrupted()) {			
			randomSleep(); 
			
			if (!Thread.interrupted()) {						
				final BundleMessage bundleMessage = processResponses();
				
				if (!bundleMessage.getPayloadMessages().isEmpty())
					sendResponses(bundleMessage);								
			} else
				interrupt();
			
			//check if there are pending ACK messages
			final Set<BroadcastMessage> ackMessages = getWaitingACKMessages();
			
			if (!ackMessages.isEmpty()) {			
				logger.trace("Peer " + peer.getPeerID() + " sending bundled message with " + ackMessages.size() + " ACK messages");
				final BundleMessage bundleACKMessages = new BundleMessage(peer.getPeerID(), new ArrayList<BroadcastMessage>(ackMessages));
				peer.directBroadcast(bundleACKMessages);
				sentACKMessages(ackMessages);
			}
		}

		logger.trace("Peer " + peer.getPeerID() + " message processor finalized");
		threadFinished();
	}

	private void randomSleep() { 
		final long randomWait = ReliableBroadcastPeer.WAIT_TIME - r.nextInt(ReliableBroadcastPeer.MAX_JITTER);				
		try {
			Thread.sleep(randomWait);
		} catch (InterruptedException e) {
			interrupt();
		}
	}

	public BundleMessage processResponses() {
		final List<BroadcastMessage> responses = new ArrayList<BroadcastMessage>();
		
		synchronized (waitingResponses) {
			int counter = 0;
			if (!waitingResponses.isEmpty())	 {								
				for (final Iterator<BroadcastMessage> it = waitingResponses.iterator(); it.hasNext() && counter < MAX_MESSAGES;) {
					final BroadcastMessage broadcastMessage = it.next();
					responses.add(broadcastMessage);
					it.remove();
					counter++;
				}
			}					
		}

		return new BundleMessage(peer.getPeerID(), responses);
	}

	private void sendResponses(final BundleMessage bundleMessage) {
		msgCounter.addSent(bundleMessage.getClass());

		synchronized (mutex) {
			reliableBroadcast = new ReliableBroadcast(peer, this);
		}
		
		reliableBroadcast.broadcast(bundleMessage);
		
		synchronized (mutex) {
			reliableBroadcast = null;
		}
	}

	public boolean addResponse(final BroadcastMessage message, CommunicationLayer layer) {
		synchronized (waitingResponses) {
			if (layer != null) {
				final boolean merged = layer.merge(Collections.unmodifiableList(waitingResponses), message);
				if (!merged) {
					waitingResponses.add(message);				
					return true;
				}
				return false;
			}
			
			waitingResponses.add(message);				
			return true;
		}
	}

	public void addReceivedACKResponse(final ACKMessage ackMessage) {
		synchronized (mutex) {
			if (reliableBroadcast != null)
				reliableBroadcast.addReceivedACKResponse(ackMessage);
		}
	}
	
	public void addACKMessage(final ACKMessage ackMessage) {
		synchronized (waitingACKMessages) {
			waitingACKMessages.add(ackMessage);
		}
	}
	
	public Set<BroadcastMessage> getWaitingACKMessages() {
		final Set<BroadcastMessage> ackMessages = new HashSet<BroadcastMessage>();
		synchronized (waitingACKMessages) {
			ackMessages.addAll(waitingACKMessages);
		}
		return ackMessages;
	}

	public void sentACKMessages(Set<BroadcastMessage> sentACKMessages) {
		synchronized (waitingACKMessages) {
			waitingACKMessages.removeAll(sentACKMessages);
		}
	}
}
