package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;

import peer.message.EnvelopeMessage;
import peer.message.MulticastMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import serialization.binary.UnserializationUtils;

/**
 * This class defines a message which can be send to multiple remote nodes. It
 * can also contain another message as payload
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RemoteMulticastMessage extends RemoteMessage implements MulticastMessage, EnvelopeMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the remote destinations for this message
	private final PeerIDSet remoteDestinations = new PeerIDSet();

	// the payload of the message
	private final PayloadMessage payload;

	// the nodes that the message is sent through
	private final PeerIDSet throughPeers = new PeerIDSet();
	
	public RemoteMulticastMessage() {
		payload = null;
	}

	/**
	 * Constructor of the remote multicast message
	 * 
	 * @param remoteDestinations
	 *            the set containing the remote destinations for the message
	 * @param payload
	 *            the payload of the message
	 * @param source
	 *            the remote node which sent the message
	 */
	public RemoteMulticastMessage(final PeerID source, final PeerIDSet remoteDestinations, final PayloadMessage payload) {
		super(source, Collections.singleton(source));
		this.remoteDestinations.addPeers(remoteDestinations);
		this.payload = payload;
		this.throughPeers.addPeers(Collections.singleton(source));
	}
	
	public RemoteMulticastMessage(final PeerID source, final PeerIDSet remoteDestinations, final PayloadMessage payload, final int distance) {
		super(source, Collections.singleton(source), distance);
		this.remoteDestinations.addPeers(remoteDestinations);
		this.payload = payload;
		this.throughPeers.addPeers(Collections.singleton(source));
	}

	/**
	 * Constructor of the remote multicast message. It uses another message as
	 * base.
	 * 
	 * @param remoteMessage
	 *            the message used to construct this one
	 * @param sender
	 *            the new sender of the message
	 * @param throughPeers
	 *            the set of peers used to reached the destination nodes
	 * @param respondingTo
	 *            the message this one responds to
	 * @param newDistance
	 *            the new distance for the message
	 */
	public RemoteMulticastMessage(final RemoteMulticastMessage multicastMessage, final PeerID sender, final PeerIDSet throughPeers, final int newDistance) {
		super(multicastMessage, sender, throughPeers.getPeerSet(), newDistance);
		this.remoteDestinations.addPeers(multicastMessage.getRemoteDestinations());
		this.payload = multicastMessage.getPayload();
		this.throughPeers.addPeers(throughPeers);
	}

	@Override
	public PayloadMessage getPayload() {
		return payload;
	}

	/**
	 * Gets the set of destinations this message is sent to
	 * 
	 * @return a set containing the destinations of this message
	 */
	public PeerIDSet getRemoteDestinations() {
		return new PeerIDSet(remoteDestinations);
	}

	/**
	 * Gets the set of neighbors used sent the message through
	 * 
	 * @return a set containing those neighbors used to sent the message
	 */
	public PeerIDSet getThroughPeers() {
		return throughPeers;
	}

	/**
	 * Removes a peer used to sent the message through
	 * 
	 * @param destination
	 *            the destination to remove from current ones
	 */
	public void removeRemoteDestination(final PeerID destination) {
		remoteDestinations.remove(destination);
	}

	@Override
	public PeerIDSet getDestNeighbors() {
		return throughPeers;
	}

	@Override
	public boolean hasPayload() {
		return payload != null;
	}

	@Override
	public String toString() {
		return super.toString() + " (To:" + getDestNeighbors() + ")";
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		remoteDestinations.addPeers(Arrays.asList((PeerID[])in.readObject()));
		throughPeers.addPeers(Arrays.asList((PeerID[])in.readObject()));
		UnserializationUtils.setFinalField(RemoteMulticastMessage.class, this, "payload", in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(remoteDestinations.getPeerSet().toArray(new PeerID[0]));
		out.writeObject(throughPeers.getPeerSet().toArray(new PeerID[0]));
		out.writeObject(payload);
	}

	@Override
	public String getType() {
		if (payload != null)
			return super.getType() + "(" + payload.getType() + ")";
		
		return super.getType();
	}
}
