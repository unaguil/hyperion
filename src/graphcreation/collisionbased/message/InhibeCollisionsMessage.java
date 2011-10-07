package graphcreation.collisionbased.message;

import graphcreation.collisionbased.collisiondetector.Collision;

import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.PeerID;
import peer.message.PayloadMessage;

public class InhibeCollisionsMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Set<Collision> inhibitedCollisions = new HashSet<Collision>();

	public InhibeCollisionsMessage(final Set<Collision> collisions, final PeerID source) {
		super(source);
		inhibitedCollisions.addAll(collisions);
	}

	public Set<Collision> getInhibedCollisions() {
		return inhibitedCollisions;
	}

	@Override
	public String toString() {
		return inhibitedCollisions.toString();
	}

	@Override
	public PayloadMessage copy() {
		return new InhibeCollisionsMessage(getInhibedCollisions(), getSource());
	}
}
