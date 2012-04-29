package detection.beaconDetector;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import peer.RegisterCommunicationLayerException;
import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import peer.message.MessageSentListener;
import peer.peerid.PeerID;
import util.WaitableThread;
import util.logger.Logger;
import util.timer.Timer;
import util.timer.TimerTask;
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
public final class BeaconDetector implements NeighborDetector, MessageSentListener, TimerTask {

	// This class is the thread which sends the beacons periodically
	private class BeaconSendThread extends WaitableThread {

		private final BeaconDetector beaconDetector;
		
		private final Random r = new Random();

		public BeaconSendThread(final BeaconDetector beaconDetector) {
			this.beaconDetector = beaconDetector;
		}

		@Override
		public void run() {			
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
				final int jitter = r.nextInt(MAX_JITTER);
				final int beaconTime = BEACON_TIME - jitter;
				
				if (elapsedTime >= beaconTime) {
					// Send the beacon
					beaconDetector.sendBeacon();
					// Schedule next beacon sleeping the thread
					sleepTime = beaconTime;
				} else
					// Schedule timer for the difference of time (time for the
					// next beacon)
					sleepTime = beaconTime - elapsedTime; 
				
				try {
					Thread.sleep(sleepTime);
				} catch (final InterruptedException e) {
					interrupt();
				}
			}
			
			finishThread();
		}
		
		private void finishThread() {
			logger.trace("Peer " + peer.getPeerID() + " beacon thread finalized");
			this.threadFinished();
		}
		
