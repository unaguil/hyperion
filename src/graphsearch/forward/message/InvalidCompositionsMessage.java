package graphsearch.forward.message;

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

public class InvalidCompositionsMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<SearchID, Service> invalidServices = new HashMap<SearchID, Service>();
	private final Map<SearchID, Set<Service>> currentSuccessors = new HashMap<SearchID, Set<Service>>();
	private final Map<SearchID, Set<Service>> invalidCompositions = new HashMap<SearchID, Set<Service>>();
	
	public InvalidCompositionsMessage() {
		
	}

	public InvalidCompositionsMessage(final PeerID source) {
		super(source, Collections.<PeerID> emptySet());
	}

	public void addInvalidLocalService(final SearchID searchID, final Service invalidLocalService, final Set<ServiceDistance> successors) {
		invalidServices.put(searchID, invalidLocalService);

		if (!currentSuccessors.containsKey(searchID))
			currentSuccessors.put(searchID, new HashSet<Service>());
		for (final ServiceDistance successor : successors)
			currentSuccessors.get(searchID).add(successor.getService());
	}

	public Set<Service> getSuccessors() {
		final Set<Service> successors = new HashSet<Service>();
		for (final Set<Service> partialSuccessors : currentSuccessors.values())
			successors.addAll(partialSuccessors);
		return successors;
	}

	public void addInvalidComposition(final SearchID searchID, final Set<Service> invalidComposition) {
		invalidCompositions.put(searchID, new HashSet<Service>(invalidComposition));
	}

	@Override
	public PayloadMessage copy() {
		final InvalidCompositionsMessage compositionModificationMessage = new InvalidCompositionsMessage(getSource());
		compositionModificationMessage.invalidServices.putAll(this.invalidServices);
		compositionModificationMessage.currentSuccessors.putAll(this.currentSuccessors);
		compositionModificationMessage.invalidCompositions.putAll(this.invalidCompositions);
		return compositionModificationMessage;
	}

	public Map<Service, Set<Service>> getLostAncestors() {
		final Map<Service, Set<Service>> lostAncestors = new HashMap<Service, Set<Service>>();

		for (final SearchID searchID : currentSuccessors.keySet())
			for (final Service successor : currentSuccessors.get(searchID)) {
				final Service lostAncestor = invalidServices.get(searchID);
				if (!lostAncestors.containsKey(successor))
					lostAncestors.put(successor, new HashSet<Service>());
				lostAncestors.get(successor).add(lostAncestor);
			}

		return lostAncestors;
	}

	public Map<SearchID, Set<Service>> getInvalidCompositions() {
		return invalidCompositions;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		readMap(currentSuccessors, in);
		readMap(invalidCompositions, in);
		
		UnserializationUtils.readMap(invalidServices, in);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		writeMap(currentSuccessors, out);
		writeMap(invalidCompositions, out);
		
		out.writeObject(invalidServices.keySet().toArray(new SearchID[0]));
		out.writeObject(invalidServices.values().toArray(new Service[0]));
	}
	
	private void writeMap(Map<SearchID, Set<Service>> map, ObjectOutput out) throws IOException {
		out.writeObject(map.keySet().toArray(new SearchID[0]));
		out.writeInt(map.values().size());
		for (Set<Service> set : map.values())
			out.writeObject(set.toArray(new Service[0]));
	}
	
	private void readMap(Map<SearchID, Set<Service>> map, ObjectInput in) throws ClassNotFoundException, IOException {
		List<SearchID> keys = Arrays.asList((SearchID[])in.readObject());
		int size = in.readInt();
		List<Set<Service>> values = new ArrayList<Set<Service>>();
		for (int i = 0; i < size; i++) {
			Set<Service> value = new HashSet<Service>(Arrays.asList((Service[])in.readObject()));
			values.add(value);
		}
		
		UnserializationUtils.fillMap(map, keys, values);
	}
}
