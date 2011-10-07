package graphsearch.forward.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class InvalidCompositionsMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<SearchID, Service> invalidServices = new HashMap<SearchID, Service>();
	private final Map<SearchID, Set<Service>> currentSuccessors = new HashMap<SearchID, Set<Service>>();
	private final Map<SearchID, Set<Service>> invalidCompositions = new HashMap<SearchID, Set<Service>>();

	public InvalidCompositionsMessage(final PeerID source) {
		super(source);
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
}
