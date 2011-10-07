package graphcreation.collisionbased.message;

import graphcreation.services.Service;

import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.EnvelopeMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class ForwardMessage extends RemoteMessage implements EnvelopeMessage, PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final PayloadMessage payload;
	private final Set<Service> destinations = new HashSet<Service>();

	public ForwardMessage(final PeerID source, final PayloadMessage payload, final Set<Service> destinations) {
		super(source);
		this.payload = payload;
		this.destinations.addAll(destinations);
	}

	@Override
	public PayloadMessage getPayload() {
		return payload;
	}

	public Set<Service> getDestinations() {
		return destinations;
	}

	@Override
	public PayloadMessage copy() {
		return new ForwardMessage(getSource(), getPayload().copy(), getDestinations());
	}

	@Override
	public boolean hasPayload() {
		return payload != null;
	}
}
