package graphsearch.forward.forwardCompositionTable;

import graphcreation.GraphCreator;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.compositionData.CompositionData;
import graphsearch.compositionData.localSearchesTable.SearchExpiredListener;
import graphsearch.forward.message.FCompositionMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ForwardCompositionData extends CompositionData {

	private final Map<SearchID, SearchEntry> entries = new HashMap<SearchID, SearchEntry>();

	public ForwardCompositionData(final long checkTime, final SearchExpiredListener searchExpiredListener, final GraphCreator gCreator) {
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

	public boolean areAllInputsCovered(final SearchID searchID, final Service service) {
		synchronized (entries) {
			if (!entries.containsKey(searchID))
				return false;
			return entries.get(searchID).areAllInputsCovered(service);
		}
	}

	public void addReceivedMessage(final Service service, final FCompositionMessage fCompositionMessage) {
		synchronized (entries) {
			if (!entries.containsKey(fCompositionMessage.getSearchID()))
				entries.put(fCompositionMessage.getSearchID(), new SearchEntry(fCompositionMessage.getRemainingTime(), gCreator));

			entries.get(fCompositionMessage.getSearchID()).addReceivedMessage(service, fCompositionMessage);
		}
	}
	
	public void addCoveredService(final SearchID searchID, final Service service) {
		synchronized (entries) {
			entries.get(searchID).addCoveredService(service);
		}
	}

	public Set<SearchID> removeMessagesReceivedFrom(final Service service, final Service ancestor) {
		final Set<SearchID> affectedSearches = new HashSet<SearchID>();
		synchronized (entries) {
			for (final SearchID searchID : entries.keySet()) {
				final SearchEntry searchEntry = entries.get(searchID);
				if (searchEntry.removeMessagesFrom(service, ancestor))
					affectedSearches.add(searchID);
			}
		}
		return affectedSearches;
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

	public Map<SearchID, List<FCompositionMessage>> getReceivedMessages(final Service service) {
		final Map<SearchID, List<FCompositionMessage>> receivedMessages = new HashMap<SearchID, List<FCompositionMessage>>();
		synchronized (entries) {
			for (final SearchID searchID : entries.keySet()) {
				final List<FCompositionMessage> messages = entries.get(searchID).getMessages(service);
				if (!messages.isEmpty())
					receivedMessages.put(searchID, messages);
			}
		}

		return receivedMessages;
	}

	public long getRemainingTime(final SearchID searchID) {
		synchronized (entries) {
			if (!entries.containsKey(searchID))
				return 0;
			return entries.get(searchID).getRemainingTime();
		}
	}

	public Set<Service> getServicesReceivingMessagesFrom(final SearchID searchID, final Service ancestor) {
		if (entries.containsKey(searchID))
			return entries.get(searchID).getServicesReceivingMessagesFrom(ancestor);
		return new HashSet<Service>();
	}
	
	public Map<SearchID, Set<Service>> getCoveredServices() {
		final Map<SearchID, Set<Service>> coveredServices = new HashMap<SearchID, Set<Service>>();
		synchronized (entries) {
			for (final Entry<SearchID, SearchEntry> entry : entries.entrySet())
				coveredServices.put(entry.getKey(), entry.getValue().getCoveredServices());
		}
		return coveredServices;
	}
}