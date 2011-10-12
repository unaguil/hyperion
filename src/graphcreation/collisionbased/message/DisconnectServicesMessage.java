package graphcreation.collisionbased.message;

import graphcreation.services.Service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class DisconnectServicesMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Set<Service> lostServices = new HashSet<Service>();
	
	public DisconnectServicesMessage() {
		
	}

	public DisconnectServicesMessage(final Set<Service> lostServices, final PeerID source) {
		super(source);
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
	public PayloadMessage copy() {
		return new DisconnectServicesMessage(getLostServices(), getSource());
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		lostServices.addAll(Arrays.asList((Service[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(lostServices.toArray(new Service[0]));
	}
}
