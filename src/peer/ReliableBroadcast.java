package peer;

import java.util.Random;
import java.util.Set;

import peer.message.ACKMessage;
import peer.message.BundleMessage;
import peer.messagecounter.ReliableBroadcastCounter;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import util.WaitableThread;
import util.logger.Logger;
import detection.NeighborEventsListener;

final class ReliableBroadcast implements NeighborEventsListener {

	private BundleMessage currentMessage;

	private final BasicPeer peer;

	private final ReliableBroadcastCounter reliableBroadcastCounter = new ReliableBroadcastCounter();

	private final Object mutex = new Object();
	
	private final Random r = new Random();
	
	private final ResponseProcessor messageProcessor;

	private final Logger logger = Logger.getLogger(ReliableBroadcast.class);

	public ReliableBroadcast(final BasicPeer peer, final ResponseProcessor messageProccessor) {
		this.peer = peer;
		this.messageProcessor = messageProccessor;
	}

	public void broadcast(final BundleMessage bundleMessage) {
		peer.getDetector().addNeighborListener(this);
		
		//wait for real neighbors only
		final Set<PeerID> currentNeighbors = peer.getDetector().getCurrentNeighbors().getPeerSet();
		
		synchronized (mutex) {
			currentMessage = bundleMessage;
			//remove invalid ones
			for (final PeerID expectedDestination : currentMessage.getExpectedDestinations())
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
				final long delayTime = peer.getFixedWaitTime() - r.nextInt(ResponseProcessor.MAX_JITTER) ;
				
				sleepSomeTime(delayTime);
				
				if (Thread.interrupted() || delivered())
					break;
					
				logger.debug("Peer " + peer.getPeerID() + " rebroadcasted message " + currentMessage.getMessageID() + " " + currentMessage.getExpectedDestinations() + " adding " + delayTime + " ms try " + tryNumber);
				reliableBroadcastCounter.addRebroadcastedMessage();				
			}
			
			peer.broadcast(bundleMessage);
			
			sleepSomeTime(responseWaitTime);
			
			if (Thread.interrupted() || delivered())
				break;
			
			tryNumber++;
		}
		
		if (delivered())
			messageDelivered(reliableBroadcastStartTime);
		else
			messageProcessor.interrupt();
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
				messageProcessor.interrupt();
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
				messageProcessor.interrupt();
			}
		}
	}

	public long getResponseWaitTime(int destinations) {
		return peer.getTransmissionTime() * (destinations + 1) + peer.getMaxJitter() + peer.getFixedWaitTime();
	}

	@Override
	public void appearedNeighbors(final PeerIDSet neighbors) {
	}

	@Override
	public void dissapearedNeighbors(final PeerIDSet disappearedNeighbors) {
		synchronized (mutex) {
			for (final PeerID dissappearedNeighbor : disappearedNeighbors.getPeerSet())
				currentMessage.removeDestination(dissappearedNeighbor);
		}
	}
}
