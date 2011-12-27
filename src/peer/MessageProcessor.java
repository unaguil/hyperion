package peer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
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
	
	private final Logger logger = Logger.getLogger(MessageProcessor.class);

	private final List<BroadcastMessage> waitingMessages = new ArrayList<BroadcastMessage>();
	private final List<Boolean> includedMessages = new ArrayList<Boolean>();

	private final Random r = new Random();
	
	private final MessageCounter msgCounter;

	// stores the unprocessed messages when the thread is stopped
	private int unprocessedMessages = 0;

	private final ReliableBroadcast reliableBroadcast;
	
	private final AtomicBoolean delayNext = new AtomicBoolean(false);
	private final AtomicLong delayTime = new AtomicLong();
	
	// the queue used for storing received messages
	private final Deque<BroadcastMessage> messageDeque = new ArrayDeque<BroadcastMessage>(); 

	private int RANDOM_WAIT = 100;

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
		try {
			final String randomWaitStr = Configuration.getInstance().getProperty("messageProcessor.randomWait");
			final int value = Integer.parseInt(randomWaitStr);
			if (value > 0)
				RANDOM_WAIT = value;

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
	public void receive(final BroadcastMessage message) {
		synchronized (messageDeque) {
			messageDeque.add(message);
		}
	} 

	@Override
	public void run() {
		// Processor loop
		while (!Thread.interrupted()) {
			final long currentTime = System.currentTimeMillis();
			
			processAllReceivedMessages();
			
			final long randomWait = r.nextInt(RANDOM_WAIT) + 1;				
			try {
				Thread.sleep(randomWait);
			} catch (InterruptedException e) {
				finishThread();
			}
			
			do {				
				if (delayNext.get()) {
					delayNext.set(false);
					try {
						Thread.sleep(delayTime.get());
					} catch (InterruptedException e) {
						finishThread();
					}
				}
				
				processAllReceivedMessages();
			} while (delayNext.get());
			
			if (!waitingMessages.isEmpty())	 {
				logger.debug("Peer " + peer.getPeerID() + " slept during " + (System.currentTimeMillis() - currentTime));
				
				final List<BroadcastMessage> bundleMessages = new ArrayList<BroadcastMessage>();
				final PeerIDSet destinations = new PeerIDSet();
				
				synchronized (waitingMessages) {
					for (int i = 0; i < waitingMessages.size(); i++) {
						final BroadcastMessage broadcastMessage = waitingMessages.get(i);
						destinations.addPeers(broadcastMessage.getExpectedDestinations());
						
						if (includedMessages.get(i).booleanValue())
							bundleMessages.add(broadcastMessage);
					}
					
					waitingMessages.clear();
					includedMessages.clear();
				}

				final BundleMessage bundleMessage = new BundleMessage(peer.getPeerID(), new ArrayList<PeerID>(destinations.getPeerSet()), bundleMessages);
				msgCounter.addSent(bundleMessage.getClass());
				
				if (Peer.USE_RELIABLE_BROADCAST)
					reliableBroadcast.broadcast(bundleMessage);
				else
					peer.broadcast(bundleMessage);
				
			}
		}

		finishThread();
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
			includedResponse = layer.checkWaitingMessages(waitingMessages, message);
			
			if (includedResponse)
				includedMessages.add(Boolean.TRUE);
			else
				includedMessages.add(Boolean.FALSE);
			
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
