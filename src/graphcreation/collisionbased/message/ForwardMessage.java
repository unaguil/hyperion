package graphcreation.collisionbased.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

public class ForwardMessage extends RemoteMessage {

	private final Set<PeerID> destinations = new HashSet<PeerID>();
	
	public ForwardMessage() {
		super(MessageTypes.FORWARD_MESSAGE);
	}

	public ForwardMessage(final PeerID source, final BroadcastMessage payload, final Set<PeerID> destinations) {
		super(MessageTypes.FORWARD_MESSAGE, source, payload, Collections.<PeerID> emptySet());
		this.destinations.addAll(destinations);
	}

	public Set<PeerID> getDestinations() {
		return destinations;
	}

	@Override
	public BroadcastMessage copy() {
		return new ForwardMessage(getSource(), getPayload().copy(), getDestinations());
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readPeers(destinations, in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(destinations, out);
	}
}
