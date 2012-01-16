package peer.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
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

	protected Set<PeerID> expectedDestinations = new HashSet<PeerID>();

	public BroadcastMessage() {
		messageID = null;
	}
	
	public BroadcastMessage(final PeerID sender, final Set<PeerID> expectedDestinations) {
		this.messageID = new MessageID(sender);
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
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(messageID);
	}

	public void merge(final BroadcastMessage broadcastMessage) {
		addExpectedDestinations(broadcastMessage.getExpectedDestinations());		
	}
}
