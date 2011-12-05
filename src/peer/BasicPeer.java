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
import java.util.zip.Deflater;

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
import peer.messagecounter.TotalMessageCounter;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import util.logger.Logger;
import config.Configuration;
import detection.NeighborDetector;
import detection.NeighborEventsListener;
import detection.beaconDetector.BeaconDetector;
import detection.message.BeaconMessage;

public final class BasicPeer implements Peer, NeighborEventsListener {

	public static final int CLEAN_REC_MSGS = 5000;

	// List of registered communication layers
	private final List<CommunicationLayer> communicationLayers = new CopyOnWriteArrayList<CommunicationLayer>();
	
	// The instance of the message counter to be used.
	private final MessageCounter msgCounter = new MessageCounter();

	private final MessageProcessor messageProcessor = new MessageProcessor(this, msgCounter);

	private final Random r = new Random();

	// used to discard already received messages
	private final ConditionRegister<MessageID> receivedMessages = new ConditionRegister<MessageID>(CLEAN_REC_MSGS);

	// A map containing entries per message class referencing listeners for the
	// received messages
	private final ConcurrentHashMap<Class<? extends BroadcastMessage>, MessageReceivedListener> receivingListenersTable = new ConcurrentHashMap<Class<? extends BroadcastMessage>, MessageReceivedListener>();

	// A list containing the listeners which will be notified when the
	// DatagramSocket sent() method is sent.
	private final List<MessageSentListener> messageSentListeners = new CopyOnWriteArrayList<MessageSentListener>();

	// the communication peer
	private final CommProvider commProvider;

	// Sets the message hearing listener
	private MessageReceivedListener hearListener = null;

	// Receiving thread used for incoming messages
	private ReceivingThread receivingThread;

	// Default neighbor detector
	private NeighborDetector detector;

	// the peer id
	private PeerID peerID;

	private boolean initialized = false;

	private int DELAYED_INIT = 0;

	private final Logger logger = Logger.getLogger(BasicPeer.class);

	// Default reception buffer length
	public static final int TRANSMISSION_TIME = 10;

	public static final int ACK_TRANSMISSION_TIME = 10;

	/**
	 * Constructor of the class. It is the default constructor which configures
	 * all internal properties.
	 */
	public BasicPeer(final CommProvider commProvider) {
		this.commProvider = commProvider;
	}

	private synchronized boolean isInitialized() {
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

		commProvider.initComm();

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

	private class DelayedRandomInit extends Thread {

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

		detector = new BeaconDetector(this, msgCounter);
		
		detector.addNeighborListener(this);

		// Initialize all layers
		for (final CommunicationLayer layer : communicationLayers)
			layer.init();

		final DelayedRandomInit delayedRandomInit = new DelayedRandomInit(this);
		delayedRandomInit.start();
	}
	
	private byte[] compress(byte[] data) {
		// Create the compressor with highest level of compression
		Deflater compressor = new Deflater();
		compressor.setLevel(Deflater.BEST_COMPRESSION);

		// Give the compressor the data to compress
		compressor.setInput(data);
		compressor.finish();

		// Create an expandable byte array to hold the compressed data.
		// You cannot use an array that's the same size as the orginal because
		// there is no guarantee that the compressed data will be smaller than
		// the uncompressed data.
		ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);

		// Compress the data
		byte[] buf = new byte[1024];
		while (!compressor.finished()) {
		    int count = compressor.deflate(buf);
		    bos.write(buf, 0, count);
		}
		try {
		    bos.close();
		} catch (IOException e) {
		}

		// Get the compressed data
		return bos.toByteArray();
	}

	@Override
	public void broadcast(final BroadcastMessage message) {
		try {
			logger.debug("Peer " + peerID + " broadcasting " + message);
			msgCounter.addBroadcasted(message.getClass());

			// Message is converted to byte array
			final byte[] data = toByteArray(message);
			
			//compress data
			final byte [] compressed = compress(data);
			
			msgCounter.addMessageSize(compressed.length);

			commProvider.broadcast(compressed);

			// Notify registered listeners
			notifySentListeners(message);
		} catch (final IOException ioe) {
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
		
		// Communication layers are stopped in reverse order of initialization
		logger.trace("Peer " + peerID + " stopping communication layers");
		Collections.reverse(communicationLayers);
		for (final CommunicationLayer layer : communicationLayers)
			layer.stop();
		
		logger.trace("Peer " + peerID + " communication layers stopped");
		
		//stop receiving thread
		logger.trace("Peer " + peerID + " stopping receiving thread");
		receivingThread.stopAndWait();

		// stop received messages table thread
		logger.trace("Peer " + peerID + " stopping received messages thread");
		receivedMessages.stopAndWait();
		
		// Stop message processor
		logger.trace("Peer " + peerID + " stopping message processor thread");
		messageProcessor.stopAndWait();

		try {
			logger.trace("Peer " + peerID + " finalizing communication provider");
			commProvider.stopComm();
		} catch (final IOException e) {
			logger.error("Peer " + peerID + " had problem finalizing communication " + e.getMessage());
		}
	}

	private void messageReceived(final BroadcastMessage broadcastMessage) {
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
			messageProcessor.receive(broadcastMessage);
		} else
			logger.trace("Peer " + peerID + " discarded " + broadcastMessage + " because it was already received.");
	}

