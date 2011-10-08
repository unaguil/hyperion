package peer;

import java.util.Random;

import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.messagecounter.ReliableBroadcastCounter;
import peer.peerid.PeerIDSet;
import util.logger.Logger;
import util.timer.Timer;
import util.timer.TimerTask;
import config.Configuration;
import detection.NeighborEventsListener;
import detection.message.BeaconMessage;

class ReliableBroadcast implements TimerTask, NeighborEventsListener {

	private static class RebroadcastThread extends Timer {

		public RebroadcastThread(final long period, final TimerTask timerTask) {
			super(period, timerTask);
		}
	}

	private BroadcastMessage currentMessage;

	private final Peer peer;

	private final RebroadcastThread rebroadcastThread;

	private final static long CHECK_PERIOD = 10;

	private final ReliableBroadcastCounter reliableBroadcastCounter = new ReliableBroadcastCounter();

	private final Object mutex = new Object();
	private boolean processingMessage = false;
	private boolean rebroadcast = true;

	private final Random r = new Random();

	private int tryNumber;
	private long broadcastStartTime;
	private long lastBroadcastTime;

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
			logger.debug("Peer " + peer.getPeerID() + " reliable broadcasting message " + broadcastMessage.getMessageID());
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

	private long getBackoffTime(final int factor) {
		final int slots = 2 << factor;
		final int pos = r.nextInt(slots);
		return BasicPeer.TRANSMISSION_TIME * pos;
	}

	public long getResponseWaitTime() {
		return BasicPeer.TRANSMISSION_TIME * (peer.getDetector().getCurrentNeighbors().size() + 1);
	}

	private boolean mustRebroadcast() {
		synchronized (mutex) {
			return rebroadcast;
		}
	}

	@Override
	public void perform() throws InterruptedException {
		final int neighbors = peer.getDetector().getCurrentNeighbors().size();
		long elapsedTime = 0;
		long responseWaitTime = 0;
		long backoffTime = 0;

		synchronized (mutex) {
			if (processingMessage) {
				// calculate elapsed time since last broadcast
				responseWaitTime = getResponseWaitTime();
				elapsedTime = System.currentTimeMillis() - lastBroadcastTime;
				if (elapsedTime >= responseWaitTime) {
					backoffTime = getBackoffTime(tryNumber);

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

		if (mustRebroadcast() && backoffTime > 0) {
			logger.debug("Peer " + peer.getPeerID() + " sleeping " + backoffTime + " ms for message " + currentMessage.getMessageID());
			Thread.sleep(backoffTime);
		}

		synchronized (mutex) {
			if (processingMessage && rebroadcast) {
				peer.broadcast(currentMessage);
				logger.debug("Peer " + peer.getPeerID() + " rebroadcasted message " + currentMessage.getMessageID() + " try " + tryNumber + " neighbors " + neighbors + " responseWaitTime " + responseWaitTime + " elapsedTime " + elapsedTime + " backoffTime " + backoffTime);
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
				currentMessage.removeExpectedDestinations(disappearedNeighbors.getPeerSet());

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

	public void sendACKMessage(final BroadcastMessage broadcastMessage) {
		final ACKMessage ackMessage = new ACKMessage(peer.getPeerID(), broadcastMessage.getMessageID());
		logger.debug("Peer " + peer.getPeerID() + " sending ACK message " + ackMessage);
		peer.broadcast(ackMessage);
	}
}
