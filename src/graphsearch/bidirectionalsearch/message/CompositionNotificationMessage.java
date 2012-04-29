package graphsearch.bidirectionalsearch.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.bidirectionalsearch.Util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.peerid.PeerID;

public class CompositionNotificationMessage extends ShortestPathNotificationMessage {
	
	public CompositionNotificationMessage() {
		super(MessageTypes.COMPOSITION_NOTIFICATION_MESSAGE);
	}

	public CompositionNotificationMessage(final PeerID source, final SearchID searchID, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath) {
		super(MessageTypes.COMPOSITION_NOTIFICATION_MESSAGE, source, searchID, serviceDistances, notificationPath);
	}

	protected CompositionNotificationMessage(final PeerID source, final SearchID searchID, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath, final Service destination) {
		super(MessageTypes.COMPOSITION_NOTIFICATION_MESSAGE, source, searchID, serviceDistances, notificationPath, destination);
	}

	public Set<Service> getComposition() {
		return Util.getAllServices(serviceDistances);
	}

	@Override
	public BroadcastMessage copy() {
		final CompositionNotificationMessage message = new CompositionNotificationMessage(getSource(), getSearchID(), serviceDistances, notificationPath, destination);
		return message;
	}
}
