package peer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.messagecounter.MessageCounter;
import peer.messagecounter.ReliableBroadcastCounter;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import util.logger.Logger;
import util.timer.Timer;
import util.timer.TimerTask;
import config.Configuration;
import detection.NeighborEventsListener;
import detection.message.BeaconMessage;

final class ReliableBroadcast implements TimerTask, NeighborEventsListener {

	private final static class RebroadcastThread extends Timer {

		public RebroadcastThread(final long period, final TimerTask timerTask) {
			super(period, timerTask);
		}
	}

	private BroadcastMessage currentMessage;

	private final Peer peer;

	private final RebroadcastThread rebroadcastThread;

	private final static long CHECK_PERIOD = 0;

	private final ReliableBroadcastCounter reliableBroadcastCounter = new ReliableBroadcastCounter();

	private final Object mutex = new Object();
	private boolean processingMessage = false;
	private boolean rebroadcast = true;
	private long responseWaitTime;

	private int tryNumber;
	private long broadcastStartTime;
	private long lastBroadcastTime;
	private final AtomicInteger lastReceivedDestinations = new AtomicInteger();

	private int MAX_TRIES = 3;

	private final Logger logger = Logger.getLogger(ReliableBroadcast.class);

	public ReliableBroadcast(final Peer peer) {
		this.peer = peer;

		this.rebroadcastThread = new RebroadcastThread(CHECK_PERIOD, this);
	}