	@Override
	public void printStatistics() {
		TotalMessageCounter.logStatistics();
		ReliableBroadcastTotalCounter.logStatistics();

		logger.info("Simulation finished");
	}

	private void processSimpleMessage(final BroadcastMessage broadcastMessage) {
		//beacon messages are not further processed
		if (broadcastMessage instanceof BeaconMessage)
			return;
		
		// received ACK messages are processed
		if (broadcastMessage instanceof ACKMessage) {
			logger.trace("Peer " + peerID + " adding ACK message " + broadcastMessage);
			ACKMessage ackMessage = (ACKMessage) broadcastMessage;
			msgCounter.addReceived(ackMessage.getClass());
			messageProcessor.addACKResponse(ackMessage);
			return;
		}

		//messages which does not have this node as destination are discarded
		if (!broadcastMessage.getExpectedDestinations().contains(peerID))
			return;
		
		//all received messages are responded with ACK
		sendACKMessage(broadcastMessage);
		
		// previously received messages are not processed again
		if (receivedMessages.contains(broadcastMessage.getMessageID()))
			return;

		receivedMessages.addEntry(broadcastMessage.getMessageID());

		messageReceived(broadcastMessage);
	}

	private void processBundleMessage(final BundleMessage bundleMessage) {
		msgCounter.addReceived(bundleMessage.getClass());
		
		// bundle messages containing only beacons are not further processed
		if (ReliableBroadcast.containsOnlyBeaconMessages(bundleMessage))
			return;
		
		//messages which does not have this node as destination are discarded
		if (!bundleMessage.getExpectedDestinations().contains(peerID))
			return;
		
		//all received messages are responded with ACK
		sendACKMessage(bundleMessage);
		
		// previously received messages are not processed again
		if (receivedMessages.contains(bundleMessage.getMessageID()))
			return;

		receivedMessages.addEntry(bundleMessage.getMessageID());

		for (final BroadcastMessage broadcastMessage : bundleMessage.getMessages())
			messageReceived(broadcastMessage);
	}

	private void sendACKMessage(final BroadcastMessage broadcastMessage) {
		if (USE_RELIABLE_BROADCAST)
			messageProcessor.sendACKMessage(broadcastMessage);
	}

	// Used by the message processor to process each dequeued message
	public void processMessage(final BroadcastMessage message) {
		logger.trace("Peer " + peerID + " processing message " + message);

		// Check if message is a multicast message and is directed to this node
		if (message instanceof MulticastMessage) {
			final MulticastMessage multicastMessage = (MulticastMessage) message;
			// Check if this peer is a receptor of this message
			if (!multicastMessage.getDestNeighbors().contains(peerID))
				return;
		}

		// Notify registered receiving listener
		notifyReceivingListener(message, System.currentTimeMillis());
	}

	private void notifyReceivingListener(final BroadcastMessage message, final long receptionTime) {
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
		logger.debug("Peer " + peerID + " received packet " + message + " from node " + message.getSender());
		msgCounter.addReceivedPacket(message.getClass());

		// Notify hear listeners indicating that a message was received
		notifyHearListener(message, System.currentTimeMillis());

		// Notify message reception to listeners
		if (message instanceof BundleMessage)
			processBundleMessage((BundleMessage) message);
		else
			processSimpleMessage(message);
	}

	private void notifyHearListener(final BroadcastMessage message, final long receptionTime) {
		if (hearListener != null)
			hearListener.messageReceived(message, receptionTime);
	}

	public CommProvider getCommProvider() {
		return commProvider;
	}

	@Override
	public PeerID getPeerID() {
		return peerID;
	}

	@Override
	public void appearedNeighbors(PeerIDSet neighbors) {}

	@Override
	public void dissapearedNeighbors(PeerIDSet neighbors) {
		for (MessageID messageID : receivedMessages.getEntries()) {
			if (neighbors.contains(messageID.getPeer())) {
				logger.trace("Peer " + peerID + " removing all messages received from neighbor " + messageID.getPeer());
				receivedMessages.remove(messageID);
			}
		}
	}
}
