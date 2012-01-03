package peer;

import java.util.Random;

import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.messagecounter.ReliableBroadcastCounter;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import util.logger.Logger;
import util.timer.Timer;
import util.timer.TimerTask;
import detection.NeighborEventsListener;
import detection.message.BeaconMessage;

final class ReliableBroadcast implements TimerTask, NeighborEventsListener {

	private final static class RebroadcastThread extends Timer {

		public RebroadcastThread(final long period, final TimerTask timerTask) {
			super(period, timerTask);
		}
	}

	private BundleMessage currentMessage;

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
	
	private final Random r = new Random();

	private final Logger logger = Logger.getLogger(ReliableBroadcast.class);

	public ReliableBroadcast(final Peer peer) {
		this.peer = peer;

		this.rebroadcastThread = new RebroadcastThread(CHECK_PERIOD, this);
	}

	public void start() {
		peer.getDetector().addNeighborListener(this);

		rebroadcastThread.start();
	}

	public void stopAndWait() {
		synchronized (mutex) {
			processingMessage = false;
			performNotify();
		}
		rebroadcastThread.stopAndWait();
	}

	public boolean isProcessingMessage() {
		synchronized (mutex) {
			return processingMessage;
		}
	}
	
	protected void performNotify() {
		synchronized (this) {
			this.notifyAll();
		}
	}

	private void performWait() {
		synchronized (this) {
			try {
				this.wait();
			} catch (final InterruptedException e) {
				// do nothing
			}
		}
	}

	public void broadcast(final BundleMessage bundleMessage) {
		// block while a message is being broadcasting
		while (isProcessingMessage())
			this.performWait();

		// bundle messages containing only BeaconMessages or ACKMessages are directly broadcasted
		if (containsOnlyBeaconMessages(bundleMessage) || containsOnlyACKMessages(bundleMessage)) {
			peer.broadcast(bundleMessage);
			return;
		}

		// messages with empty destinations are not reliable broadcasted
		if (bundleMessage.getExpectedDestinations().isEmpty())
			return;

		synchronized (mutex) {
			currentMessage = bundleMessage;
			broadcastStartTime = lastBroadcastTime = System.currentTimeMillis();
			tryNumber = 1;
			responseWaitTime = getResponseWaitTime(currentMessage.getExpectedDestinations().size());
			logger.debug("Peer " + peer.getPeerID() + " reliable broadcasting message " + bundleMessage.getMessageID() + " " + bundleMessage.getExpectedDestinations() + " responseWaitTime: " + responseWaitTime);
			rebroadcast = false;
			processingMessage = true;
		}

		reliableBroadcastCounter.addBroadcastedMessage();
		peer.broadcast(bundleMessage);
	}

	public static boolean containsOnlyBeaconMessages(final BundleMessage bundleMessage) {
		for (final BroadcastMessage broadcastMessage : bundleMessage.getMessages())
			if (!(broadcastMessage instanceof BeaconMessage))
				return false;
		return true;
	}
	
	public static boolean containsOnlyACKMessages(final BundleMessage bundleMessage) {
		for (final BroadcastMessage broadcastMessage : bundleMessage.getMessages())
			if (!(broadcastMessage instanceof ACKMessage))
				return false;
		return true;
	}

	public void receivedACKResponse(final ACKMessage ackMessage) {
		// check if message is responded by the current ACK message
		synchronized (mutex) {
			if (processingMessage)
				if (ackMessage.getRespondedMessageID().equals(currentMessage.getMessageID())) {
					currentMessage.removeExpectedDestination(ackMessage.getSender());
					logger.trace("Peer " + peer.getPeerID() + " added response from " + ackMessage.getSender() + " for " + currentMessage.getMessageID() + " missing responses: " + currentMessage.getExpectedDestinations());
					// check if all responses have being received
					if (currentMessage.getExpectedDestinations().isEmpty()) {
						final long deliveringTime = System.currentTimeMillis() - broadcastStartTime;
						reliableBroadcastCounter.addDeliveredMessage(deliveringTime);
						logger.debug("Peer " + peer.getPeerID() + " delivered message " + currentMessage.getMessageID());
						processingMessage = false;
						performNotify();
					}
				}
		}
	}

	public long getResponseWaitTime(int destinations) {
		return BasicPeer.TRANSMISSION_TIME + (destinations + 1) * BasicPeer.RANDOM_DELAY;
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
					rebroadcast = true;
				}
			}
		}

		final long delayTime = r.nextInt(BasicPeer.RANDOM_DELAY);
		if (mustRebroadcast() && delayTime > 0) {
			try {
				Thread.sleep(delayTime);
			} catch (InterruptedException e) {
				rebroadcastThread.interrupt();
				return;
			}
		}

		if (mustRebroadcast()) {
			synchronized (mutex) {
				if (tryNumber == 1) {
					logger.debug("Peer " + peer.getPeerID() + " rebroadcasted message " + currentMessage.getMessageID() + " " + currentMessage.getExpectedDestinations() + " adding " + delayTime + " ms");
					reliableBroadcastCounter.addRebroadcastedMessage();
				}
				
				peer.broadcast(currentMessage);
				
				responseWaitTime = getResponseWaitTime(currentMessage.getExpectedDestinations().size());
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

				if (currentMessage.getExpectedDestinations().isEmpty()) {
					processingMessage = false;
					performNotify();
				}
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
	
	public void includeACKResponse(final ACKMessage ackMessage) {
		synchronized (mutex) {
			if (isProcessingMessage())
				currentMessage.addACKMessage(ackMessage);
		}
		logger.trace("Peer " + peer.getPeerID() + " ACK message " + ackMessage + " included in current broadcasting message");
	}
}
