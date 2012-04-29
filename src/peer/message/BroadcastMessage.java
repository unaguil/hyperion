package peer.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import peer.ReliableBroadcastPeer;
import peer.peerid.PeerID;
import serialization.binary.BSerializable;

/**
 * The type of message which are sent by the reliable broadcasting.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public abstract class BroadcastMessage implements BSerializable {
	
	// the identification of the message (peer, id)
	private final MessageID messageID;
	protected final byte mType;

	protected Set<PeerID> expectedDestinations = new HashSet<PeerID>(); //not serialized

	public BroadcastMessage(final byte mType) {
		this.messageID = new MessageID();
		this.mType = mType;
	}
	
	public BroadcastMessage(final byte mType, final PeerID sender, final Set<PeerID> expectedDestinations) {
		this.mType = mType;
		this.messageID = new MessageID(sender, MessageIDGenerator.getNewID());
		this.expectedDestinations.addAll(expectedDestinations);
	}
	
	public void addExpectedDestinations(final Set<PeerID> destinations) {
		this.expectedDestinations.addAll(destinations);
	}
	
	public boolean removeDestination(final PeerID dest) {
		if (expectedDestinations.remove(dest)) {
			if (expectedDestinations.isEmpty())
				return true;
		}
		return false;
	}

	public long getID() {
		return messageID.getID();
	}

	public PeerID getSender() {
		return messageID.getPeer();
	}

	public MessageID getMessageID() {
		return messageID;
	}

	public String getType() {
		return getClass().getName();
	}

	public Set<PeerID> getExpectedDestinations() {
		return Collections.unmodifiableSet(expectedDestinations);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof BroadcastMessage))
			return false;

		final BroadcastMessage broadcastMessage = (BroadcastMessage) o;
		return this.mType == broadcastMessage.mType && this.messageID.equals(broadcastMessage.messageID);
	}

	@Override
	public int hashCode() {
		return messageID.hashCode();
	}

	@Override
	public String toString() {
		return messageID.toString();
	} 

	@Override
	public void read(ObjectInputStream in) throws IOException {
		messageID.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		out.writeByte(mType);
		messageID.write(out);
	}
	
	public BroadcastMessage copy() {
		return this;
	}

	public static Set<PeerID> removePropagatedNeighbors(final BroadcastMessage broadcastMessage, final ReliableBroadcastPeer peer) {
		final Set<PeerID> neighbors = new HashSet<PeerID>(peer.getDetector().getCurrentNeighbors());
		neighbors.remove(broadcastMessage.getSender());
		return neighbors;
	}
}
