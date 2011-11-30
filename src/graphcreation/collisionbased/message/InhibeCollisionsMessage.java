package graphcreation.collisionbased.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class InhibeCollisionsMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Set<Inhibition> inhibitedCollisions = new HashSet<Inhibition>();
	
	public InhibeCollisionsMessage() {
		
	}

	public InhibeCollisionsMessage(final PeerID source, final Set<Inhibition> inhibitions) {
		super(source, new ArrayList<PeerID>());
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
	public PayloadMessage copy() {
		return new InhibeCollisionsMessage(getSource(), getInhibedCollisions());
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		inhibitedCollisions.addAll(Arrays.asList((Inhibition[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(inhibitedCollisions.toArray(new Inhibition[0]));
	}
}
