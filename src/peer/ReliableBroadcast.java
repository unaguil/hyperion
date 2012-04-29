package peer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.messagecounter.ReliableBroadcastCounter;
import peer.peerid.PeerID;
import util.WaitableThread;
import util.logger.Logger;
import detection.NeighborEventsListener;

final class ReliableBroadcast implements NeighborEventsListener {

	private BundleMessage currentMessage;

	private final ReliableBroadcastPeer peer;

	private final ReliableBroadcastCounter reliableBroadcastCounter = new ReliableBroadcastCounter();

	private final Object mutex = new Object();
	
	private final Random r = new Random();
	
	private final ResponseProcessor responseProcessor;

	private final Logger logger = Logger.getLogger(ReliableBroadcast.class);

	public ReliableBroadcast(final ReliableBroadcastPeer peer, final ResponseProcessor messageProccessor) {
		this.peer = peer;
		this.responseProcessor = messageProccessor;
	}

	public void broadcast(final BundleMessage bundleMessage) {
		peer.getDetector().addNeighborListener(this);
		
		//wait for real neighbors only
		final Set<PeerID> currentNeighbors = peer.getDetector().getCurrentNeighbors();
		
		synchronized (mutex) {
			currentMessage = bundleMessage;
			//remove invalid ones
			final Set<PeerID> expectedDestinations = new HashSet<PeerID>(currentMessage.getExpectedDestinations());
			for (final PeerID expectedDestination : expectedDestinations)
				if (!currentNeighbors.contains(expectedDestination))
					currentMessage.removeDestination(expectedDestination);
		}
		
		// messages with empty destinations are not reliable broadcasted
		if (bundleMessage.getExpectedDestinations().isEmpty())
			return;
		
		int tryNumber = 1;
		
		final long reliableBroadcastStartTime = System.currentTimeMillis();
		
		while (!Thread.interrupted() && !delivered()) {
			final long responseWaitTime = getResponseWaitTime(getExpectedResponses());			
			
			if (tryNumber == 1) {
				logger.debug("Peer " + peer.getPeerID() + " reliable broadcasting message " + bundleMessage.getMessageID() + " dest: " + bundleMessage.getExpectedDestinations() + " responseWaitTime: " + responseWaitTime);
				reliableBroadcastCounter.addBroadcastedMessage();
			}
			else {
				final long delayTime = ReliableBroadcastPeer.WAIT_TIME * (tryNumber * tryNumber) - r.nextInt(ReliableBroadcastPeer.MAX_JITTER);
				
				sleepSomeTime(delayTime);
				
				if (Thread.interrupted() || delivered())
					break;
					
				logger.debug("Peer " + peer.getPeerID() + " rebroadcasted message " + currentMessage.getMessageID() + " " + currentMessage.getExpectedDestinations() + " adding " + delayTime + " ms try " + tryNumber);
				reliableBroadcastCounter.addRebroadcastedMessage();	
			}
			
			synchronized (mutex) {
				bundleMessage.addMessages(new ArrayList<BroadcastMessage>(responseProcessor.getWaitingACKMessages()));
			}
			
			peer.directBroadcast(bundleMessage);
			
			final Set<BroadcastMessage> sentACKMessages = new HashSet<BroadcastMessage>();
			synchronized (mutex) {
				sentACKMessages.addAll(bundleMessage.removeACKMessages());
			}
			
			responseProcessor.sentACKMessages(sentACKMessages);
			
			sleepSomeTime(responseWaitTime);
			
			if (Thread.interrupted() || delivered())
				break;
			
			tryNumber++;
		}
		
		if (delivered())
			messageDelivered(reliableBroadcastStartTime);
		else
			responseProcessor.interrupt();
	}

	private void messageDelivered(final long reliableBroadcastStartTime) {
		final long deliveringTime = System.currentTimeMillis() - reliableBroadcastStartTime;
		reliableBroadcastCounter.addDeliveredMessage(deliveringTime);
		logger.debug("Peer " + peer.getPeerID() + " delivered message " + currentMessage.getMessageID());
	}

	private void sleepSomeTime(final long delayTime) {	
		try {
			WaitableThread.mySleep(delayTime);
		} catch (InterruptedException e) {
			if (!delivered())
				responseProcessor.interrupt();
		}
	}
	
	private boolean delivered() {
		return getExpectedResponses() == 0;
	}
	
	private int getExpectedResponses() {
		return currentMessage.getExpectedDestinations().size();
	}
	
	private boolean isRespondedBy(final ACKMessage ackMessage) {
		synchronized (mutex) {
			return ackMessage.getRespondedMessageID().equals(currentMessage.getMessageID());
		}
	}

	public void addReceivedACKResponse(final ACKMessage ackMessage) {
		if (isRespondedBy(ackMessage)) {
			synchronized (mutex) {
				currentMessage.removeDestination(ackMessage.getSender());
			}
			
			logger.trace("Peer " + peer.getPeerID() + " added response from " + ackMessage.getSender() + " for " + currentMessage.getMessageID() + " missing responses: " + currentMessage.getExpectedDestinations());
			
			if (delivered()) {
				responseProcessor.interrupt();
			}
		}
	}

	public long getResponseWaitTime(int destinations) {
		return ReliableBroadcastPeer.TRANSMISSION_TIME * (destinations + 1) + ReliableBroadcastPeer.WAIT_TIME + ReliableBroadcastPeer.MAX_JITTER;
	}

	@Override
	public void neighborsChanged(final Set<PeerID> newNeighbors, final Set<PeerID> lostNeighbors) {
		synchronized (mutex) {
			for (final PeerID dissappearedNeighbor : lostNeighbors)
				currentMessage.removeDestination(dissappearedNeighbor);
		}
	}
}
