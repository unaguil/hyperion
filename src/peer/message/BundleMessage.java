package peer.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import peer.peerid.PeerID;

public class BundleMessage extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>();
	
	public BundleMessage() {
		
	}

	public BundleMessage(final PeerID sender, final List<BroadcastMessage> messages) {
		super(sender);
		this.messages.addAll(messages);
	}

	public List<BroadcastMessage> getMessages() {
		return messages;
	}

	@Override
	public String toString() {
		return getType() +  " " + getMessageID() + " [" + messages.size() + "]";
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		messages.addAll(Arrays.asList((BroadcastMessage[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(messages.toArray(new BroadcastMessage[0]));
	}
}