	public void start() {
		try {
			final String maxTries = Configuration.getInstance().getProperty("reliableBroadcast.maxTries");
			MAX_TRIES = Integer.parseInt(maxTries);
			logger.info("Peer " + peer.getPeerID() + " set MAX_TRIES to " + MAX_TRIES);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		peer.getDetector().addNeighborListener(this);

		rebroadcastThread.start();
	}

	public void stopAndWait() {
		synchronized (mutex) {
			processingMessage = false;
		}
		rebroadcastThread.stopAndWait();
	}

	private boolean mustWait() {
		synchronized (mutex) {
			return processingMessage;
		}
	}

	public void broadcast(final BroadcastMessage broadcastMessage) {
		// block while a message is being broadcasting
		while (mustWait())
			Thread.yield();

		// bundle messages containing beacon messages only are directly
		// broadcasted
		if (broadcastMessage instanceof BundleMessage && containsOnlyBeaconMessages((BundleMessage) broadcastMessage)) {
			peer.broadcast(broadcastMessage);
			return;
		}

		// messages with empty destinations are not reliable broadcasted
		if (broadcastMessage.getExpectedDestinations().isEmpty())
			return;

		synchronized (mutex) {
			currentMessage = broadcastMessage;
			broadcastStartTime = lastBroadcastTime = System.currentTimeMillis();
			tryNumber = 1;
			responseWaitTime = getResponseWaitTime(currentMessage.getExpectedDestinations().size());
			logger.debug("Peer " + peer.getPeerID() + " reliable broadcasting message " + broadcastMessage.getMessageID() + " " + broadcastMessage.getExpectedDestinations() + " responseWaitTime: " + responseWaitTime);
			rebroadcast = false;
			processingMessage = true;
		}

		reliableBroadcastCounter.addBroadcastedMessage();
		peer.broadcast(broadcastMessage);
	}

	public static boolean containsOnlyBeaconMessages(final BundleMessage bundleMessage) {
		for (final BroadcastMessage broadcastMessage : bundleMessage.getMessages())
			if (!(broadcastMessage instanceof BeaconMessage))
				return false;
		return true;
	}

	public void addACKResponse(final ACKMessage ackMessage) {
		// check if message is responded by the current ACK message
		synchronized (mutex) {
			if (processingMessage)
				if (ackMessage.getRespondedMessageID().equals(currentMessage.getMessageID())) {
					currentMessage.removeExpectedDestination(ackMessage.getSender());
					logger.debug("Peer " + peer.getPeerID() + " added response from " + ackMessage.getSender() + " for " + currentMessage.getMessageID());
					// check if all responses have being received
					if (currentMessage.getExpectedDestinations().isEmpty()) {
						final long deliveringTime = System.currentTimeMillis() - broadcastStartTime;
						reliableBroadcastCounter.addDeliveredMessage(deliveringTime);
						logger.debug("Peer " + peer.getPeerID() + " delivered message " + currentMessage.getMessageID());
						processingMessage = false;
					}
				}
		}
	}

	public static long getBackoffTime(int currentNeighbors) {
		return currentNeighbors * BasicPeer.ACK_TRANSMISSION_TIME;
	}

	public long getResponseWaitTime(int destinations) {
		return BasicPeer.TRANSMISSION_TIME + destinations * BasicPeer.ACK_TRANSMISSION_TIME;
	}

	private boolean mustRebroadcast() {
		synchronized (mutex) {
			return processingMessage && rebroadcast;
		}
	}

	@Override
	public void perform() throws InterruptedException {
		synchronized (mutex) {
			if (processingMessage && !mustRebroadcast()) {
				// calculate elapsed time since last broadcast
				long elapsedTime = System.currentTimeMillis() - lastBroadcastTime;
				if (elapsedTime >= responseWaitTime) {
					if (tryNumber == MAX_TRIES) {
						// maximum tries reached. failed broadcast
						processingMessage = false;
						rebroadcast = false;
						logger.debug("Peer " + peer.getPeerID() + " failed reliable broadcast " + currentMessage.getMessageID());
						return;
					}

					rebroadcast = true;
				}
			}
		}

//		final long backoffTime = getBackoffTime(lastReceivedDestinations.get());
// 
//		if (mustRebroadcast() && backoffTime > 0) {
//			logger.debug("Peer " + peer.getPeerID() + " waiting " + backoffTime + " ms for rebroadcast of message " + currentMessage.getMessageID());
//			long startTime = System.currentTimeMillis();
//			boolean interrupted = false;
//			do {
//				Thread.yield();
//				interrupted = Thread.interrupted();
//				if (interrupted) {
//					rebroadcastThread.interrupt();
//					return;
//				}
//			} while (mustRebroadcast() && (System.currentTimeMillis() - startTime) < backoffTime && !interrupted);
//		}

		if (mustRebroadcast()) {
			synchronized (mutex) {
				peer.broadcast(currentMessage);
				reliableBroadcastCounter.addRebroadcastedMessage();
				responseWaitTime = getResponseWaitTime(currentMessage.getExpectedDestinations().size());
				logger.debug("Peer " + peer.getPeerID() + " rebroadcasted message " + currentMessage.getMessageID() + " " + currentMessage.getExpectedDestinations() + " backoffTime " + 0 + " waitingTime: " + responseWaitTime);
				lastBroadcastTime = System.currentTimeMillis();
				tryNumber++;
				rebroadcast = false;
			}
		}
	}

	private void removeNeighbors(final PeerIDSet disappearedNeighbors) {
		synchronized (mutex) {
			if (processingMessage) {
				// remove disappeared neighbors
				for (final PeerID dissappearedNeighbor : disappearedNeighbors.getPeerSet())
					currentMessage.removeExpectedDestination(dissappearedNeighbor);

				if (currentMessage.getExpectedDestinations().isEmpty())
					processingMessage = false;
			}
		}
	}

	@Override
	public void appearedNeighbors(final PeerIDSet neighbors) {
	}

	@Override
	public void dissapearedNeighbors(final PeerIDSet neighbors) {
		removeNeighbors(neighbors);
	}
	
	private class DelayACK extends Thread {
		
		private final ACKMessage ackMessage;
		private final List<PeerID> expectedDestinations;
		private final MessageCounter msgCounter;
		
		public DelayACK(final ACKMessage ackMessage, List<PeerID> expectedDestinations, MessageCounter msgCounter) {
			this.ackMessage = ackMessage;
			this.expectedDestinations = expectedDestinations;
			this.msgCounter = msgCounter;
		}
		
		@Override
		public void run() {
			
			int index = 0;
			for (PeerID dest : expectedDestinations) {
				if (dest.equals(peer.getPeerID()))
					break;
				index++;
			}
			
			if (index > 0) {
				final long time = index * BasicPeer.ACK_TRANSMISSION_TIME;
				logger.debug("Peer " + peer.getPeerID() + " delayed ACK response " + ackMessage + " during " + time + " ms");
				try {
					Thread.sleep(time);
				} catch (InterruptedException e) {
				}
			}		
			
			msgCounter.addSent(ackMessage.getClass());
			peer.broadcast(ackMessage);
		}
	}

	public void sendACKMessage(final BroadcastMessage broadcastMessage, MessageCounter msgCounter) {
		final ACKMessage ackMessage = new ACKMessage(peer.getPeerID(), broadcastMessage.getMessageID());
		logger.debug("Peer " + peer.getPeerID() + " sending ACK message " + ackMessage);
		lastReceivedDestinations.set(broadcastMessage.getExpectedDestinations().size());
		DelayACK delayACK = new DelayACK(ackMessage, broadcastMessage.getExpectedDestinations(), msgCounter);
		delayACK.start();
	}
}
