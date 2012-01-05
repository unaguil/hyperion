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
	
	// the queue used for storing received messages
	private final Deque<BroadcastMessage> messageDeque = new ArrayDeque<BroadcastMessage>();
	
	private ReliableBroadcast reliableBroadcast = null;
	private final Object mutex = new Object();
	
	private final DelayAdjuster delayAdjuster;

	/**
	 * Constructor of the message processor
	 * 
	 * @param peer
	 *            the communication peer
	 */
	public MessageProcessor(final BasicPeer peer, final MessageCounter msgCounter, final DelayAdjuster delayAdjuster) {
		this.peer = peer;
		this.msgCounter = msgCounter;
		this.delayAdjuster = delayAdjuster;
	}

	public void init() {
		start();
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
		while (!Thread.interrupted()) {			
			randomSleep(); 
			
			if (!Thread.interrupted()) {			
				processAllReceivedMessages();
			
				BundleMessage bundleMessage = processResponses();
				if (!bundleMessage.getMessages().isEmpty())
					sendResponses(bundleMessage);
				
			} else
				interrupt();
		}

		logger.trace("Peer " + peer.getPeerID() + " message processor finalized");
		threadFinished();
	}

	private void randomSleep() { 
		final long randomWait = r.nextInt(delayAdjuster.getCurrentMaxDelay());				
		try {
			WaitableThread.mySleep(randomWait);
		} catch (InterruptedException e) {
			interrupt();
		}
	}

	public BundleMessage processResponses() {
		final List<BroadcastMessage> bundleMessages = new ArrayList<BroadcastMessage>();
		
		synchronized (waitingMessages) {
			if (!waitingMessages.isEmpty())	 {								
				for (final BroadcastMessage broadcastMessage : waitingMessages)					
					bundleMessages.add(broadcastMessage);
				
				waitingMessages.clear();					
			}
		}

		return new BundleMessage(peer.getPeerID(), bundleMessages);
	}

	private void sendResponses(final BundleMessage bundleMessage) {
		msgCounter.addSent(bundleMessage.getClass());

		synchronized (mutex) {
			reliableBroadcast = new ReliableBroadcast(peer, this, delayAdjuster);
		}
		
		reliableBroadcast.broadcast(bundleMessage);
		
		synchronized (mutex) {
			reliableBroadcast = null;
		}
	}

	public void processAllReceivedMessages() {
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

	/**
	 * Cancels the execution of the message processor thread
	 * 
	 * @return the number of unprocessed messages
	 */
	@Override
	public void stopAndWait() {		
		super.stopAndWait();
	}

	public int getUnprocessedMessages() {
		return unprocessedMessages;
	}

	public boolean addResponse(final BroadcastMessage message, CommunicationLayer layer) {
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

	public void addReceivedACKResponse(final ACKMessage ackMessage) {
		synchronized (mutex) {
			if (reliableBroadcast != null)
				reliableBroadcast.addReceivedACKResponse(ackMessage);
		}
	}
}
