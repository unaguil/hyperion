package peer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import multicast.search.message.RemoteMulticastMessage;
import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.peerid.PeerIDSet;
import util.WaitableThread;
import util.logger.Logger;
import config.Configuration;

/**
 * This class implements a message processor used for decoupling it from the
 * receiving thread. This enables to continue receiving messages when the a
 * waiting action is being perform (i.e reliableBroadcast)
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
final class MessageProcessor extends WaitableThread {

	// the communication peer
	private final BasicPeer peer;

	// the queue used for storing received messages
	private final Deque<BroadcastMessage> messageDeque = new ArrayDeque<BroadcastMessage>();

	private final Logger logger = Logger.getLogger(MessageProcessor.class);

	private final List<BroadcastMessage> waitingMessages = new ArrayList<BroadcastMessage>();

	private final Random r = new Random();

	// stores the unprocessed messages when the thread is stopped
	private int unprocessedMessages = 0;

	private final ReliableBroadcast reliableBroadcast;

	private int RANDOM_WAIT = 200;

	/**
	 * Constructor of the message processor
	 * 
	 * @param peer
	 *            the communication peer
	 */
	public MessageProcessor(final BasicPeer peer) {
		this.peer = peer;

		this.reliableBroadcast = new ReliableBroadcast(peer);
	}

	public void init() {
		try {
			final String randomWaitStr = Configuration.getInstance().getProperty("messageProcessor.randomWait");
			RANDOM_WAIT = Integer.parseInt(randomWaitStr);
			logger.info("Peer " + peer.getPeerID() + " set RANDOM_WAIT to " + RANDOM_WAIT);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		start();

		reliableBroadcast.start();
	}

	/**
	 * Enqueues a new message for future processing
	 * 
	 * @param message
	 *            the message to enqueue
	 */
	public void enqueue(final BroadcastMessage message) {
		synchronized (messageDeque) {
			messageDeque.add(message);
		}
	}

	@Override
	public void run() {
		// Processor loop
		while (!Thread.interrupted()) {
			final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>();

			synchronized (messageDeque) {
				while (!messageDeque.isEmpty()) {
					final BroadcastMessage message = messageDeque.poll();
					messages.add(message);
				}
			}

			for (final BroadcastMessage message : messages)
				peer.processMessage(message);

			final List<BroadcastMessage> messagesToSend = new ArrayList<BroadcastMessage>();

			synchronized (waitingMessages) {
				messagesToSend.addAll(waitingMessages);
				waitingMessages.clear();
			}

			if (!messagesToSend.isEmpty()) {
				final List<BroadcastMessage> bundleMessages = new ArrayList<BroadcastMessage>();
				final PeerIDSet destinations = new PeerIDSet();

				for (final BroadcastMessage broadcastMessage : messagesToSend) {
					if (broadcastMessage instanceof RemoteMulticastMessage) {
						final RemoteMulticastMessage remoteMulticastMessage = (RemoteMulticastMessage) broadcastMessage;
						destinations.addPeers(remoteMulticastMessage.getThroughPeers());
					} else
						destinations.addPeers(peer.getDetector().getCurrentNeighbors());

					bundleMessages.add(broadcastMessage);
				}

				final long randomWait = r.nextInt(RANDOM_WAIT);
				try {
					Thread.sleep(randomWait);
				} catch (final InterruptedException e) {
					finishThread();
					return;
				}

				final BundleMessage bundleMessage = new BundleMessage(peer.getPeerID(), bundleMessages);
				bundleMessage.setExpectedDestinations(destinations.getPeerSet());

				if (Peer.USE_RELIABLE_BROADCAST)
					reliableBroadcast.broadcast(bundleMessage);
				else
					peer.broadcast(bundleMessage);
			} else
				Thread.yield();
		}

		finishThread();
	}

	private void finishThread() {
		logger.trace("Peer " + peer.getPeerID() + " message processor finalized");

		this.threadFinished();
	}

	/**
	 * Cancels the execution of the message processor thread
	 * 
	 * @return the number of unprocessed messages
	 */
	@Override
	public void stopAndWait() {
		super.stopAndWait();

		synchronized (messageDeque) {
			unprocessedMessages = messageDeque.size();
		}
	}

	public int getUnprocessedMessages() {
		return unprocessedMessages;
	}

	public boolean contains(final BroadcastMessage broadcastMessage) {
		return messageDeque.contains(broadcastMessage);
	}

	public void addResponse(final BroadcastMessage message) {
		synchronized (waitingMessages) {
			waitingMessages.add(message);
		}
	}

	public void addACKResponse(final ACKMessage ackMessage) {
		reliableBroadcast.addACKResponse(ackMessage);
	}

	public void sendACKMessage(final BroadcastMessage broadcastMessage) {
		reliableBroadcast.sendACKMessage(broadcastMessage);
	}
}