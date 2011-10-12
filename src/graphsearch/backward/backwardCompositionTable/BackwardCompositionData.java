package graphsearch.backward.backwardCompositionTable;

import graphcreation.GraphCreator;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.backward.MessageTree;
import graphsearch.backward.message.BCompositionMessage;
import graphsearch.compositionData.CompositionData;
import graphsearch.compositionData.localSearchesTable.SearchExpiredListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BackwardCompositionData extends CompositionData {

	private final Map<Service, Set<Set<ServiceDistance>>> coveringSetTable = new HashMap<Service, Set<Set<ServiceDistance>>>();

	private final Map<SearchID, SearchEntry> entries = new HashMap<SearchID, SearchEntry>();

	public BackwardCompositionData(final long checkTime, final SearchExpiredListener searchExpiredListener, final GraphCreator gCreator) {
		super(checkTime, searchExpiredListener, gCreator);
	}

	@Override
	public void perform() {
		super.perform();
		// remove expired searches
		synchronized (entries) {
			for (final Iterator<SearchID> it = entries.keySet().iterator(); it.hasNext();) {
				final SearchID searchID = it.next();
				final long elapsedTime = System.currentTimeMillis() - entries.get(searchID).getTimestamp();
				if (elapsedTime >= entries.get(searchID).getRemainingTime())
					it.remove();
			}
		}
	}

	public MessageTree addReceivedMessage(final Service service, final BCompositionMessage bCompositionMessage) {
		synchronized (entries) {
			if (!entries.containsKey(bCompositionMessage.getSearchID()))
				entries.put(bCompositionMessage.getSearchID(), new SearchEntry(bCompositionMessage.getRemainingTime()));

			return entries.get(bCompositionMessage.getSearchID()).addReceivedMessage(service, bCompositionMessage);
		}
	}

	public void removeEntry(final SearchID searchID) {
		synchronized (entries) {
			entries.remove(searchID);
		}
	}

	public void removeService(final Service service) {
		synchronized (entries) {
			for (final SearchEntry searchEntry : entries.values())
				searchEntry.removeService(service);
		}
	}

	public Set<Set<ServiceDistance>> getCoveringSets(final Service service) {
		if (!coveringSetTable.containsKey(service))
			return new HashSet<Set<ServiceDistance>>();
		return coveringSetTable.get(service);
	}

	public void addCoveringSets(final Service service, final Set<Set<ServiceDistance>> newCoveringSets) {
		if (!coveringSetTable.containsKey(service))
			coveringSetTable.put(service, new HashSet<Set<ServiceDistance>>());
		coveringSetTable.get(service).addAll(newCoveringSets);
	}

	public void removeCoveringSets(final Service service, final Set<ServiceDistance> removedServices) {
		if (coveringSetTable.containsKey(service)) {
			for (final Iterator<Set<ServiceDistance>> it = coveringSetTable.get(service).iterator(); it.hasNext();) {
				final Set<ServiceDistance> coveringSet = it.next();
				final int previousSize = coveringSet.size();
				coveringSet.removeAll(removedServices);
				if (previousSize > coveringSet.size())
					it.remove();
			}

			if (coveringSetTable.get(service).isEmpty())
				coveringSetTable.remove(service);
		}
	}

	public Map<SearchID, List<BCompositionMessage>> getReceivedMessages(final Service service) {
		final Map<SearchID, List<BCompositionMessage>> receivedMessages = new HashMap<SearchID, List<BCompositionMessage>>();

		synchronized (entries) {
			for (final SearchID searchID : entries.keySet()) {
				final List<BCompositionMessage> messages = entries.get(searchID).getMessages(service);
				if (!messages.isEmpty())
					receivedMessages.put(searchID, messages);
			}
		}

		return receivedMessages;
	}

	public Set<MessageTree> getCompleteTrees(final SearchID searchID, final Service service) {
		if (entries.containsKey(searchID))
			return entries.get(searchID).getCompleteTrees(service);
		return new HashSet<MessageTree>();
	}
}
