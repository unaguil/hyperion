package detection.beaconDetector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import peer.Peer;
import peer.PeerID;
import peer.PeerIDSet;
import peer.RegisterCommunicationLayerException;
import peer.message.BroadcastMessage;
import peer.message.MessageSentListener;
import util.WaitableThread;
import config.Configuration;
import detection.NeighborDetector;
import detection.NeighborEventsListener;
import detection.message.BeaconMessage;

/**
 * This class implements a neighbor detector using beacon sending. Beacons are
 * sent using a fixed period of time unless another message was recently
 * broadcasted from this peer.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class BeaconDetector implements NeighborDetector, MessageSentListener {

	// This class is the thread which sends the beacons periodically
	private class BeaconSendThread extends WaitableThread {

		private final BeaconDetector beaconDetector;

		public BeaconSendThread(final BeaconDetector beaconDetector) {
			this.beaconDetector = beaconDetector;
		}

		@Override
		public void run() {
			// send initial beacon
			beaconDetector.sendBeacon();

			while (!Thread.interrupted()) {
				
					logger.trace("Peer " + peer.getPeerID() + " beacon thread running");

				// Get the time elapsed since the last packet (beacon or not)
				// was sent by this node
				final long elapsedTime = System.currentTimeMillis() - lastSentTime.get();

				// Clean old neighbors
				beaconDetector.cleanOldNeighbors();

				// Check if a beacon must be sent. Beacons are only sent if a
				// packet was not previously broadcasted by this node in a
				// period of time
				
				final long sleepTime; 
				if (elapsedTime >= BEACON_TIME) {
					// Send the beacon
					beaconDetector.sendBeacon();
					// Schedule next beacon sleeping the thread
					sleepTime = BEACON_TIME;
				} else {
					// Schedule timer for the difference of time (time for the 
					// next beacon)
					sleepTime = BEACON_TIME - elapsedTime;
				}
				
				try {
					Thread.sleep(sleepTime);
				} catch (final InterruptedException e) {
					finishBeaconThread();
					return;
				}
			}

			finishBeaconThread();
		}
		
		private void finishBeaconThread() {
			
				logger.trace("Peer " + peer.getPeerID() + " beacon thread finalized");

			this.threadFinished();
		}
	}

	// Configuration properties
	private int BEACON_TIME = 1000; // Default values
	private int RANDOM_WAIT = 200;

	// Stores the time of the last packet sent by this peer
	private final AtomicLong lastSentTime = new AtomicLong();

	private final Logger logger = Logger.getLogger(BeaconDetector.class);

	// List of neighbor notification listeners
	private final List<NeighborEventsListener> neighborNotificationListeners = new CopyOnWriteArrayList<NeighborEventsListener>();

	// Map which contains current neighbors

	private final Map<PeerID, Long> neighborsTable = new ConcurrentHashMap<PeerID, Long>();
	private final PeerIDSet newNeighbors = new PeerIDSet();

	// Reference to the peer
	private final Peer peer;

	// Thread which sends beacons periodically
	private BeaconSendThread beaconThread;
	private NeighborNotifier neighborNotifier;

	private BeaconMessage beaconMessage;

	private long LOST_TIME;

	private boolean init = false;

	private final int NOTIFY_TIME = 100;

	/**
	 * Constructor of the class. Configures internal properties using global
	 * configuration.
	 * 
	 * @param peer
	 *            the peer which provides communication capabilities
	 */
	public BeaconDetector(final Peer peer) {
		this.peer = peer;

		peer.addSentListener(this);

		// Register the message heard listener
		peer.setHearListener(this);

		try {
			peer.addCommunicationLayer(this, new HashSet<Class<? extends BroadcastMessage>>());
		} catch (final RegisterCommunicationLayerException e) {
			logger.error("Peer " + peer.getPeerID() + " had problem registering communication layer: " + e.getMessage());
		}
	}

	@Override
	public void addNeighborListener(final NeighborEventsListener listener) {
		neighborNotificationListeners.add(listener);
	}

	@Override
	public PeerIDSet getCurrentNeighbors() {
		final PeerIDSet currentNeighbors = new PeerIDSet();
		synchronized (neighborsTable) {
			currentNeighbors.addPeers(neighborsTable.keySet());
		}
		return currentNeighbors;
	}

	private static class NeighborNotifier extends WaitableThread {

		private final long notifyTime;
		private final BeaconDetector beaconDetector;

		public NeighborNotifier(final BeaconDetector beaconDetector, final int notifyTime) {
			this.notifyTime = notifyTime;
			this.beaconDetector = beaconDetector;
		}

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				beaconDetector.notifyNewNeighbors();
				try {
					Thread.sleep(notifyTime);
				} catch (final InterruptedException e) {
					this.threadFinished();
					return;
				}
			}

			this.threadFinished();
		}
	}

	@Override
	public void init() {
		try {
			final String beaconTimeStr = Configuration.getInstance().getProperty("beaconDetector.beaconTime");
			BEACON_TIME = Integer.parseInt(beaconTimeStr);
			logger.info("Peer " + peer.getPeerID() + " set BEACON_TIME to " + BEACON_TIME);

			final String randomWaitStr = Configuration.getInstance().getProperty("messageProcessor.randomWait");
			RANDOM_WAIT = Integer.parseInt(randomWaitStr);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		LOST_TIME = (BEACON_TIME + RANDOM_WAIT) * 2;

		
			logger.trace("Peer " + peer.getPeerID() + " beacon time (" + BEACON_TIME + ")");

		beaconMessage = new BeaconMessage(peer.getPeerID());

		// Send initial beacon and schedule next
		beaconThread = new BeaconSendThread(this);
		beaconThread.start();

		neighborNotifier = new NeighborNotifier(this, NOTIFY_TIME);
		neighborNotifier.start();

		init = true;
	}

	public void notifyNewNeighbors() {
		final PeerIDSet neighbors = new PeerIDSet();
		synchronized (newNeighbors) {
			neighbors.addPeers(newNeighbors);
			newNeighbors.clear();
		}

		if (!neighbors.isEmpty())
			notifyAppearance(neighbors);
	}

	@Override
	public void messageReceived(final BroadcastMessage message, final long receptionTime) {
		if (init) {
			// Check that the message was received from an unknown neighbor
			final boolean newNeighbor = !neighborsTable.containsKey(message.getSender());

			if (newNeighbor)
				synchronized (newNeighbors) {
					newNeighbors.addPeer(message.getSender());
				}

			// Update sender of the received message
			neighborsTable.put(message.getSender(), Long.valueOf(System.currentTimeMillis()));
			
				logger.trace("Peer " + peer.getPeerID() + " has updated neighbor " + message.getSender());
		}
	}

	@Override
	public void messageSent(final BroadcastMessage message, final long sentTime) {
		// A message has been sent by this peer. Record sent time.
		
			logger.trace("Peer " + peer.getPeerID() + " detected sent message");
		lastSentTime.set(sentTime);
	}

	@Override
	public synchronized void stop() {
		neighborNotifier.stopAndWait();
		beaconThread.stopAndWait();
	}

	// Cleans those neighbor not heard in a period of time specified by
	// LOST_TIME
	private void cleanOldNeighbors() {
		final PeerIDSet removedPeers = new PeerIDSet();

		synchronized (neighborsTable) {
			// Remove those neighbors old enough
			for (final Iterator<PeerID> it = neighborsTable.keySet().iterator(); it.hasNext();) {
				final PeerID neighbor = it.next();
				final long timestamp = neighborsTable.get(neighbor).longValue();
				final long elapsedTime = System.currentTimeMillis() - timestamp;
				if (elapsedTime >= LOST_TIME) {
					
						logger.trace("Peer " + peer.getPeerID() + " removing neighbor " + neighbor + " elapsed time " + elapsedTime + " [" + System.currentTimeMillis() + " - " + timestamp + "]");
					removedPeers.addPeer(neighbor);
					it.remove();
					synchronized (newNeighbors) {
						newNeighbors.remove(neighbor);
					}
				}
			}
		}

		// Notify neighbor removal to registered listeners
		if (!removedPeers.isEmpty())
			notifyDissappearance(removedPeers);
	}

	// Notify appearance of neighbors to listeners
	private void notifyAppearance(final PeerIDSet neighbors) {
		logger.debug("Peer " + peer.getPeerID() + " has new neighbors: " + neighbors);
		
			logger.trace("Peer " + peer.getPeerID() + " current neighbors: " + getCurrentNeighbors());
		for (final NeighborEventsListener listener : neighborNotificationListeners)
			listener.appearedNeighbors(neighbors);
	}

	// Notify disappearance of neighbors to listeners
	private void notifyDissappearance(final PeerIDSet neighbors) {
		logger.debug("Peer " + peer.getPeerID() + " has lost neighbors: " + neighbors);
		
			logger.trace("Peer " + peer.getPeerID() + " current neighbors: " + getCurrentNeighbors());
		for (final NeighborEventsListener listener : neighborNotificationListeners)
			listener.dissapearedNeighbors(neighbors);
	}

	// Send a beacon using broadcast provided by communication peer
	private void sendBeacon() {
		peer.enqueueBroadcast(beaconMessage);
	}
}
