package graphsearch.forward.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class FCompositionMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// stores the services in the order they are added to the composition
	private final Set<Service> compositionServices = new HashSet<Service>();

	// the destination services of this message
	private final Set<ServiceDistance> destServices = new HashSet<ServiceDistance>();

	// the service which originates the message
	private final Service sourceService;

	// a table which saves the distance among services
	private final Map<Service, Set<ServiceDistance>> successorDistances = new HashMap<Service, Set<ServiceDistance>>();

	// the identifier of the current search
	private final SearchID searchID;

	private final int ttl;

	private final long remainingTime;
	
	private int hops = 0;
	
	public FCompositionMessage() {
		sourceService = null;
		searchID = null;
		ttl = 0;
		remainingTime = 0;
	}

	/**
	 * Constructor of the forward composition message
	 * 
	 * @param searchID
	 *            the identifier of the current search
	 * @param sourceService
	 *            the service which originates the message
	 * @param destServices
	 *            the services which are the destination of this message
	 * @param the
	 *            TTL of the message
	 * @param the
	 *            remaining time for this message to expire
	 */
	public FCompositionMessage(final SearchID searchID, final Service sourceService, final Set<ServiceDistance> destServices, final int ttl, final long remainingTime) {
		super(sourceService.getPeerID(), new ArrayList<PeerID>());
		this.searchID = searchID;
		this.destServices.addAll(destServices);
		this.sourceService = sourceService;
		this.ttl = ttl;
		this.remainingTime = remainingTime;

		compositionServices.add(sourceService);

		successorDistances.put(sourceService, new HashSet<ServiceDistance>());
		successorDistances.get(sourceService).addAll(destServices);
	}

	/**
	 * Gets the identifier of the current search
	 * 
	 * @return the identifier of the associated search
	 */
	public SearchID getSearchID() {
		return searchID;
	}

	/**
	 * The services which participate in the composition
	 * 
	 * @return the collection of services which participate in the composition
	 */
	public Set<Service> getComposition() {
		return compositionServices;
	}

	/**
	 * The service which originates the message
	 * 
	 * @return the service which originates the message
	 */
	public Service getSourceService() {
		return sourceService;
	}

	/**
	 * Gets the current TTL of the message
	 * 
	 * @return the current TTL of the message
	 */
	public int getTTL() {
		return ttl;
	}
	
	public int getHops() {
		return hops;
	}
	
	public void addHops(int hops) {
		this.hops += hops;		
	}

	/**
	 * Gets the remaining time for this message to expire
	 * 
	 * @return the remaining time for the expiration of this message
	 */
	public long getRemainingTime() {
		return remainingTime;
	}

	/**
	 * The set of services which are the destinations of this message
	 * 
	 * @return the destination services
	 */
	public Set<ServiceDistance> getDestServices() {
		return destServices;
	}

	@Override
	public String toString() {
		return super.toString() + " searchID: " + searchID + " services: " + compositionServices.toString();
	}

	@Override
	public PayloadMessage copy() {
		final FCompositionMessage fCompositionMessage = new FCompositionMessage(getSearchID(), getSourceService(), getDestServices(), getTTL(), getRemainingTime());
		fCompositionMessage.compositionServices.addAll(this.compositionServices);
		fCompositionMessage.successorDistances.putAll(this.successorDistances);
		fCompositionMessage.hops = this.hops;
		return fCompositionMessage;
	}

	public void join(final FCompositionMessage fCompositionMessage) {
		compositionServices.addAll(fCompositionMessage.compositionServices);

		for (final Service service : fCompositionMessage.successorDistances.keySet()) {
			if (!successorDistances.containsKey(service))
				successorDistances.put(service, new HashSet<ServiceDistance>());
			successorDistances.get(service).addAll(fCompositionMessage.successorDistances.get(service));
		}
		
		hops += fCompositionMessage.hops;
	}

	public Map<Service, Set<ServiceDistance>> getSuccessorDistances() {
		return successorDistances;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		compositionServices.addAll(Arrays.asList((Service[])in.readObject()));
		destServices.addAll(Arrays.asList((ServiceDistance[])in.readObject()));
		readMap(successorDistances, in);
		UnserializationUtils.setFinalField(FCompositionMessage.class, this, "sourceService", in.readObject());
		UnserializationUtils.setFinalField(FCompositionMessage.class, this, "searchID", in.readObject());
		UnserializationUtils.setFinalField(FCompositionMessage.class, this, "ttl", in.readInt());
		UnserializationUtils.setFinalField(FCompositionMessage.class, this, "remainingTime", in.readLong());
		hops = in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(compositionServices.toArray(new Service[0]));
		out.writeObject(destServices.toArray(new ServiceDistance[0]));
		writeMap(successorDistances, out);
		out.writeObject(sourceService);
		out.writeObject(searchID);
		out.writeInt(ttl);
		out.writeLong(remainingTime);
		out.writeInt(hops);
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
