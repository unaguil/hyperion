package peer.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import peer.peerid.PeerID;

public class BundleMessage extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>();
	
	private final Set<PeerID> removedDestinations = new HashSet<PeerID>();
	
	public BundleMessage() {
		
	}

	public BundleMessage(final PeerID sender, final List<PeerID> expectedDestinations, final List<BroadcastMessage> messages) {
		super(sender, expectedDestinations);
		this.messages.addAll(messages);
	}

	public List<BroadcastMessage> getMessages() {
		return messages;
	}

	@Override
	public boolean removeExpectedDestination(PeerID peerID) {
		if (super.removeExpectedDestination(peerID)) {
			removedDestinations.add(peerID);
		
			//Remove all those message whose destinations were removed
			for (final Iterator<BroadcastMessage> it = messages.iterator(); it.hasNext(); ) {
				BroadcastMessage broadcastMessage = it.next();
				if (removedDestinations.containsAll(broadcastMessage.getExpectedDestinations()))
					it.remove();
			}
			
			return true;
		}
		
		return false;
	}

	@Override
	public String toString() {
		return getType() +  " " + getMessageID() + " [" + messages.size() + "]";
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		expectedDestinations.addAll(Arrays.asList((PeerID[])in.readObject()));
		messages.addAll(Arrays.asList((BroadcastMessage[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(expectedDestinations.toArray(new PeerID[0]));
		out.writeObject(messages.toArray(new BroadcastMessage[0]));
	}
}
