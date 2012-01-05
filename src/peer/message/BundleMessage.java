package peer.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	
	public BundleMessage() {
		
	}

	public BundleMessage(final PeerID sender, final List<BroadcastMessage> messages) {
		super(sender, Collections.<PeerID> emptySet());
		
		this.messages.addAll(messages);
		
		for (final BroadcastMessage message : messages) {
			expectedDestinations.addAll(message.getExpectedDestinations());
		}
	}

	public List<BroadcastMessage> getMessages() {
		return messages;
	}

	@Override
	public boolean removeDestination(final PeerID dest) {
		//Remove all those message whose destinations were removed
		for (final Iterator<BroadcastMessage> it = messages.iterator(); it.hasNext(); ) {
			BroadcastMessage broadcastMessage = it.next();
			if (broadcastMessage.removeDestination(dest)) {
				it.remove();
			}
		}
		
		//remove all destinations which have not related messages
		final Set<PeerID> reallyExpectedDestinations = new HashSet<PeerID>();
		for (final BroadcastMessage broadcastMessage : messages)
			reallyExpectedDestinations.addAll(broadcastMessage.getExpectedDestinations());
		
		expectedDestinations.retainAll(reallyExpectedDestinations);
		
		return expectedDestinations.isEmpty();
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

	public void merge(BundleMessage bundleMessage) {
		expectedDestinations.addAll(bundleMessage.expectedDestinations);
		messages.addAll(bundleMessage.messages);
	}
}
