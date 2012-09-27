/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package peer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import peer.message.BroadcastMessage;
import peer.message.MessageReceivedListener;
import peer.message.MessageSentListener;
import peer.messagecounter.MessageCounter;
import peer.messagecounter.ReliableBroadcastTotalCounter;
import peer.messagecounter.TotalMessageCounter;
import peer.peerid.PeerID;
import serialization.binary.BSerializable;
import util.logger.Logger;
import config.Configuration;

public class BasicPeer implements Peer {

	public static final int CLEAN_REC_MSGS = 60000;

	// List of registered communication layers
	private final List<CommunicationLayer> communicationLayers = new CopyOnWriteArrayList<CommunicationLayer>();
	
	// The instance of the message counter to be used.
	protected final MessageCounter msgCounter = new MessageCounter();

	private final Random r = new Random();

	// A map containing entries per message class referencing listeners for the
	// received messages
	private final ConcurrentHashMap<Class<? extends BroadcastMessage>, MessageReceivedListener> receivingListenersTable = new ConcurrentHashMap<Class<? extends BroadcastMessage>, MessageReceivedListener>();

	// A list containing the listeners which will be notified when the
	// DatagramSocket sent() method is sent.
	private final List<MessageSentListener> messageSentListeners = new CopyOnWriteArrayList<MessageSentListener>();

	// the communication peer
	private final CommProvider commProvider;
	
	//Received messages processor
	private ReceivedProcessor receivedProcessor;
		
	// the peer id
	private PeerID peerID;

	private boolean initialized = false;

	private int DELAYED_INIT = 0;
	

	private final Logger logger = Logger.getLogger(BasicPeer.class);

	/**
	 * Constructor of the class. It is the default constructor which configures
	 * all internal properties.
	 */
	public BasicPeer(final CommProvider commProvider) {
		this.commProvider = commProvider;
	}

	@Override
	public synchronized boolean isInitialized() {
		return initialized;
	}

	protected synchronized void initialize() {
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

		logger.info("Peer " + peerID + " initializing");
		
		receivedProcessor = new ReceivedProcessor(this, msgCounter);

		init();
			
		commProvider.initComm();
		
		receivedProcessor.start();
		
		logger.trace("Peer " + peerID + " basic functionality initialized");
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
		receivedProcessor.setHearListerner(hearListener);
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
	
	protected int getSize(final BroadcastMessage message) throws IOException {
		final byte[] data = toByteArray(message);
		return data.length;
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

			initialize();
		}
	}

	private void init() {
		try {
			final String delayedInitStr = Configuration.getInstance().getProperty("basicPeer.delayedInit");
			if (delayedInitStr != null) {
				DELAYED_INIT = Integer.parseInt(delayedInitStr);
				logger.info("Peer " + peerID + " set DELAYED_INIT to " + DELAYED_INIT);
			}
		} catch (final Exception e) {
			logger.error("Peer " + peerID + " had problem loading configuration: " + e.getMessage());
		}

		initializeLayers();

		final DelayedRandomInit delayedRandomInit = new DelayedRandomInit(this);
		delayedRandomInit.start();
	}

	protected void initializeLayers() {
		// Initialize all layers
		for (final CommunicationLayer layer : communicationLayers)
			layer.init();
	}
	
	@Override
	public void directBroadcast(final BroadcastMessage message) {
		try {
			final int messageSize = getSize(message);
			logger.debug("Peer " + peerID + " sending " + message.getType() + " " + message.getMessageID() + " " + messageSize + " bytes");
			msgCounter.addSent(message.getClass());
			broadcast(message);
		} catch (IOException e) {
			logger.error("Peer " + peerID + " unable to obtain message size");
		}
	}

	private void broadcast(final BroadcastMessage message) {
		try {
			logger.debug("Peer " + peerID + " broadcasting " + message + " " + getSize(message) + " bytes");
			msgCounter.addBroadcasted(message.getClass());

			// Message is converted to byte array
			final byte[] data = toByteArray(message);
			
			msgCounter.addMessageSize(data.length);

			commProvider.broadcast(data);

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
	private byte[] toByteArray(final BSerializable bSerializable) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(baos);
		bSerializable.write(out);
		out.close();
		byte[] data = baos.toByteArray();
		return data;
	}

	@Override
	public void stopPeer() {
		unitialize();
		
		stopCommunicationLayers();
		
		stopThreads();
	}

	protected void stopThreads() {		
		logger.trace("Peer " + peerID + " stopping received processor thread");
		receivedProcessor.stopAndWait();
		
		try {
			logger.trace("Peer " + peerID + " finalizing communication provider");
			commProvider.stopComm();
		} catch (final IOException e) {
			logger.error("Peer " + peerID + " had problem finalizing communication " + e.getMessage());
		}
				
		logger.trace("Peer " + peerID + " all threads stopped");
	}

	private void stopCommunicationLayers() {
		// Communication layers are stopped in reverse order of initialization
		logger.trace("Peer " + peerID + " stopping communication layers");
		Collections.reverse(communicationLayers);
		for (final CommunicationLayer layer : communicationLayers) {
			logger.trace("Peer " + peerID + " stopping layer " + layer.getClass().getCanonicalName());
			layer.stop();
		}
		
		logger.trace("Peer " + peerID + " communication layers stopped");
	}

	@Override
	public void printStatistics() {
		TotalMessageCounter.logStatistics();
		ReliableBroadcastTotalCounter.logStatistics();

		logger.info("Simulation finished");
	}
	
	protected void receiveMessage(final BroadcastMessage message) {
		logger.trace("Peer " + getPeerID() + " processing message " + message);
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

	public CommProvider getCommProvider() {
		return commProvider;
	}

	@Override
	public PeerID getPeerID() {
		return peerID;
	}
}
