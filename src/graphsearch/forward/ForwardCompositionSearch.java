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

package graphsearch.forward;

import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.CompositionListener;
import graphsearch.SearchID;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;
import graphsearch.forward.forwardCompositionTable.ForwardCompositionData;
import graphsearch.forward.message.FCompositionMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import peer.peerid.PeerID;

public class ForwardCompositionSearch extends CommonCompositionSearch {

	private ForwardComposer forwardComposer;

	public ForwardCompositionSearch(final ReliableBroadcastPeer peer, final CompositionListener compositionListener) {
		super(peer, compositionListener, GraphType.FORWARD);
	}

	@Override
	public void init() {
		super.init();

		compositionData = new ForwardCompositionData(EXPIRATION_CHECK_TIME, this, gCreator);
		compositionData.start();

		forwardComposer = new ForwardComposer((ForwardCompositionData) compositionData, this, DIRECT_BROADCAST, MULTIPLE_PATHS);
	}
	
	@Override
	public void stop() {
		super.stop();
	}
	
	@Override
	public SearchID startComposition(final Service searchedService) {
		final SearchID searchID = super.startComposition(searchedService);
		forwardComposer.initFComposition(getInitService(searchedService), searchID);
		return searchID;
	}

	@Override
	public void newSuccessors(final Map<Service, Set<ServiceDistance>> newSuccessors) {
		forwardComposer.newSuccessors(newSuccessors);
	}

	@Override
	public void newAncestors(final Map<Service, Set<ServiceDistance>> newAncestors) {
		forwardComposer.newAncestors(newAncestors);
	}

	@Override
	public void messageReceived(final BroadcastMessage message, final long receptionTime) {
	}

	@Override
	public void multicastMessageAccepted(final PeerID source, final BroadcastMessage payload, final int distance, final boolean directBroadcast) {
		if (payload instanceof FCompositionMessage) {
			final FCompositionMessage fCompositionMessage = (FCompositionMessage) payload;
			fCompositionMessage.addHops(distance);
			forwardComposer.receivedFComposition(fCompositionMessage);
		}
	}

	@Override
	public void acceptShortestPathNotificationMessage(final ShortestPathNotificationMessage shortestPathNotificationMessage) {
	}

	@Override
	public boolean merge(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
		return false;		
	}

	@Override
	public void initFComposition(final Service initService, final SearchID searchID) {
		if (MSG_INTERVAL > 0)
			forwardComposer.initFComposition(initService, searchID);
	}
}
