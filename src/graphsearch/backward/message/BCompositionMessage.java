package graphsearch.backward.message;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.MessageID;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class BCompositionMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// stores the services in the order they are added to the composition
	private final Set<Service> compositionServices = new HashSet<Service>();

	// the destination services of this message
	private final Set<ServiceDistance> destServices = new HashSet<ServiceDistance>();
	// a table which saves the distance among services
	private final Map<Service, Set<ServiceDistance>> ancestorDistances = new HashMap<Service, Set<ServiceDistance>>();

	// the service which originates the message
	private final Service sourceService;

	// the identifier of the current search
	private final SearchID searchID;

	// the message partition
	private final MessagePart messagePart;

	private final int ttl;

	private final long remainingTime;
	
	public BCompositionMessage() {
		sourceService = null;
		searchID = null;
		messagePart = null;
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
	 * @param ttl
	 *            the TTL of the message
	 * @param remainingTime
	 *            the remaining time for this message to expire
	 * @param peerID
	 *            the peerID which created the message
	 */
	public BCompositionMessage(final SearchID searchID, final Service sourceService, final Set<ServiceDistance> destServices, final int ttl, final long remainingTime, final PeerID peerID) {
		super(sourceService.getPeerID(), new ArrayList<PeerID>());
		this.searchID = searchID;
		this.destServices.addAll(destServices);
		this.sourceService = sourceService;
		this.ttl = ttl;
		this.remainingTime = remainingTime;
		this.messagePart = new MessagePart(peerID);

		compositionServices.add(sourceService);

		ancestorDistances.put(sourceService, destServices);
	}

	private BCompositionMessage(final SearchID searchID, final Service sourceService, final Set<ServiceDistance> destServices, final int ttl, final long remainingTime, final MessagePart newMessagePart) {
		super(sourceService.getPeerID(), new ArrayList<PeerID>());
		this.searchID = searchID;
		this.destServices.addAll(destServices);
		this.sourceService = sourceService;
		this.ttl = ttl;
		this.remainingTime = remainingTime;
		this.messagePart = newMessagePart;

		compositionServices.add(sourceService);

		ancestorDistances.put(sourceService, destServices);
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

	public Map<Service, Set<ServiceDistance>> getAncestorDistances() {
		return ancestorDistances;
	}

	@Override
	public String toString() {
		return super.toString() + " searchID: " + searchID + " services: " + compositionServices.toString();
	}

	public MessageID getRootID() {
		return messagePart.getRootID();
	}

	public MessagePart getMessagePart() {
		return messagePart;
	}

	public Map<Service, BCompositionMessage> split(final Service service, final Set<ServiceDistance> destinationServices, final PeerID peerID, final long messagesRemainingTime) {
		final Map<Service, BCompositionMessage> messages = new HashMap<Service, BCompositionMessage>();
		final Set<MessagePart> messageParts = this.messagePart.split(destinationServices.size(), peerID);
		final Iterator<MessagePart> it = messageParts.iterator();
		for (final ServiceDistance antecessor : destinationServices) {
			final MessagePart newMessagePart = it.next();
			final BCompositionMessage message = new BCompositionMessage(this.searchID, service, destinationServices, this.ttl - 1, messagesRemainingTime, newMessagePart);
			// add the ancestor
			message.getComposition().add(antecessor.getService());
			// add the current composition
			message.getComposition().addAll(this.getComposition());
			messages.put(antecessor.getService(), message);

			copyDistances(message.ancestorDistances, this.ancestorDistances);

			// add new distances
			final Map<Service, Set<ServiceDistance>> newDistances = new HashMap<Service, Set<ServiceDistance>>();
			newDistances.put(service, destinationServices);
			copyDistances(message.ancestorDistances, newDistances);
		}
		return messages;
	}

	@Override
	public PayloadMessage copy() {
		final BCompositionMessage bCompositionMessage = new BCompositionMessage(getSearchID(), getSourceService(), getDestServices(), getTTL(), getRemainingTime(), getMessagePart());
		bCompositionMessage.compositionServices.addAll(compositionServices);

		copyDistances(bCompositionMessage.ancestorDistances, this.ancestorDistances);

		return bCompositionMessage;
	}

	private void copyDistances(final Map<Service, Set<ServiceDistance>> destination, final Map<Service, Set<ServiceDistance>> original) {
		for (final Entry<Service, Set<ServiceDistance>> entry : original.entrySet()) {
			final Service service = entry.getKey();
			if (!destination.containsKey(service))
				destination.put(service, new HashSet<ServiceDistance>());
			destination.get(service).addAll(entry.getValue());
		}
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		compositionServices.addAll(Arrays.asList((Service[])in.readObject()));
		destServices.addAll(Arrays.asList((ServiceDistance[])in.readObject()));
		readMap(ancestorDistances, in);
		UnserializationUtils.setFinalField(BCompositionMessage.class, this, "sourceService", in.readObject());
		UnserializationUtils.setFinalField(BCompositionMessage.class, this, "searchID", in.readObject());
		UnserializationUtils.setFinalField(BCompositionMessage.class, this, "ttl", in.readInt());
		UnserializationUtils.setFinalField(BCompositionMessage.class, this, "messagePart", in.readObject());
		UnserializationUtils.setFinalField(BCompositionMessage.class, this, "remainingTime", in.readLong());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(compositionServices.toArray(new Service[0]));
		out.writeObject(destServices.toArray(new ServiceDistance[0]));
		writeMap(ancestorDistances, out);
		out.writeObject(sourceService);
		out.writeObject(searchID);
		out.writeInt(ttl);
		out.writeObject(messagePart);
		out.writeLong(remainingTime);
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
