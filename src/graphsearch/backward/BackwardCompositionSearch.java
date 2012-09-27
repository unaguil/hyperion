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
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package graphsearch.backward;

import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.CompositionListener;
import graphsearch.SearchID;
import graphsearch.backward.backwardCompositionTable.BackwardCompositionData;
import graphsearch.backward.message.BCompositionMessage;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;

import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import peer.peerid.PeerID;

public class BackwardCompositionSearch extends CommonCompositionSearch {

	private BackwardComposer backwardComposer;

	public BackwardCompositionSearch(final ReliableBroadcastPeer peer, final CompositionListener compositionListener) {
		super(peer, compositionListener, GraphType.BACKWARD);
	}

	@Override
	public void init() {
		super.init();

		compositionData = new BackwardCompositionData(EXPIRATION_CHECK_TIME, this, gCreator);
		compositionData.start();

		backwardComposer = new BackwardComposer((BackwardCompositionData) compositionData, this);
	}

	@Override
	public void newSuccessors(final Map<Service, Set<ServiceDistance>> newSuccessors) {
	}

	@Override
	public void lostSuccessors(final Map<Service, Set<Service>> lostSuccessors) {
	}

	@Override
	public void lostAncestors(final Map<Service, Set<Service>> lostAncestors) {
	}

	@Override
	public void newAncestors(final Map<Service, Set<ServiceDistance>> newAncestors) {
		backwardComposer.newAncestors(newAncestors);
	}

	@Override
	public void multicastMessageAccepted(final PeerID source, final BroadcastMessage payload, final int distance, final boolean directBroadcast) {
		if (payload instanceof BCompositionMessage)
			backwardComposer.receivedBComposition((BCompositionMessage) payload);
	}

	@Override
	public void messageReceived(final BroadcastMessage message, final long receptionTime) {
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
	}
}
