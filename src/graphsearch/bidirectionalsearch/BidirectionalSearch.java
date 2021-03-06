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

package graphsearch.bidirectionalsearch;

import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;
import graphsearch.CompositionListener;
import graphsearch.SearchID;
import graphsearch.backward.backwardCompositionTable.BackwardCompositionData;
import graphsearch.backward.message.BCompositionMessage;
import graphsearch.bidirectionalsearch.message.CompositionNotificationMessage;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;
import graphsearch.forward.forwardCompositionTable.ForwardCompositionData;
import graphsearch.forward.message.FCompositionMessage;
import graphsearch.shortestpathnotificator.ShortestPathCalculator;
import graphsearch.util.Utility;

import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import peer.peerid.PeerID;
import taxonomy.Taxonomy;
import util.logger.Logger;

public class BidirectionalSearch extends CommonCompositionSearch {

	private BackwardCompositionData bCompositionData;

	private BidirectionalForwardComposer forwardComposer;
	private BidirectionalBackwardComposer backwardComposer;

	private final Logger logger = Logger.getLogger(BidirectionalSearch.class);

	public BidirectionalSearch(final ReliableBroadcastPeer peer, final CompositionListener compositionListener) {
		super(peer, compositionListener, GraphType.BIDIRECTIONAL);
	}

	@Override
	public void init() {
		super.init();

		compositionData = new ForwardCompositionData(EXPIRATION_CHECK_TIME, this, gCreator);
		compositionData.start();

		bCompositionData = new BackwardCompositionData(EXPIRATION_CHECK_TIME, this, gCreator);
		bCompositionData.start();

		forwardComposer = new BidirectionalForwardComposer((ForwardCompositionData) compositionData, bCompositionData, this);
		backwardComposer = new BidirectionalBackwardComposer(bCompositionData, (ForwardCompositionData) compositionData, this);
	}

	@Override
	public SearchID startComposition(final Service searchedService) {
		final SearchID searchID = new SearchID(peer.getPeerID());
		logger.debug("Peer " + peer.getPeerID() + " started composition search " + searchID);
		logger.debug("Peer " + peer.getPeerID() + " finding composition for service " + searchedService);
		// the search is added to the search table as waiting
		compositionData.addWaitingSearch(searchID);
		bCompositionData.addWaitingSearch(searchID);
		startComposition(searchedService, MAX_TTL, SEARCH_EXPIRATION, searchID, false);
		return searchID;
	}

	@Override
	protected void startComposition(final Service service, final int maxTTL, final long maxTime, final SearchID searchID, final boolean wasPrepared) {
		logger.trace("Peer " + peer.getPeerID() + " starting composition process: " + searchID + " of service: " + service);
		final Service initService = Utility.createInitService(service, searchID);
		final Service goalService = Utility.createGoalService(service, searchID);

		// save the INIT and goal services with the current searchID
		compositionData.addRunningSearch(searchID, initService, goalService, maxTTL, maxTime, wasPrepared);
		bCompositionData.addRunningSearch(searchID, initService, goalService, maxTTL, maxTime, wasPrepared);

		// Add INIT and GOAL services to current node
		final ServiceList addedServices = new ServiceList();
		addedServices.addService(initService);
		addedServices.addService(goalService);

		logger.trace("Peer " + peer.getPeerID() + " added INIT service " + initService);

		logger.trace("Peer " + peer.getPeerID() + " added GOAL service " + goalService);
		gCreator.manageLocalServices(addedServices, new ServiceList());
	}

	@Override
	public void multicastMessageAccepted(final PeerID source, final BroadcastMessage payload, final int distance, final boolean directBroadcast) {
		if (payload instanceof BCompositionMessage)
			backwardComposer.receivedBComposition((BCompositionMessage) payload);
		else if (payload instanceof FCompositionMessage)
			forwardComposer.receivedFComposition((FCompositionMessage) payload);
		else if (payload instanceof CompositionNotificationMessage)
			getShortestPathNotificator().processShortestPathNotificationMessage((CompositionNotificationMessage) payload);
	}

	public void notifyComposition(final SearchID searchID, final Map<Service, Set<ServiceDistance>> distanceBetweenServices, final PeerID currentPeer, final Taxonomy taxonomy) {
		logger.debug("Peer " + peer.getPeerID() + " found composition for search " + searchID);
		final List<Service> notificationPath = ShortestPathCalculator.findShortestPath(distanceBetweenServices, currentPeer, taxonomy);

		final CompositionNotificationMessage compositionNotificationMessage = new CompositionNotificationMessage(peer.getPeerID(), searchID, distanceBetweenServices, notificationPath);

		getShortestPathNotificator().processShortestPathNotificationMessage(compositionNotificationMessage);
	}

	@Override
	public void newSuccessors(final Map<Service, Set<ServiceDistance>> newSuccessors) {
		forwardComposer.newSuccessors(newSuccessors);
	}

	@Override
	public void newAncestors(final Map<Service, Set<ServiceDistance>> newAncestors) {
		backwardComposer.newAncestors(newAncestors);
	}

	@Override
	public void lostSuccessors(final Map<Service, Set<Service>> lostSuccessors) {
	}

	@Override
	public void messageReceived(final BroadcastMessage message, final long receptionTime) {
	}

	@Override
	public void lostAncestors(final Map<Service, Set<Service>> lostAncestors) {
	}

	@Override
	public void acceptShortestPathNotificationMessage(final ShortestPathNotificationMessage shortestPathNotificationMessage) {
		if (shortestPathNotificationMessage instanceof CompositionNotificationMessage) {
			final CompositionNotificationMessage compositionNotificationMessage = (CompositionNotificationMessage) shortestPathNotificationMessage;
			final SearchID searchID = compositionNotificationMessage.getSearchID();
			notifyComposition(searchID, compositionNotificationMessage.getComposition(), 0, compositionData.getStartingTime(searchID));
		}
	}

	@Override
	public boolean merge(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
		return false;
	}

	@Override
	public void initFComposition(final Service initService, final SearchID searchID) {
	}
}
