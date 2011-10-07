package peer;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import message.BroadcastMessage;
import message.BundleMessage;
import message.MessageID;
import peer.conditionRegister.ConditionRegister;
import peer.counter.MessageCounter;
import peer.message.ACKMessage;
import peer.message.MulticastMessage;
import peer.reliableCounter.ReliableBroadcastTotalCounter;
import util.logger.Logger;
import config.Configuration;
import detection.NeighborDetector;
import detection.beaconDetector.BeaconDetector;
import detection.message.BeaconMessage;

/**
 * This class provides a extension of the PeerAgentJ which includes broadcasting
 * control.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public abstract class BasicPeer extends PeerAgentJ implements Peer {

	// Directory used to output information
	protected static final String TEMP_DIR = "tmp";

	private static final int CLEAN_REC_MSGS = 5000;

	// List of registered communication layers
	private final List<CommunicationLayer> communicationLayers = new CopyOnWriteArrayList<CommunicationLayer>();

	// Default neighbor detector
	private final NeighborDetector detector = new BeaconDetector(this);

	private final MessageProcessor messageProcessor = new MessageProcessor(this);

	private final Random r = new Random();

	// used to discard already received messages
	private final ConditionRegister<MessageID> receivedMessages = new ConditionRegister<MessageID>(CLEAN_REC_MSGS);

	private final Logger myLogger = Logger.getLogger(BasicPeer.class);

	private int DELAYED_INIT = 0;

	private int ACK_RANDOM_TIME = 100;
	
	public static final int TRANSMISSION_TIME = 40;

	/**
	 * Constructor of the class. It is the default constructor which configures
	 * all internal properties.
	 */
	public BasicPeer() {
		super(new MessageCounter());
	}

	/**
	 * Registers a new communication layer which will be notified when receiving
	 * those messages compatible with the specified classes.
	 * 
	 * @param layer
	 *            the communication layer to add.
	 * @param messageClasses
	 *            a set containing those message classes to register
	 */
	@Override
	public void addCommunicationLayer(final CommunicationLayer layer, final Set<Class<? extends BroadcastMessage>> messageClasses) throws RegisterCommunicationLayerException {
		// Check if layers were already initialized
		if (isInitialized())
			throw new RegisterCommunicationLayerException("Peer was already initialized. New layers cannot be added.");

		try {
			for (final Class<? extends BroadcastMessage> messageClass : messageClasses)
				addReceivingListener(messageClass, layer);
			communicationLayers.add(layer);
		} catch (final AlreadyRegisteredListenerException arle) {
			throw new RegisterCommunicationLayerException(arle);
		}
	}

	/**
	 * Gets the used neighbor detector.
	 * 
	 * @return the used neighbor detector.
	 */
	@Override
	public NeighborDetector getDetector() {
		return detector;
	}

	@Override
	public void enqueueBroadcast(final BroadcastMessage message) {
		messageProcessor.addResponse(message);
		myLogger.debug("Peer " + this.getPeerID() + " sending " + message.getType() + " " + message.getMessageID());
		getMessageCounter().addSent(message.getClass());
	}

	// Delegated methods
	public abstract void loadData();

	public abstract void printOutputs();

	// Checks if temporal output directory exists
	protected boolean tmpDirExists() {
		final File tmp = new File(TEMP_DIR);
		return tmp.exists();
	}

	class DelayedRandomInit extends Thread {

		private final BasicPeer peer;

		public DelayedRandomInit(final BasicPeer peer) {
			this.peer = peer;
		}

		@Override
		public void run() {
			if (DELAYED_INIT > 0) {
				final int initDelay = r.nextInt(DELAYED_INIT);
				myLogger.info("Peer " + peer.getPeerID() + " initialization has been delayed " + initDelay + " ms");

				try {
					Thread.sleep(initDelay);
				} catch (final InterruptedException e) {
					return;
				}
			}

			// Initialize all layers
			for (final CommunicationLayer layer : communicationLayers)
				layer.init();

			// Load peer data
			loadData();

			receivedMessages.start();

			// Starts message processing thread
			messageProcessor.init();

			initialize();
		}
	}

	@Override
	protected void init() {
		try {
			final String delayedInitStr = Configuration.getInstance().getProperty("basicPeer.delayedInit");

			DELAYED_INIT = Integer.parseInt(delayedInitStr);
		} catch (final Exception e) {
			myLogger.error("Peer " + getPeerID() + " had problem loading configuration: " + e.getMessage());
		}
		
		try {
			final String rebroadcastTime = Configuration.getInstance().getProperty("reliableBroadcast.rebroadcastTime");
			ACK_RANDOM_TIME  = Math.round(Integer.parseInt(rebroadcastTime) / 2.0f);
			myLogger.info("Peer " + getPeerID() + " set ACK_RANDOM_TIME to " + ACK_RANDOM_TIME);
		} catch (final Exception e) {
			myLogger.error("Peer " + getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		final DelayedRandomInit delayedRandomInit = new DelayedRandomInit(this);
		delayedRandomInit.start();
	}

	@Override
	protected void stop() {
		// stop received messages table thread
		receivedMessages.stopAndWait();

		// Stop message processor
		messageProcessor.stopAndWait();

		if (Logger.TRACE)
			myLogger.trace("Peer " + this.getPeerID() + " unprocessed messages: " + messageProcessor.getUnprocessedMessages());

		// Communication layers are stopped in reverse order of initialization
		Collections.reverse(communicationLayers);
		for (final CommunicationLayer layer : communicationLayers)
			layer.stop();

		// Check if temporal directory exists and print outputs
		if (tmpDirExists())
			printOutputs();

		super.stop();
	}

	@Override
	protected void messageReceived(final BroadcastMessage broadcastMessage, final long receptionTime) {
		getMessageCounter().addReceived(broadcastMessage.getClass());
		if (Logger.TRACE)
			myLogger.trace("Peer " + this.getPeerID() + " received " + broadcastMessage + " from node " + broadcastMessage.getSender() + " using broadcast");
		myLogger.debug("Peer " + this.getPeerID() + " received " + broadcastMessage.getType() + " " + broadcastMessage.getMessageID() + " from node " + broadcastMessage.getSender());

		if (broadcastMessage instanceof BeaconMessage)
			return;

		// if message was not already received
		if (!receivedMessages.contains(broadcastMessage.getMessageID())) {
			// save new messages
			receivedMessages.addEntry(broadcastMessage.getMessageID());

			// Put the message into the blocking queue for processing
			messageProcessor.enqueue(broadcastMessage);
		} else if (Logger.TRACE)
			myLogger.trace("Peer " + this.getPeerID() + " discarded " + broadcastMessage + " because it was already received.");
	}

	@Override
	protected void printStatistics() {
		ReliableBroadcastTotalCounter.logStatistics();

		myLogger.info("Simulation finished");
	}

	@Override
	protected void processSimpleMessage(final BroadcastMessage broadcastMessage) {	
		// received ACK messages are processed
		if (broadcastMessage instanceof ACKMessage) {
			if (Logger.TRACE)
				myLogger.trace("Peer " + this.getPeerID() + " adding ACK message " + broadcastMessage);
			messageProcessor.addACKResponse((ACKMessage) broadcastMessage);
			return;
		}
		
		if (!broadcastMessage.getExpectedDestinations().isEmpty() && !broadcastMessage.getExpectedDestinations().contains(getPeerID()))
			return;

		if (!(broadcastMessage instanceof BeaconMessage))
			sendACKMessage(broadcastMessage);

		messageReceived(broadcastMessage, System.currentTimeMillis());
	}

	@Override
	protected void processBundleMessage(final BundleMessage bundleMessage) {
		// previously received bundle messages are not processed again
		if (receivedMessages.contains(bundleMessage.getMessageID()))
			return;

		receivedMessages.addEntry(bundleMessage.getMessageID());
		
		if (!bundleMessage.getExpectedDestinations().isEmpty() && !bundleMessage.getExpectedDestinations().contains(getPeerID()))
			return;

		// beacon messages are not responded with ACK
		if (!ReliableBroadcast.containsOnlyBeaconMessages(bundleMessage))
			sendACKMessage(bundleMessage);

		for (final BroadcastMessage broadcastMessage : bundleMessage.getMessages())
			messageReceived(broadcastMessage, System.currentTimeMillis());
	}
	
	private class ACKSender extends Thread {
		
		private final BroadcastMessage broadcastMessage;
		private final int ackRandomTime;
		
		public ACKSender(BroadcastMessage broadcastMessage, int ackRandomTime) {
			this.broadcastMessage = broadcastMessage;
			this.ackRandomTime = ackRandomTime;
		}
		
		@Override
		public void run() {
			try {
				int sleepTime = r.nextInt(ackRandomTime);
				Thread.sleep(sleepTime);
				messageProcessor.sendACKMessage(broadcastMessage);
			} catch (InterruptedException e) {}
		}
	}

	private void sendACKMessage(final BroadcastMessage broadcastMessage) {
		if (USE_RELIABLE_BROADCAST) {
			if (ACK_RANDOM_TIME > 0) {
				final ACKSender ackSender = new ACKSender(broadcastMessage, ACK_RANDOM_TIME);
				ackSender.start();
			} else {
				messageProcessor.sendACKMessage(broadcastMessage);
			}
		}
	}

	// Used by the message processor to process each dequeued message
	@Override
	public void processMessage(final BroadcastMessage message) {
		if (Logger.TRACE)
			myLogger.trace("Peer " + this.getPeerID() + " processing message " + message);

		// Check if message is a multicast message and is directed to this node
		if (message instanceof MulticastMessage) {
			final MulticastMessage multicastMessage = (MulticastMessage) message;
			// Check if this peer is a receptor of this message
			if (!multicastMessage.getDestinations().contains(this.getPeerID()))
				return;
		}

		// Notify registered receiving listener
		notifyReceivingListener(message, System.currentTimeMillis());
	}
}
