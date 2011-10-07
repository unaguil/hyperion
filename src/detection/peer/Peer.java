package detection.peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import message.BroadcastMessage;
import message.MessageString;
import peer.BasicPeer;
import peer.CommunicationLayer;
import peer.PeerID;
import peer.PeerIDSet;
import peer.RegisterCommunicationLayerException;
import util.logger.Logger;
import detection.NeighborEventsListener;

/**
 * This is an implementation of a peer which only provides reliable broadcast
 * and neighbor detection. The node has flood mode that when set starts to
 * broadcast message periodically. It is intended for testing purposes.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class Peer extends BasicPeer implements NeighborEventsListener {

	private static class DumbCommunicationLayer implements CommunicationLayer {

		@Override
		public void messageReceived(final BroadcastMessage message, final long receptionTime) {
		}

		@Override
		public void init() {
		}

		@Override
		public void stop() {
		}
	}

	private final Logger myLogger = Logger.getLogger(Peer.class);

	/**
	 * Constructs the detection layer peer.
	 */
	public Peer() {
		getDetector().addNeighborListener(this);

		final Set<Class<? extends BroadcastMessage>> messageClasses = new HashSet<Class<? extends BroadcastMessage>>();
		messageClasses.add(MessageString.class);
		try {
			this.addCommunicationLayer(new DumbCommunicationLayer(), messageClasses);
		} catch (final RegisterCommunicationLayerException e) {
			myLogger.error("Peer " + this.getPeerID() + " had problem registering communication layer: " + e.getMessage());
		}
	}

	@Override
	protected boolean peerCommands(final String command, final String[] args) {
		if (command.equals("broadcast")) {
			final MessageString msgStr = new MessageString(this.getPeerID(), new String(new byte[1]));
			enqueueBroadcast(msgStr);
			return true;
		}
		return false;
	}

	@Override
	public void loadData() {
	}

	private String getNeighbourListPath(final PeerID peerID) {
		return TEMP_DIR + File.separator + "Neighbours" + peerID + ".xml";
	}

	public void printNeighbourList() {
		final String xmlPath = getNeighbourListPath(getPeerID());
		try {
			final FileOutputStream f = new FileOutputStream(xmlPath);
			getDetector().getCurrentNeighbors().saveToXML(f);
			f.close();
		} catch (final IOException e) {
			myLogger.error("Peer " + this.getPeerID() + " had problem printing neighbor list: " + e.getMessage());
		}
	}

	@Override
	public void printOutputs() {
		printNeighbourList();
	}

	@Override
	public void appearedNeighbors(final PeerIDSet neighbours) {
	}

	@Override
	public void dissapearedNeighbors(final PeerIDSet neighbours) {
	}
}
