package graphcreation.collisionbased.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class ConnectServicesMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<Service, Set<ServiceDistance>> remoteSuccessors = new HashMap<Service, Set<ServiceDistance>>();
	private final Map<Service, Set<ServiceDistance>> remoteAncestors = new HashMap<Service, Set<ServiceDistance>>();

	public ConnectServicesMessage(final Map<Service, Set<ServiceDistance>> remoteSuccessors, final Map<Service, Set<ServiceDistance>> remoteAncestors, final PeerID source) {
		super(source);
		this.remoteAncestors.putAll(remoteAncestors);
		this.remoteSuccessors.putAll(remoteSuccessors);
	}

	public Map<Service, Set<ServiceDistance>> getRemoteSuccessors() {
		return remoteSuccessors;
	}

	public Map<Service, Set<ServiceDistance>> getRemoteAncestors() {
		return remoteAncestors;
	}

	@Override
	public String toString() {
		return super.toString() + " remoteAncestors: " + remoteAncestors + " remoteSuccessors: " + remoteSuccessors;
	}

	@Override
	public PayloadMessage copy() {
		return new ConnectServicesMessage(getRemoteSuccessors(), getRemoteAncestors(), getSource());
	}
}
