package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.message.MulticastMessage;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

/**
 * This class defines a message which can be send to multiple remote nodes. It
 * can also contain another message as payload
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RemoteMulticastMessage extends RemoteMessage implements MulticastMessage {

	// the remote destinations for this message
	private final Set<PeerID> remoteDestinations = new HashSet<PeerID>();

	// the nodes that the message is sent through
	private final Set<PeerID> throughPeers = new HashSet<PeerID>();
	
	private final boolean directBroadcast;
	
	public RemoteMulticastMessage() {
		super(MessageTypes.REMOTE_MULTICAST_MESSAGE);
		directBroadcast = false;
	}
	
	protected RemoteMulticastMessage(final byte mType) {
		super(mType);
		directBroadcast = false;
	}

	protected RemoteMulticastMessage(final byte mType, final PeerID source, final Set<PeerID> remoteDestinations, final BroadcastMessage payload, final boolean directBroadcast) {
		super(mType, source, payload, Collections.singleton(source));
		this.remoteDestinations.addAll(remoteDestinations);
		this.throughPeers.addAll(Collections.singleton(source));
		this.directBroadcast = directBroadcast;
	}
	
	protected RemoteMulticastMessage(final byte mType, final PeerID source, final Set<PeerID> remoteDestinations, final BroadcastMessage payload, final int distance, final boolean directBroadcast) {
		super(mType, source, payload, Collections.singleton(source), distance);
		this.remoteDestinations.addAll(remoteDestinations);
		this.throughPeers.addAll(Collections.singleton(source));
		this.directBroadcast = directBroadcast;
	}
	
	public RemoteMulticastMessage(final PeerID source, final Set<PeerID> remoteDestinations, final BroadcastMessage payload, final boolean directBroadcast) {
		super(MessageTypes.REMOTE_MULTICAST_MESSAGE, source, payload, Collections.singleton(source));
		this.remoteDestinations.addAll(remoteDestinations);
		this.throughPeers.addAll(Collections.singleton(source));
		this.directBroadcast = directBroadcast;
	}
	
	public RemoteMulticastMessage(final PeerID source, final Set<PeerID> remoteDestinations, final BroadcastMessage payload, final int distance, final boolean directBroadcast) {
		super(MessageTypes.REMOTE_MULTICAST_MESSAGE, source, payload, Collections.singleton(source), distance);
		this.remoteDestinations.addAll(remoteDestinations);
		this.throughPeers.addAll(Collections.singleton(source));
		this.directBroadcast = directBroadcast;
	}

	public RemoteMulticastMessage(final RemoteMulticastMessage multicastMessage, final PeerID sender, final Set<PeerID> throughPeers, final int newDistance) {
		super(multicastMessage, sender, throughPeers, newDistance);
		this.remoteDestinations.addAll(multicastMessage.getRemoteDestinations());
		this.throughPeers.addAll(throughPeers);
		this.directBroadcast = multicastMessage.directBroadcast;
	}

	/**
	 * Gets the set of destinations this message is sent to
	 * 
	 * @return a set containing the destinations of this message
	 */
	public Set<PeerID> getRemoteDestinations() {
		return new HashSet<PeerID>(remoteDestinations);
	}

	/**
	 * Gets the set of neighbors used sent the message through
	 * 
	 * @return a set containing those neighbors used to sent the message
	 */
	public Set<PeerID> getThroughPeers() {
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
	
	public boolean isDirectBroadcas() {
		return directBroadcast;
	}

	@Override
	public Set<PeerID> getDestNeighbors() {
		return throughPeers;
	}

	@Override
	public String toString() {
		return super.toString() + " (To:" + getDestNeighbors() + ")";
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readPeers(remoteDestinations, in);
		SerializationUtils.readPeers(throughPeers, in);
		SerializationUtils.setFinalField(RemoteMulticastMessage.class, this, "directBroadcast", in.readBoolean());
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(remoteDestinations, out);
		SerializationUtils.writeCollection(throughPeers, out);
		out.writeBoolean(directBroadcast);
	}
}
