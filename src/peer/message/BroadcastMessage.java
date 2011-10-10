package peer.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

/**
 * The type of message which are sent by the reliable broadcasting.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public abstract class BroadcastMessage implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the identification of the message (peer, id)
	private final MessageID messageID;

	private Set<PeerID> expectedDestinations = new HashSet<PeerID>();

	
	public BroadcastMessage() {
		messageID = null;
	}
	
	public BroadcastMessage(final PeerID sender) {
		this.messageID = new MessageID(sender, MessageIDGenerator.getNewID());
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

	public void setExpectedDestinations(final Set<PeerID> expectedDestinations) {
		this.expectedDestinations = new HashSet<PeerID>(expectedDestinations);
	}

	public void removeExpectedDestination(final PeerID peerID) {
		expectedDestinations.remove(peerID);
	}

	public void removeExpectedDestinations(final Set<PeerID> peerIDSet) {
		expectedDestinations.removeAll(peerIDSet);
	}

	public Set<PeerID> getExpectedDestinations() {
		return Collections.unmodifiableSet(expectedDestinations);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof BroadcastMessage))
			return false;

		final BroadcastMessage broadcastMessage = (BroadcastMessage) o;
		return this.messageID.equals(broadcastMessage.messageID);
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
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(BroadcastMessage.class, this, "messageID", in.readObject());
		expectedDestinations.addAll(Arrays.asList((PeerID[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(messageID);
		out.writeObject(expectedDestinations.toArray(new PeerID[0]));
	}
}
