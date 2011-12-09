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
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class DisconnectServicesMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Set<Service> lostServices = new HashSet<Service>();
	
	private final boolean servicesWereRemoved;
	
	public DisconnectServicesMessage() {
		this.servicesWereRemoved = true;
	}

	public DisconnectServicesMessage(final PeerID source, final Set<Service> lostServices, final boolean servicesWereRemoved) {
		super(source, new ArrayList<PeerID>());
		this.lostServices.addAll(lostServices);
		this.servicesWereRemoved = servicesWereRemoved;
	}

	public Set<Service> getLostServices() {
		return lostServices;
	}
	
	public PeerID getServicesPeer() {
		return lostServices.iterator().next().getPeerID(); 
	}
	
	public boolean wereServicesRemoved() {
		return servicesWereRemoved;
	}

	@Override
	public String toString() {
		return super.toString() + " " + lostServices;
	}

	@Override
	public PayloadMessage copy() {
		return new DisconnectServicesMessage(getSource(), getLostServices(), servicesWereRemoved);
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		lostServices.addAll(Arrays.asList((Service[])in.readObject()));
		UnserializationUtils.setFinalField(DisconnectServicesMessage.class, this, "servicesWereRemoved", in.readBoolean());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(lostServices.toArray(new Service[0]));
		out.writeBoolean(servicesWereRemoved);
	}
}
