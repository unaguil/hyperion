/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

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
import java.util.Map;
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

	public boolean addReceivedMessage(final Service service, final FCompositionMessage fCompositionMessage) {
		synchronized (entries) {
			if (!entries.containsKey(fCompositionMessage.getSearchID()))
				entries.put(fCompositionMessage.getSearchID(), new SearchEntry(fCompositionMessage.getRemainingTime(), gCreator));

			entries.get(fCompositionMessage.getSearchID()).addReceivedMessage(service, fCompositionMessage);
			return entries.get(fCompositionMessage.getSearchID()).hasChanged();
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

	public Map<SearchID, Set<FCompositionMessage>> getReceivedMessages(final Service service) {
		final Map<SearchID, Set<FCompositionMessage>> receivedMessages = new HashMap<SearchID, Set<FCompositionMessage>>();
		synchronized (entries) {
			for (final SearchID searchID : entries.keySet()) {
				final Set<FCompositionMessage> messages = entries.get(searchID).getMessages(service);
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
	
	public void addForwardedSuccessor(final SearchID searchID, final Service successor) {
		synchronized (entries) {
			if (entries.containsKey(searchID))
				entries.get(searchID).addForwardedSuccessor(successor);
		}
	}
	
	public boolean wasAlreadyForwarded(final SearchID searchID, final Service successor) {
		synchronized (entries) {
			if (entries.containsKey(searchID))
				return entries.get(searchID).wasAlreadyForwarded(successor);
			return false;
		}
	}
}