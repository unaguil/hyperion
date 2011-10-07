package graphsearch.forward.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.PeerID;
import peer.message.PayloadMessage;

public class CompositionModificationMessage extends ShortestPathNotificationMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Set<Service> removedServices = new HashSet<Service>();

	public CompositionModificationMessage(final PeerID source, final SearchID searchID, final Set<Service> removedServices, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath) {
		super(source, searchID, serviceDistances, notificationPath);
		this.removedServices.addAll(removedServices);
	}

	private CompositionModificationMessage(final PeerID source, final SearchID searchID, final Set<Service> removedServices, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath, final Service destination) {
		super(source, searchID, serviceDistances, notificationPath, destination);
		this.removedServices.addAll(removedServices);
	}

	public Set<Service> getRemovedServices() {
		return removedServices;
	}

	@Override
	public PayloadMessage copy() {
		final CompositionModificationMessage compositionModificationMessage = new CompositionModificationMessage(getSource(), getSearchID(), removedServices, serviceDistances, notificationPath, destination);
		return compositionModificationMessage;
	}
}
