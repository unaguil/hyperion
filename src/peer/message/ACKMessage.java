package peer.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class ACKMessage extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final MessageID respondingTo;
	
	public ACKMessage() {
		respondingTo = null;
	}

	public ACKMessage(final PeerID sender, final MessageID respondingTo) {
		super(sender);
		this.respondingTo = respondingTo;
	}

	public MessageID getRespondedMessageID() {
		return respondingTo;
	}

	@Override
	public String toString() {
		return getType() + " " + respondingTo;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.setFinalField(ACKMessage.class, this, "respondingTo", in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(respondingTo);
	}
}
