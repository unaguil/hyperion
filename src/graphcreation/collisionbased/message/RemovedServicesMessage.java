package graphcreation.collisionbased.message;

import graphcreation.services.Service;

import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.PeerID;
import peer.message.PayloadMessage;

public class RemovedServicesMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Set<Service> lostServices = new HashSet<Service>();

	public RemovedServicesMessage(final Set<Service> lostServices, final PeerID source) {
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
		return new RemovedServicesMessage(getLostServices(), getSource());
	}
}