		@Override
		public void stopAndWait() {
			logger.trace("Peer " + peer.getPeerID() + " interrupting beacon thread");
			super.stopAndWait();
		}
	}

	// Configuration properties
	private int BEACON_TIME = 1000; // Default values

	// Stores the time of the last packet sent by this peer
	private final AtomicLong lastSentTime = new AtomicLong();

	private final Logger logger = Logger.getLogger(BeaconDetector.class);

	// List of neighbor notification listeners
	private final List<NeighborEventsListener> neighborNotificationListeners = new CopyOnWriteArrayList<NeighborEventsListener>();

	// Map which contains current neighbors
	private final Map<PeerID, Long> neighborsTable = new ConcurrentHashMap<PeerID, Long>();
	
	private final Set<PeerID> newNeighbors = new HashSet<PeerID>();
	private final Set<PeerID> lostNeighbors = new HashSet<PeerID>();

	// Reference to the peer
	private final ReliableBroadcastPeer peer;

	// Thread which sends beacons periodically
	private BeaconSendThread beaconThread;

	private BeaconMessage beaconMessage; 
	
	private static final int MAX_JITTER = 500;

	private long LOST_TIME;

	private boolean init = false;
	
	private static final long DELAY_NOTIFICATION = 50;
	
	private Timer notificationTimer = new Timer(DELAY_NOTIFICATION, this);

	/**
	 * Constructor of the class. Configures internal properties using global
	 * configuration.
	 * 
	 * @param peer
	 *            the peer which provides communication capabilities
	 */
	public BeaconDetector(final ReliableBroadcastPeer peer) {
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
	public Set<PeerID> getCurrentNeighbors() {
		final Set<PeerID> currentNeighbors = new HashSet<PeerID>();
		synchronized (neighborsTable) {
			currentNeighbors.addAll(neighborsTable.keySet());
		}
		
		synchronized (newNeighbors) {
			currentNeighbors.addAll(newNeighbors);
			currentNeighbors.remove(lostNeighbors);
		}
		
		return Collections.unmodifiableSet(currentNeighbors);
	}

	@Override
	public void init() {
		try {
			final String beaconTimeStr = Configuration.getInstance().getProperty("beaconDetector.beaconTime");
			BEACON_TIME = Integer.parseInt(beaconTimeStr);
			logger.info("Peer " + peer.getPeerID() + " set BEACON_TIME to " + BEACON_TIME);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		LOST_TIME = BEACON_TIME * 2;

		logger.trace("Peer " + peer.getPeerID() + " beacon time (" + BEACON_TIME + ")");

		beaconMessage = new BeaconMessage(peer.getPeerID());
		
		final Random r = new Random(); 
		lastSentTime.set(System.currentTimeMillis() - r.nextInt(BEACON_TIME));

		// Send initial beacon and schedule next
		beaconThread = new BeaconSendThread(this);
		beaconThread.start();
		
		notificationTimer.start();

		init = true;
	}

	@Override
	public void messageReceived(final BroadcastMessage message, final long receptionTime) {
		if (init) {
			// Check that the message was received from an unknown neighbor
			final boolean newNeighbor = !neighborsTable.containsKey(message.getSender());

			// Update sender of the received message
			neighborsTable.put(message.getSender(), Long.valueOf(System.currentTimeMillis()));
			logger.trace("Peer " + peer.getPeerID() + " has updated neighbor " + message.getSender());
			
			if (newNeighbor)
				addNewNeighbor(message.getSender());
		}
	}

	@Override
	public void messageSent(final BroadcastMessage message, final long sentTime) {
		// A message has been sent by this peer. Record sent time.
		logger.trace("Peer " + peer.getPeerID() + " detected sent message");
		lastSentTime.set(sentTime);
	}

	@Override
	public void stop() {
		notificationTimer.stopAndWait();
		beaconThread.stopAndWait();
	}

	// Cleans those neighbor not heard in a period of time specified by
	// LOST_TIME
	private void cleanOldNeighbors() {
		final Set<PeerID> removedPeers = new HashSet<PeerID>();

		synchronized (neighborsTable) {
			// Remove those neighbors old enough
			for (final Iterator<PeerID> it = neighborsTable.keySet().iterator(); it.hasNext();) {
				final PeerID neighbor = it.next();
				final long timestamp = neighborsTable.get(neighbor).longValue();
				final long elapsedTime = System.currentTimeMillis() - timestamp;
				if (elapsedTime >= LOST_TIME) {

					logger.trace("Peer " + peer.getPeerID() + " removing neighbor " + neighbor + " elapsed time " + elapsedTime + " [" + System.currentTimeMillis() + " - " + timestamp + "]");
					removedPeers.add(neighbor);
					it.remove();
				}
			}
		}

		// Notify neighbor removal to registered listeners
		if (!removedPeers.isEmpty())
			removeLostNeighbors(removedPeers);
	}

	// Send a beacon using broadcast provided by communication peer
	private void sendBeacon() {
		peer.directBroadcast(beaconMessage);
	}

	@Override
	public boolean merge(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
		return false;
	}
	
	private void addNewNeighbor(final PeerID newNeighbor) {
		synchronized (newNeighbors) {
			lostNeighbors.remove(newNeighbor);
			newNeighbors.add(newNeighbor);
		}
	}
	
	private void removeLostNeighbors(final Set<PeerID> expiredNeighbors) {
		synchronized (newNeighbors) {
			newNeighbors.removeAll(expiredNeighbors);
			lostNeighbors.addAll(expiredNeighbors);
		}
	}

	@Override
	public void perform() throws InterruptedException {
		final Set<PeerID> addedNeighbors = new HashSet<PeerID>();
		final Set<PeerID> removedNeighbors = new HashSet<PeerID>();
		synchronized (newNeighbors) {
			addedNeighbors.addAll(newNeighbors);
			removedNeighbors.addAll(lostNeighbors);
			
			newNeighbors.clear();
			lostNeighbors.clear();
		}
		
		if (!addedNeighbors.isEmpty())
			logger.debug("Peer " + peer.getPeerID() + " has new neighbors: " + addedNeighbors);
		
		if (!removedNeighbors.isEmpty())
			logger.debug("Peer " + peer.getPeerID() + " has lost neighbors: " + removedNeighbors);
		
		for (final NeighborEventsListener listener : neighborNotificationListeners)
			listener.neighborsChanged(Collections.unmodifiableSet(addedNeighbors), Collections.unmodifiableSet(removedNeighbors));
	}
}
