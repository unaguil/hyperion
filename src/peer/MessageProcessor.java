package peer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.messagecounter.MessageCounter;
import peer.peerid.PeerID;
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
	private final Map<BroadcastMessage, Boolean> includedMessages = new HashMap<BroadcastMessage, Boolean>();

	private final Random r = new Random();
	
	private final MessageCounter msgCounter;

	// stores the unprocessed messages when the thread is stopped
	private int unprocessedMessages = 0;

	private final ReliableBroadcast reliableBroadcast;
	
	private final AtomicBoolean delayNext = new AtomicBoolean(false);
	private final AtomicLong delayTime = new AtomicLong();
	
	private final static int DELAY = 10;
	
	// the queue used for storing received messages
	private final Deque<BroadcastMessage> messageDeque = new ArrayDeque<BroadcastMessage>(); 

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
			randomSleep();
			
			applyNextMessageDelay();
			
			processAllReceivedMessages();
			
			processResponses();
		}

		finishThread();
	}

	private void randomSleep() {
		final int neighbors = peer.getDetector().getCurrentNeighbors().size(); 
		final long randomWait = r.nextInt((neighbors + 1) * DELAY) + 1;				
		try {
			Thread.sleep(randomWait);
		} catch (InterruptedException e) {
			finishThread();
		}
	}

	private void applyNextMessageDelay() {
		do {				
			if (delayNext.get()) {
				delayNext.set(false);
				try {
					Thread.sleep(delayTime.get());
				} catch (InterruptedException e) {
					finishThread();
				}
			}
		} while (delayNext.get());
	}

	private void processResponses() {
		final List<BroadcastMessage> bundleMessages = new ArrayList<BroadcastMessage>();
		final PeerIDSet destinations = new PeerIDSet();
		
		synchronized (waitingMessages) {
			if (!waitingMessages.isEmpty())	 {								
				for (final BroadcastMessage broadcastMessage : waitingMessages) {
					destinations.addPeers(broadcastMessage.getExpectedDestinations());
					
					if (includedMessages.get(broadcastMessage).booleanValue())
						bundleMessages.add(broadcastMessage);
				}
				
				waitingMessages.clear();
				includedMessages.clear();					
			}
		}

		if (!bundleMessages.isEmpty()) {	
			final BundleMessage bundleMessage = new BundleMessage(peer.getPeerID(), new ArrayList<PeerID>(destinations.getPeerSet()), bundleMessages);
			msgCounter.addSent(bundleMessage.getClass());
		
			if (Peer.USE_RELIABLE_BROADCAST)
				reliableBroadcast.broadcast(bundleMessage);
			else
				peer.broadcast(bundleMessage);
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

	public boolean addResponse(final BroadcastMessage message, CommunicationLayer layer) {
		boolean includedResponse = false;
		synchronized (waitingMessages) {
			includedResponse = layer.checkWaitingMessages(Collections.unmodifiableList(waitingMessages), message);
			
			if (includedResponse)
				includedMessages.put(message, Boolean.TRUE);
			else
				includedMessages.put(message, Boolean.FALSE);
			
			waitingMessages.add(message);
		}
		return includedResponse;
	}

	public void addACKResponse(final ACKMessage ackMessage) {
		reliableBroadcast.addACKResponse(ackMessage);
	}

	public void sendACKMessage(final BroadcastMessage broadcastMessage) {
		final long time = broadcastMessage.getExpectedDestinations().size() * BasicPeer.ACK_TRANSMISSION_TIME + BasicPeer.ACK_TRANSMISSION_TIME;
		delayNextMessage(time);
		
		reliableBroadcast.sendACKMessage(broadcastMessage, msgCounter);
	}
	
	private void delayNextMessage(long time) {
		delayTime.set(time);
		delayNext.set(true); 
	}
}
