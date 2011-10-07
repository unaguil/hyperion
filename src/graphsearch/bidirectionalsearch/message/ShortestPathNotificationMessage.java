package graphsearch.bidirectionalsearch.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.PeerID;
import peer.message.PayloadMessage;

public abstract class ShortestPathNotificationMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final SearchID searchID;

	protected final Map<Service, Set<ServiceDistance>> serviceDistances = new HashMap<Service, Set<ServiceDistance>>();

	protected final List<Service> notificationPath = new ArrayList<Service>();

	protected final Service destination;

	public ShortestPathNotificationMessage(final PeerID source, final SearchID searchID, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath) {
		super(source);

		this.searchID = searchID;

		this.serviceDistances.putAll(serviceDistances);

		this.notificationPath.addAll(notificationPath);

		this.destination = notificationPath.get(notificationPath.size() - 1);
	}

	protected ShortestPathNotificationMessage(final PeerID source, final SearchID searchID, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath, final Service destination) {
		super(source);

		this.searchID = searchID;

		this.serviceDistances.putAll(serviceDistances);

		this.notificationPath.addAll(notificationPath);

		this.destination = destination;
	}

	public SearchID getSearchID() {
		return searchID;
	}

	public boolean hasMoreElements() {
		return !notificationPath.isEmpty();
	}

	public Service nextService() {
		final Service service = notificationPath.get(0);
		notificationPath.remove(0);
		return service;
	}

	public Service getDestination() {
		return destination;
	}

	private int getDistance(final Service currentService, final Service nextService) {
		if (serviceDistances.containsKey(currentService))
			for (final ServiceDistance sDistance : serviceDistances.get(currentService))
				if (sDistance.getService().equals(nextService)) {
					final int distance = sDistance.getDistance().intValue();
					return distance;
				}
		return 0;
	}

	public int getPathDistance() {
		if (notificationPath.size() > 1) {
			int distance = 0;

			int current = 0;
			do {
				final Service currentService = notificationPath.get(current);
				final Service nextService = notificationPath.get(current + 1);

				distance += getDistance(currentService, nextService);
				distance += getDistance(nextService, currentService);

				current++;
			} while (current < notificationPath.size() - 1);

			return distance;
		}

		return 0;
	}

	@Override
	public String toString() {
		return "searchID: " + searchID + " notificationPath: " + notificationPath;
	}
}
