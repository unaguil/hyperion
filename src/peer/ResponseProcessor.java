package peer;

import java.util.ArrayList;
import java.util.Collections;
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
final class ResponseProcessor extends WaitableThread {

	// the communication peer
	private final BasicPeer peer;
	
	private final Logger logger = Logger.getLogger(ResponseProcessor.class);

	private final List<BroadcastMessage> waitingResponses = new ArrayList<BroadcastMessage>();

	private final Random r = new Random();
	
	private final MessageCounter msgCounter;
		
	private ReliableBroadcast reliableBroadcast = null;
	private final Object mutex = new Object();
	
	static final int MAX_JITTER = 10;

	/**
	 * Constructor of the message processor
	 * 
	 * @param peer
	 *            the communication peer
	 */
	public ResponseProcessor(final BasicPeer peer, final MessageCounter msgCounter) {
		this.peer = peer;
		this.msgCounter = msgCounter;
	}

	@Override
	public void run() {		
		while (!Thread.interrupted()) {			
			randomSleep(); 
			
			if (!Thread.interrupted()) {						
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
		final long randomWait = peer.getFixedWaitTime() - r.nextInt(MAX_JITTER);				
		try {
			Thread.sleep(randomWait);
		} catch (InterruptedException e) {
			interrupt();
		}
	}

	public BundleMessage processResponses() {
		final List<BroadcastMessage> bundleMessages = new ArrayList<BroadcastMessage>();
		
		synchronized (waitingResponses) {
			if (!waitingResponses.isEmpty())	 {								
				for (final BroadcastMessage broadcastMessage : waitingResponses)					
					bundleMessages.add(broadcastMessage);
				
				waitingResponses.clear();					
			}
		}

		return new BundleMessage(peer.getPeerID(), bundleMessages);
	}

	private void sendResponses(final BundleMessage bundleMessage) {
		msgCounter.addSent(bundleMessage.getClass());

		synchronized (mutex) {
			reliableBroadcast = new ReliableBroadcast(peer, this);
		}
		
		reliableBroadcast.broadcast(bundleMessage);
		
		synchronized (mutex) {
			reliableBroadcast = null;
		}
	}

	public boolean addResponse(final BroadcastMessage message, CommunicationLayer layer) {
		synchronized (waitingResponses) {
			if (layer != null) {
				final BroadcastMessage duplicatedMessage = layer.isDuplicatedMessage(Collections.unmodifiableList(waitingResponses), message);
				if (duplicatedMessage == null) {
					waitingResponses.add(message);				
					return true;
				}
				
				duplicatedMessage.addExpectedDestinations(message.getExpectedDestinations());
				return false;
			}
			
			waitingResponses.add(message);				
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
