package graphcreation.collisionbased.message;

import graphcreation.services.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.PeerID;
import peer.message.PayloadMessage;

public class CollisionResponseMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<Service, Integer> serviceDistanceTable = new HashMap<Service, Integer>();

	public CollisionResponseMessage(final Map<Service, Integer> serviceTable, final PeerID source) {
		super(source);
		serviceDistanceTable.putAll(serviceTable);
	}

	public Set<Service> getServices() {
		return serviceDistanceTable.keySet();
	}

	public Integer getDistance(final Service s) {
		return serviceDistanceTable.get(s);
	}

	public boolean removeServices(final Set<Service> services) {
		boolean removed = false;
		for (final Service service : services)
			if (serviceDistanceTable.containsKey(service)) {
				serviceDistanceTable.remove(service);
				removed = true;
			}
		return removed;
	}

	@Override
	public String toString() {
		return super.toString() + " " + serviceDistanceTable;
	}

	@Override
	public PayloadMessage copy() {
		return new CollisionResponseMessage(serviceDistanceTable, getSource());
	}

	public void addDistance(final int distance) {
		for (final Service service : serviceDistanceTable.keySet()) {
			final Integer newDistance = Integer.valueOf(serviceDistanceTable.get(service).intValue() + distance);
			serviceDistanceTable.put(service, newDistance);
		}
	}
}
