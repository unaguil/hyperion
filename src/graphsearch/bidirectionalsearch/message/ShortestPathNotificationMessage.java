package graphsearch.bidirectionalsearch.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public abstract class ShortestPathNotificationMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final SearchID searchID;

	protected final Map<Service, Set<ServiceDistance>> serviceDistances = new HashMap<Service, Set<ServiceDistance>>();

	protected final List<Service> notificationPath = new ArrayList<Service>();

	protected final Service destination;
	
	public ShortestPathNotificationMessage() {
		searchID = null;
		destination = null;
	}

	public ShortestPathNotificationMessage(final PeerID source, final SearchID searchID, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath) {
		super(source, Collections.<PeerID> emptySet());

		this.searchID = searchID;

		this.serviceDistances.putAll(serviceDistances);

		this.notificationPath.addAll(notificationPath);

		this.destination = notificationPath.get(notificationPath.size() - 1);
	}

	protected ShortestPathNotificationMessage(final PeerID source, final SearchID searchID, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath, final Service destination) {
		super(source, Collections.<PeerID> emptySet());

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

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.setFinalField(ShortestPathNotificationMessage.class, this, "searchID", in.readObject());
		readMap(serviceDistances, in);
		notificationPath.addAll(Arrays.asList((Service[])in.readObject()));
		UnserializationUtils.setFinalField(ShortestPathNotificationMessage.class, this, "destination", in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(searchID);
		writeMap(serviceDistances, out);
		out.writeObject(notificationPath.toArray(new Service[0]));
		out.writeObject(destination);
	}
	
	private void writeMap(Map<Service, Set<ServiceDistance>> map, ObjectOutput out) throws IOException {
		out.writeObject(map.keySet().toArray(new Service[0]));
		out.writeInt(map.values().size());
		for (Set<ServiceDistance> set : map.values())
			out.writeObject(set.toArray(new ServiceDistance[0]));
	}
	
	private void readMap(Map<Service, Set<ServiceDistance>> map, ObjectInput in) throws ClassNotFoundException, IOException {
		List<Service> keys = Arrays.asList((Service[])in.readObject());
		int size = in.readInt();
		List<Set<ServiceDistance>> values = new ArrayList<Set<ServiceDistance>>();
		for (int i = 0; i < size; i++) {
			Set<ServiceDistance> value = new HashSet<ServiceDistance>(Arrays.asList((ServiceDistance[])in.readObject()));
			values.add(value);
		}
		
		UnserializationUtils.fillMap(map, keys, values);
	}
}
