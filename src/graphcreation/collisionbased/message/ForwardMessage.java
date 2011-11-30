package graphcreation.collisionbased.message;

import graphcreation.services.Service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.EnvelopeMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class ForwardMessage extends RemoteMessage implements EnvelopeMessage, PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final PayloadMessage payload;
	private final Set<Service> destinations = new HashSet<Service>();
	
	public ForwardMessage() {
		payload = null;
	}

	public ForwardMessage(final PeerID source, final PayloadMessage payload, final Set<Service> destinations) {
		super(source, new ArrayList<PeerID>());
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

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.setFinalField(ForwardMessage.class, this, "payload", in.readObject());
		destinations.addAll(Arrays.asList((Service[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(payload);
		out.writeObject(destinations.toArray(new Service[0]));
	}
}
