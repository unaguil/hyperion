package peer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.messagecounter.MessageCounter;
import peer.peerid.PeerIDSet;
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
final class MessageProcessor extends WaitableThread {

	// the communication peer
	private final BasicPeer peer;
	
	private final Logger logger = Logger.getLogger(MessageProcessor.class);

	private final List<BroadcastMessage> waitingMessages = new ArrayList<BroadcastMessage>();

	private final Random r = new Random();
	
	private final MessageCounter msgCounter;

	// stores the unprocessed messages when the thread is stopped
	private int unprocessedMessages = 0;

	private final ReliableBroadcast reliableBroadcast;
	
	// the queue used for storing received messages
	private final Deque<BroadcastMessage> messageDeque = new ArrayDeque<BroadcastMessage>();
	
	private static final int MAX_JITTER = 25;

	/**
	 * Constructor of the message processor
	 * 
	 * @param peer
	 *            the communication peer
	 */
	public MessageProcessor(final BasicPeer peer, MessageCounter msgCounter) {
		this.peer = peer;
		this.reliableBroadcast = new ReliableBroadcast(peer);
		this.msgCounter = msgCounter;
	}

	public void init() {
		start();

		reliableBroadcast.start();
	}

	/**
	 * Enqueues a new message for future processing
	 * 
	 * @param message
	 *            the message to enqueue
	 */
	public void receive(final BroadcastMessage message) {
		synchronized (messageDeque) {
			messageDeque.add(message);
		}
	} 

	@Override
	public void run() {
		// Processor loop
		while (!Thread.interrupted()) {									
			processAllReceivedMessages();
			
			processResponses();
			
			Thread.yield();
		}

		finishThread();
	}

	private void applyJitter() { 
		final long jitter = r.nextInt(MAX_JITTER);		
		if (jitter > 0) {
			try {
				Thread.sleep(jitter);
			} catch (InterruptedException e) {
				finishThread();
			}
		}
	}

	private void processResponses() {
		final List<BroadcastMessage> bundleMessages = new ArrayList<BroadcastMessage>();
		final PeerIDSet destinations = new PeerIDSet();
		
		synchronized (waitingMessages) {
			if (!waitingMessages.isEmpty())	 {								
				for (final BroadcastMessage broadcastMessage : waitingMessages) {
					destinations.addPeers(broadcastMessage.getExpectedDestinations());
					
					bundleMessages.add(broadcastMessage);
				}
				
				waitingMessages.clear();					
			}
		}

		if (!bundleMessages.isEmpty()) {	
			final BundleMessage bundleMessage = new BundleMessage(peer.getPeerID(), destinations.getPeerSet(), bundleMessages);
			
			applyJitter();
		
			reliableBroadcast.broadcast(bundleMessage);
			
			msgCounter.addSent(bundleMessage.getClass());
		}
	}

	private void processAllReceivedMessages() {
		final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>();						
		synchronized (messageDeque) {					
			while (!messageDeque.isEmpty()) {
				final BroadcastMessage message = messageDeque.poll();
				messages.add(message);
			}
		}
		
		for (final BroadcastMessage message : messages)
			peer.processMessage(message);
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
		reliableBroadcast.stopAndWait();
		
		super.stopAndWait();
	}

	public int getUnprocessedMessages() {
		return unprocessedMessages;
	}

	public boolean enqueueResponse(final BroadcastMessage message, CommunicationLayer layer) {
		synchronized (waitingMessages) {
			if (layer != null) {
				final BroadcastMessage duplicatedMessage = layer.isDuplicatedMessage(Collections.unmodifiableList(waitingMessages), message);
				if (duplicatedMessage == null) {
					waitingMessages.add(message);				
					return true;
				}
				
				duplicatedMessage.addExpectedDestinations(message.getExpectedDestinations());
				return false;
			}
			
			waitingMessages.add(message);				
			return true;
		}
	}

	public void addACKResponse(final ACKMessage ackMessage) {
		reliableBroadcast.addACKResponse(ackMessage);
	}

	public boolean isSendingMessage() {
		return reliableBroadcast.isProcessingMessage();
	}
}
