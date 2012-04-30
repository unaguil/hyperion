package graphsearch.forward.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

public class FCompositionMessage extends RemoteMessage {

	// stores the services in the order they are added to the composition
	private final Set<Service> compositionServices = new HashSet<Service>();

	// the destination services of this message
	private final Set<ServiceDistance> destServices = new HashSet<ServiceDistance>();
	
	// a table which saves the distance among services
	private final Map<Service, Set<ServiceDistance>> successorDistances = new HashMap<Service, Set<ServiceDistance>>();

	// the service which originates the message
	private final Service sourceService;

	// the identifier of the current search
	private final SearchID searchID;

	private final short ttl;

	private final long remainingTime;
	
	private short hops = 0;
	
	public FCompositionMessage() {
		super(MessageTypes.FCOMPOSITION_MESSAGE);
		sourceService = new Service();
		searchID = new SearchID();
		ttl = 0;
		remainingTime = 0;
	}
	
	public Map<Service, Set<ServiceDistance>> getSuccessorDistances() {
		return successorDistances;
	}

	/**
	 * Constructor of the forward composition message
	 * 
	 * @param searchID
	 *            the identifier of the current search
	 * @param sourceService
	 *            the service which originates the message
	 * @param the
	 *            TTL of the message
	 * @param the
	 *            remaining time for this message to expire
	 */
	public FCompositionMessage(final SearchID searchID, final Service sourceService, final Set<ServiceDistance> destServices, final int ttl, final long remainingTime) {
		super(MessageTypes.FCOMPOSITION_MESSAGE, sourceService.getPeerID(), null, Collections.<PeerID> emptySet());
		this.searchID = searchID;
		this.sourceService = sourceService;
		this.destServices.addAll(destServices);
		this.ttl = (short)ttl;
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
	
	public void addHops(int addedHops) {
		this.hops += addedHops;		
	}

	/**
	 * Gets the remaining time for this message to expire
	 * 
	 * @return the remaining time for the expiration of this message
	 */
	public long getRemainingTime() {
		return remainingTime;
	}

	@Override
	public String toString() {
		return super.toString() + " searchID: " + searchID + " services: " + compositionServices.toString();
	}

	@Override
	public BroadcastMessage copy() {
		final FCompositionMessage fCompositionMessage = new FCompositionMessage(getSearchID(), getSourceService(), getDestServices(), getTTL(), getRemainingTime());
		fCompositionMessage.compositionServices.addAll(this.compositionServices);
		fCompositionMessage.hops = this.hops;
		fCompositionMessage.successorDistances.putAll(this.successorDistances);
		return fCompositionMessage;
	}

	public void join(final FCompositionMessage fCompositionMessage) {
		compositionServices.addAll(fCompositionMessage.compositionServices);
		
		for (final Service service : fCompositionMessage.successorDistances.keySet()) {
			if (!successorDistances.containsKey(service))
				successorDistances.put(service, new HashSet<ServiceDistance>());
			successorDistances.get(service).addAll(fCompositionMessage.successorDistances.get(service));
		}
		
		hops = (short) Math.max(hops, fCompositionMessage.getHops());
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
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readServices(compositionServices, in);
		SerializationUtils.readServiceDistances(destServices, in);
		
		sourceService.read(in);
		searchID.read(in);
		SerializationUtils.setFinalField(FCompositionMessage.class, this, "ttl", in.readShort());
		SerializationUtils.setFinalField(FCompositionMessage.class, this, "remainingTime", in.readLong());
		SerializationUtils.readServiceMap(successorDistances, in);
		hops = in.readShort();
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(compositionServices, out);
		SerializationUtils.writeCollection(destServices, out);
		sourceService.write(out);
		searchID.write(out);
		out.writeShort(ttl);
		out.writeLong(remainingTime);
		SerializationUtils.writeServiceMap(successorDistances, out);
		out.writeShort(hops);
	}
}
