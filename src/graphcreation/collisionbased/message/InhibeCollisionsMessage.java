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

public class InhibeCollisionsMessage extends RemoteMessage {

	private final Set<Inhibition> inhibitedCollisions = new HashSet<Inhibition>();
	
	public InhibeCollisionsMessage() {
		super(MessageTypes.INHIBE_COLLISIONS_MESSAGE);
	}

	public InhibeCollisionsMessage(final PeerID source, final Set<Inhibition> inhibitions) {
		super(MessageTypes.INHIBE_COLLISIONS_MESSAGE, source, null, Collections.<PeerID> emptySet());
		inhibitedCollisions.addAll(inhibitions);
	}

	public Set<Inhibition> getInhibedCollisions() {
		return inhibitedCollisions;
	}

	@Override
	public String toString() {
		return inhibitedCollisions.toString();
	}

	@Override
	public BroadcastMessage copy() {
		return new InhibeCollisionsMessage(getSource(), getInhibedCollisions());
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		final byte sCollisions = in.readByte();
		for (int i = 0; i < sCollisions; i++) {
			final Inhibition inhibition = new Inhibition();
			inhibition.read(in);
			inhibitedCollisions.add(inhibition);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(inhibitedCollisions, out);
	}
}
