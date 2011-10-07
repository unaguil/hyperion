package peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import peer.conditionregister.ConditionRegister;
import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import peer.message.MessageID;
import peer.message.MessageReceivedListener;
import peer.message.MessageSentListener;
import peer.message.MulticastMessage;
import peer.messagecounter.MessageCounter;
import peer.messagecounter.ReliableBroadcastTotalCounter;
import peer.peerid.PeerID;
import config.Configuration;
import detection.NeighborDetector;
import detection.beaconDetector.BeaconDetector;
import detection.message.BeaconMessage;

public class BasicPeer implements Peer {

	private static final int CLEAN_REC_MSGS = 5000;

	// List of registered communication layers
	private final List<CommunicationLayer> communicationLayers = new CopyOnWriteArrayList<CommunicationLayer>();

	private final MessageProcessor messageProcessor = new MessageProcessor(this);

	private final Random r = new Random();

	// used to discard already received messages
	private final ConditionRegister<MessageID> receivedMessages = new ConditionRegister<MessageID>(CLEAN_REC_MSGS);
	
	// A map containing entries per message class referencing listeners for the
	// received messages
	private final ConcurrentHashMap<Class<? extends BroadcastMessage>, MessageReceivedListener> receivingListenersTable = new ConcurrentHashMap<Class<? extends BroadcastMessage>, MessageReceivedListener>();
	
	// A list containing the listeners which will be notified when the
	// DatagramSocket sent() method is sent.
	private final List<MessageSentListener> messageSentListeners = new CopyOnWriteArrayList<MessageSentListener>(); 

	//the communication peer
	private PeerBehavior peerBehavior;
	
	// The instance of the message counter to be used.
	private final MessageCounter msgCounter = new MessageCounter();
		
	// Sets the message hearing listener
	private MessageReceivedListener hearListener = null;
		
	// Receiving thread used for incoming messages
	private ReceivingThread receivingThread;
	
	// Default neighbor detector
	private NeighborDetector detector;
	
	//the peer id
	private PeerID peerID;

	private boolean initialized = false;

	private int DELAYED_INIT = 0;
	
	private final Logger logger = Logger.getLogger(BasicPeer.class);
	
	// Default reception buffer length
	public static final int TRANSMISSION_TIME = 40;


	/**
	 * Constructor of the class. It is the default constructor which configures
	 * all internal properties.
	 */
	public BasicPeer(PeerBehavior peerBehavior) {
		this.peerBehavior = peerBehavior;
	}
	
	public synchronized boolean isInitialized() {
		return initialized;
	}

	private synchronized void initialize() {
		initialized = true;
	}

	private synchronized void unitialize() {
		initialized = false;
	}

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
	
	// Initializes the peer
	@Override
	public void initPeer(final PeerID id) throws IOException {
		this.peerID = id;
		
		logger.trace("Peer " + peerID + " initializing");
		
		init();
		
		peerBehavior.init();
		
		receivingThread = new ReceivingThread(this);
		receivingThread.start();
		logger.trace("Peer " + peerID + " starts receiving");
	}
	
	/**
	 * Adds a message receiving listener which will be called every time a
	 * message of the specified class is received by the node. Only one listener
	 * can be registered to each message class.
	 * 
	 * @param messageClass
	 *            the class used to filter incoming messages
	 * @param receivedListener
	 *            the listener to be registered
	 * @throws AlreadyRegisteredListenerException
	 *             this exception is thrown if the message class was previously
	 *             registered to another listener
	 */
	@Override
	public void addReceivingListener(final Class<? extends BroadcastMessage> messageClass, final MessageReceivedListener receivedListener) throws AlreadyRegisteredListenerException {
		if (receivingListenersTable.putIfAbsent(messageClass, receivedListener) != null)
			throw new AlreadyRegisteredListenerException(messageClass + " messages were already registered for class " + receivedListener.getClass().toString());
	}

