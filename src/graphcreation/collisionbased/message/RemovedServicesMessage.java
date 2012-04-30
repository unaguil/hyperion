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

public class RemovedServicesMessage extends RemoteMessage {

	private final Set<Service> lostServices = new HashSet<Service>();
	
	public RemovedServicesMessage() {
		super (MessageTypes.REMOVED_SERVICE_MESSAGE);
	}

	public RemovedServicesMessage(final PeerID source, final Set<Service> lostServices) {
		super(MessageTypes.REMOVED_SERVICE_MESSAGE, source, null, Collections.<PeerID> emptySet());
		this.lostServices.addAll(lostServices);
	}

	public Set<Service> getLostServices() {
		return lostServices;
	}

	@Override
	public String toString() {
		return super.toString() + " " + lostServices;
	}

	@Override
	public BroadcastMessage copy() {
		return new RemovedServicesMessage(getSource(), getLostServices());
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readServices(lostServices, in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(lostServices, out);
	}
}
