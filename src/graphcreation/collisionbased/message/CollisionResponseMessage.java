package graphcreation.collisionbased.message;

import graphcreation.services.Service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class CollisionResponseMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<Service, Integer> serviceDistanceTable = new HashMap<Service, Integer>();
	
	public CollisionResponseMessage() {
		
	}

	public CollisionResponseMessage(final PeerID source, final Map<Service, Integer> serviceTable) {
		super(source, Collections.<PeerID> emptySet());
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
		final Map<Service, Integer> serviceDistanceTableCopy = new HashMap<Service, Integer>();
		for (final Entry<Service, Integer> entry : serviceDistanceTable.entrySet())
			serviceDistanceTableCopy.put(entry.getKey(), new Integer(entry.getValue().intValue()));
		
		return new CollisionResponseMessage(getSource(), serviceDistanceTableCopy);
	}

	public void addDistance(final int distance) {
		for (final Service service : serviceDistanceTable.keySet()) {
			final Integer newDistance = Integer.valueOf(serviceDistanceTable.get(service).intValue() + distance);
			serviceDistanceTable.put(service, newDistance);
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.readMap(serviceDistanceTable, in);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(serviceDistanceTable.keySet().toArray(new Service[0]));
		out.writeObject(serviceDistanceTable.values().toArray(new Integer[0]));
	}
}
