package graphsearch.forward;

import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.CompositionListener;
import graphsearch.bidirectionalsearch.message.ShortestPathNotificationMessage;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;
import graphsearch.forward.forwardCompositionTable.ForwardCompositionData;
import graphsearch.forward.message.CompositionModificationMessage;
import graphsearch.forward.message.FCompositionMessage;
import graphsearch.forward.message.InvalidCompositionsMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.Peer;
import peer.message.BroadcastMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;

public class ForwardCompositionSearch extends CommonCompositionSearch {

	private ForwardComposer forwardComposer;

	public ForwardCompositionSearch(final Peer peer, final CompositionListener compositionListener) {
		super(peer, compositionListener, GraphType.FORWARD);
	}

	@Override
	public void init() {
		super.init();

		compositionData = new ForwardCompositionData(EXPIRATION_CHECK_TIME, this, gCreator);
		compositionData.start();

		forwardComposer = new ForwardComposer((ForwardCompositionData) compositionData, this);
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
	public void multicastMessageAccepted(final PeerID source, final PayloadMessage payload, final int distance) {
		if (payload instanceof FCompositionMessage)
			forwardComposer.receivedFComposition((FCompositionMessage) payload);
		if (payload instanceof InvalidCompositionsMessage)
			forwardComposer.receivedInvalidComposition((InvalidCompositionsMessage) payload);
		if (payload instanceof CompositionModificationMessage)
			getShortestPathNotificator().processShortestPathNotificationMessage((CompositionModificationMessage) payload);
	}

	@Override
	public void lostSuccessors(final Map<Service, Set<Service>> lostSuccessors) {
	}

	@Override
	public void lostAncestors(final Map<Service, Set<Service>> lostAncestors) {
		//forwardComposer.lostAncestors(lostAncestors);
	}

	@Override
	public void acceptShortestPathNotificationMessage(final ShortestPathNotificationMessage shortestPathNotificationMessage) {
		if (shortestPathNotificationMessage instanceof CompositionModificationMessage) {
			final CompositionModificationMessage compositionModificationMessage = (CompositionModificationMessage) shortestPathNotificationMessage;
			notifyCompositionModified(compositionModificationMessage.getSearchID(), compositionModificationMessage.getRemovedServices());
		}
	}

	@Override
	public boolean checkWaitingMessages(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
		return true;		
	}
}
