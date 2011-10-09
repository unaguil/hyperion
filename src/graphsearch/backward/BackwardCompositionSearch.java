package graphsearch.backward;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.CompositionListener;
import graphsearch.backward.backwardCompositionTable.BackwardCompositionData;
import graphsearch.backward.message.BCompositionMessage;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;

import java.util.Map;
import java.util.Set;

import peer.Peer;
import peer.message.BroadcastMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class BackwardCompositionSearch extends CommonCompositionSearch {

	private BackwardComposer backwardComposer;

	public BackwardCompositionSearch(final Peer peer, final CompositionListener compositionListener) {
		super(peer, compositionListener);
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
		backwardComposer.lostAncestors(lostAncestors);
	}

	@Override
	public void newAncestors(final Map<Service, Set<ServiceDistance>> newAncestors) {
		backwardComposer.newAncestors(newAncestors);
	}

	@Override
	public void multicastMessageAccepted(final PeerID source, final PayloadMessage payload, final int distance) {
		if (payload instanceof BCompositionMessage)
			backwardComposer.receivedBComposition((BCompositionMessage) payload);
	}

	@Override
	public void messageReceived(final BroadcastMessage message, final long receptionTime) {
	}

	@Override
	public void acceptShortestPathNotificationMessage(final ShortestPathNotificationMessage shortestPathNotificationMessage) {
	}
}