	/**
	 * Sets the listener for message hearing. This listener is notified every
	 * time a message is received and not depends on the message class.
	 * 
	 * @param hearListener
	 *            the listener for heard messages
	 */
	@Override
	public void setHearListener(final MessageReceivedListener hearListener) {
		this.hearListener = hearListener;
	}

	/**
	 * Adds a new message sent listener which will be called every time a packet
	 * is sent.
	 * 
	 * @param sentListener
	 *            the message listener to be added.
	 */
	@Override
	public void addSentListener(final MessageSentListener sentListener) {
		messageSentListeners.add(sentListener);
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
		logger.debug("Peer " + peerID + " sending " + message.getType() + " " + message.getMessageID());
		msgCounter.addSent(message.getClass());
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
				logger.info("Peer " + peer.getPeerID() + " initialization has been delayed " + initDelay + " ms");

				try {
					Thread.sleep(initDelay);
				} catch (final InterruptedException e) {
					return;
				}
			}

			receivedMessages.start();

			// Starts message processing thread
			messageProcessor.init();

			initialize();
		}
	}

	private void init() {
		try {
			final String delayedInitStr = Configuration.getInstance().getProperty("basicPeer.delayedInit");
			if (delayedInitStr != null)
				DELAYED_INIT = Integer.parseInt(delayedInitStr);
		} catch (final Exception e) {
			logger.error("Peer " + peerID + " had problem loading configuration: " + e.getMessage());
		}
		
		detector = new BeaconDetector(this);
		
		// Initialize all layers
		for (final CommunicationLayer layer : communicationLayers)
			layer.init();

		final DelayedRandomInit delayedRandomInit = new DelayedRandomInit(this);
		delayedRandomInit.start();
	}
	
	@Override
	public void broadcast(final BroadcastMessage message) {
		try {
			logger.debug("Peer " + peerID + " sending " + message);
			msgCounter.addSent(message.getClass());

			// Message is converted to byte array
			final byte[] data = toByteArray(message);
			
			msgCounter.addMessageSize(data.length);
			
			peerBehavior.broadcast(data);

			// Notify registered listeners
			notifySentListeners(message);

		} catch (final IOException ioe) {
			ioe.printStackTrace();
			logger.error("Peer " + peerID + " broadcast error. " + ioe.getMessage());
		}
	}
	
	private void notifySentListeners(final BroadcastMessage message) {
		for (final MessageSentListener sentListenerListener : messageSentListeners)
			sentListenerListener.messageSent(message, System.currentTimeMillis());
	}
	
	// Converts an object to its byte array representation
	private byte[] toByteArray(final Object o) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutput out = new ObjectOutputStream(baos);
		out.writeObject(o);
		return baos.toByteArray();
	}

	@Override
	public void stopPeer() {
		unitialize();
		
		// stop received messages table thread
		receivedMessages.stopAndWait();

		// Stop message processor
		messageProcessor.stopAndWait();

		
		logger.trace("Peer " + peerID + " unprocessed messages: " + messageProcessor.getUnprocessedMessages());

		// Communication layers are stopped in reverse order of initialization
		Collections.reverse(communicationLayers);
		for (final CommunicationLayer layer : communicationLayers)
			layer.stop();
		
		logger.trace("Peer " + peerID + " stopping receiving thread");
		receivingThread.stopAndWait();
	}

	protected void messageReceived(final BroadcastMessage broadcastMessage) {
		msgCounter.addReceived(broadcastMessage.getClass());
		
		logger.trace("Peer " + peerID + " received " + broadcastMessage + " from node " + broadcastMessage.getSender() + " using broadcast");
		logger.debug("Peer " + peerID + " received " + broadcastMessage.getType() + " " + broadcastMessage.getMessageID() + " from node " + broadcastMessage.getSender());

		if (broadcastMessage instanceof BeaconMessage)
			return;

		// if message was not already received
		if (!receivedMessages.contains(broadcastMessage.getMessageID())) {
			// save new messages
			receivedMessages.addEntry(broadcastMessage.getMessageID());

			// Put the message into the blocking queue for processing
			messageProcessor.enqueue(broadcastMessage);
		} else 
			logger.trace("Peer " + peerID + " discarded " + broadcastMessage + " because it was already received.");
	}

	@Override
	public void printStatistics() {
		ReliableBroadcastTotalCounter.logStatistics();

		logger.info("Simulation finished");
	}

	protected void processSimpleMessage(final BroadcastMessage broadcastMessage) {	
		// received ACK messages are processed
		if (broadcastMessage instanceof ACKMessage) {
			
				logger.trace("Peer " + peerID + " adding ACK message " + broadcastMessage);
			messageProcessor.addACKResponse((ACKMessage) broadcastMessage);
			return;
		}
		
		if (!broadcastMessage.getExpectedDestinations().isEmpty() && !broadcastMessage.getExpectedDestinations().contains(peerID))
			return;

		if (!(broadcastMessage instanceof BeaconMessage))
			sendACKMessage(broadcastMessage);

		messageReceived(broadcastMessage);
	}

	protected void processBundleMessage(final BundleMessage bundleMessage) {
		// previously received bundle messages are not processed again
		if (receivedMessages.contains(bundleMessage.getMessageID()))
			return;

		receivedMessages.addEntry(bundleMessage.getMessageID());
		
		if (!bundleMessage.getExpectedDestinations().isEmpty() && !bundleMessage.getExpectedDestinations().contains(peerID))
			return;

		// beacon messages are not responded with ACK
		if (!ReliableBroadcast.containsOnlyBeaconMessages(bundleMessage))
			sendACKMessage(bundleMessage);

		for (final BroadcastMessage broadcastMessage : bundleMessage.getMessages())
			messageReceived(broadcastMessage);
	}

	private void sendACKMessage(final BroadcastMessage broadcastMessage) {
		if (USE_RELIABLE_BROADCAST) {
			messageProcessor.sendACKMessage(broadcastMessage);
		}
	}

	// Used by the message processor to process each dequeued message
	@Override
	public void processMessage(final BroadcastMessage message) {
		
			logger.trace("Peer " + peerID + " processing message " + message);

		// Check if message is a multicast message and is directed to this node
		if (message instanceof MulticastMessage) {
			final MulticastMessage multicastMessage = (MulticastMessage) message;
			// Check if this peer is a receptor of this message
			if (!multicastMessage.getDestinations().contains(peerID))
				return;
		}

		// Notify registered receiving listener
		notifyReceivingListener(message, System.currentTimeMillis());
	}
	
	protected void notifyReceivingListener(final BroadcastMessage message, final long receptionTime) {
		if (receivingListenersTable.containsKey(message.getClass())) {
			final MessageReceivedListener listener = receivingListenersTable.get(message.getClass());
			final long time = System.nanoTime();
			listener.messageReceived(message, receptionTime);
			final long elapsedTime = System.nanoTime() - time;
			msgCounter.addProcessTime(elapsedTime);
		}
	}
	
	public void processReceivedPacket(final BroadcastMessage message) {
		// messages are only processed if node is initialized
		logger.debug("Peer " + peerID + " received " + message + " from node " + message.getSender());
		msgCounter.addReceived(message.getClass());
		
		// Notify hear listeners indicating that a message was received
		notifyHearListener(message, System.currentTimeMillis());

		// Notify message reception to listeners
		if (message instanceof BundleMessage)
			processBundleMessage((BundleMessage) message);
		else
			processSimpleMessage(message);
	}
	
	protected void notifyHearListener(final BroadcastMessage message, final long receptionTime) {
		if (hearListener != null)
			hearListener.messageReceived(message, receptionTime);
	}

	public PeerBehavior getPeerBehavior() {
		return peerBehavior;
	}

	@Override
	public PeerID getPeerID() {
		return peerID;
	}
}
