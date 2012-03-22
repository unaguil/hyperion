package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

/**
 * This abstract class defines those messages which can be sent to a node that
 * is not a direct neighbor of the current node.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public abstract class RemoteMessage extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the identification of the remote message (it is different from the one
	// used in near broadcasting)
	private final MessageID remoteMessageID;

	// the traversed distance (number of hops) of the current message. It will be transmitted as a byte (127 hops max.)
	private final int distance;
	
	public RemoteMessage() {
		remoteMessageID = null;
		distance = 0;
	}

	/**
	 * Constructs a remote message.
	 * 
	 * @param source
	 *            the remote source of the message
	 */
	public RemoteMessage(final PeerID source, final Set<PeerID> expectedDestinations) {
		super(source, expectedDestinations);
		this.remoteMessageID = new MessageID(source, MessageIDGenerator.getNewID());
		this.distance = 0;
	}

	/**
	 * Constructs a remote message with a given initial traversed distance.
	 * 
	 * @param source
	 *            the remote source of the message
	 * @param distance
	 *            the initial traversed distance by the message
	 */
	public RemoteMessage(final PeerID source, final Set<PeerID> expectedDestinations, final int distance) {
		super(source, expectedDestinations);
		this.remoteMessageID = new MessageID(source, MessageIDGenerator.getNewID());
		this.distance = distance;
	}

	/**
	 * Constructs a remote message which uses another one as base
	 * 
	 * @param remoteMessage
	 *            the message used to construct this one
	 * @param sender
	 *            the new sender of the message
	 * @param respondingTo
	 *            the message this one responds to
	 */
	public RemoteMessage(final RemoteMessage remoteMessage, final PeerID sender, final Set<PeerID> expectedDestinations, final int newDistance) {
		super(sender, expectedDestinations);
		this.remoteMessageID = remoteMessage.getRemoteMessageID();
		this.distance = newDistance;
	}

	/**
	 * Gets the remote source node of the message
	 * 
	 * @return the remote source node of the message
	 */
	public PeerID getSource() {
		return remoteMessageID.getPeer();
	}

	/**
	 * Gets the remote identifier of this message
	 * 
	 * @return the remote identifier of this message
	 */
	public MessageID getRemoteMessageID() {
		return remoteMessageID;
	}

	/**
	 * Gets the distance traversed by the message.
	 * 
	 * @return the distance traversed by the message
	 */
	public int getDistance() {
		return distance;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof RemoteMessage))
			return false;

		final RemoteMessage remoteMessage = (RemoteMessage) o;
		return this.remoteMessageID.equals(remoteMessage.remoteMessageID);
	}

	@Override
	public int hashCode() {
		return remoteMessageID.hashCode();
	}

	@Override
	public String toString() {
		return super.toString() + " R:" + getRemoteMessageID() + " D:" + getDistance() + "";
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.setFinalField(RemoteMessage.class, this, "remoteMessageID", in.readObject());
		UnserializationUtils.setFinalField(RemoteMessage.class, this, "distance", in.readByte());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(remoteMessageID);
		out.writeByte(distance);
	}
}
