package graphcreation.collisionbased.message;

import graphcreation.services.Service;

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

public class DisconnectServicesMessage extends RemoteMessage {

	private final Set<Service> lostServices = new HashSet<Service>();
	
	private final boolean servicesWereRemoved;
	
	public DisconnectServicesMessage() {
		super(MessageTypes.DISCONNECT_SERVICES_MESSAGE);
		this.servicesWereRemoved = true;
	}

	public DisconnectServicesMessage(final PeerID source, final Set<Service> lostServices, final boolean servicesWereRemoved) {
		super(MessageTypes.DISCONNECT_SERVICES_MESSAGE, source, null, Collections.<PeerID> emptySet());
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
	public BroadcastMessage copy() {
		return new DisconnectServicesMessage(getSource(), getLostServices(), servicesWereRemoved);
	}
	
	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readServices(lostServices, in);
		SerializationUtils.setFinalField(DisconnectServicesMessage.class, this, "servicesWereRemoved", in.readBoolean());
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(lostServices, out);
		out.writeBoolean(servicesWereRemoved);
	}
}
