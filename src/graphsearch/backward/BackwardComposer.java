package graphsearch.backward;

import graphcreation.GraphCreator;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.backward.backwardCompositionTable.BackwardCompositionData;
import graphsearch.backward.message.BCompositionMessage;
import graphsearch.backward.message.MessageTree;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;
import graphsearch.util.Utility;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import peer.peerid.PeerID;
import util.logger.Logger;

public class BackwardComposer {

	protected final BackwardCompositionData bCompositionData;

	protected final PeerID peerID;

	protected final GraphCreator gCreator;

	protected final CommonCompositionSearch commonCompositionSearch;

	private final Logger logger = Logger.getLogger(BackwardComposer.class);

	public BackwardComposer(final BackwardCompositionData bCompositionData, final CommonCompositionSearch commonCompositionSearch) {
		this.bCompositionData = bCompositionData;
		this.peerID = commonCompositionSearch.getPeer().getPeerID();
		this.gCreator = commonCompositionSearch.getGraphCreator();
		this.commonCompositionSearch = commonCompositionSearch;
	}

	public void lostAncestors(final Map<Service, Set<Service>> lostAncestors) {
		// remove those covering sets which containing the disappeared ancestors
		// for (Service service : lostAncestors.keySet())
		// bCompositionData.removeCoveringSets(service,
		// lostAncestors.get(service));
	}

	public void newAncestors(final Map<Service, Set<ServiceDistance>> newAncestors) {
		for (final Service service : newAncestors.keySet())
			if (gCreator.isLocal(service)) {

				logger.trace("Peer " + peerID + " service " + service + " has new ancestors " + newAncestors.get(service));

				// Obtain the current ancestors set for the service
				final Set<Set<ServiceDistance>> currentCoveringSets = bCompositionData.getCoveringSets(service);

				final Set<ServiceDistance> ancestors = gCreator.getAncestors(service);
				// Calculate the current power set of the service
				final Set<Set<ServiceDistance>> newCoveringSets = CoveringSets.calculateCoveringSets(service, currentCoveringSets, ancestors, gCreator.getPSearch().getDisseminationLayer().getTaxonomy());

				logger.trace("Peer " + peerID + " service " + service + " covering sets: " + currentCoveringSets + " news: " + newCoveringSets);

				// Update the covering sets for the service
				bCompositionData.addCoveringSets(service, newCoveringSets);

				// if the service is the GOAL service start the composition
				if (Utility.isGoalService(service))
					startBComposition(service, newCoveringSets);
				else {
					// Get the current messages and propagate them
					final Map<SearchID, List<BCompositionMessage>> receivedMessages = bCompositionData.getReceivedMessages(service);
					if (!receivedMessages.isEmpty()) {

						logger.trace("Peer " + peerID + " processing messages for searchs " + receivedMessages.keySet());
						for (final List<BCompositionMessage> messages : receivedMessages.values())
							for (final BCompositionMessage message : messages)
								sendBCompositionMessages(service, newCoveringSets, message);
					}
				}
			}
	}

	private void startBComposition(final Service service, final Set<Set<ServiceDistance>> newCoveringSets) {
		final SearchID searchID = bCompositionData.getGoalRelatedSearch(service);
		if (searchID != null) {
			final int maxTTL = bCompositionData.getMaxTTL(searchID);
			final long searchTime = bCompositionData.getRemainingInitTime(searchID);

			logger.trace("Peer " + peerID + " the service is a GOAL service. Sending start messages.");
			final BCompositionMessage message = new BCompositionMessage(searchID, service, new HashSet<ServiceDistance>(), maxTTL, searchTime, peerID);
			sendBCompositionMessages(service, newCoveringSets, message);
		}
	}

	protected void sendBCompositionMessages(final Service service, final Set<Set<ServiceDistance>> coveringSets, final BCompositionMessage receivedCompositionMessage) {
		for (final Set<ServiceDistance> coveringSet : coveringSets) {

			logger.trace("Peer " + peerID + " spliting message " + receivedCompositionMessage + " for covering set: " + coveringSet);
			final Map<Service, BCompositionMessage> messages = receivedCompositionMessage.split(service, coveringSet, peerID, receivedCompositionMessage.getRemainingTime());

			// TODO messages should be multicasted to a group of services to
			// reduce number of sent messages
			for (final Entry<Service, BCompositionMessage> entry : messages.entrySet()) {
				final Service antecessor = entry.getKey();
				final BCompositionMessage message = entry.getValue();
				if (message.getTTL() > 0) {

					logger.trace("Peer " + peerID + " enqueuing message forwarding " + message + " for successors " + Collections.singleton(antecessor));
					final Set<Service> destServices = Utility.getServices(coveringSet);
					gCreator.forwardMessage(message, destServices);
				} else
					logger.trace("Peer " + peerID + " discarded search message due to TTL");
			}
		}
	}

	public void receivedBComposition(final BCompositionMessage bCompositionMessage) {

		logger.trace("Peer " + peerID + " received backward composition search " + bCompositionMessage.getSearchID() + " from service " + bCompositionMessage.getSourceService());

		for (final ServiceDistance sDistance : bCompositionMessage.getDestServices()) {

			logger.trace("Peer " + peerID + " checking service " + sDistance.getService());
			final Service service = sDistance.getService();

			if (gCreator.isLocal(service)) {
				// Updating received messages for current service node
				final MessageTree modifiedTree = bCompositionData.addReceivedMessage(service, bCompositionMessage);

				logger.trace("Peer " + peerID + " added composition message to message table");

				if (Utility.isINITService(service))
					checkCompositionFinished(bCompositionMessage, service, modifiedTree);
				else
					propagateBCompositionMessage(bCompositionMessage, service, modifiedTree);
			}
		}
	}

	protected void propagateBCompositionMessage(final BCompositionMessage bCompositionMessage, final Service service, @SuppressWarnings("unused") final MessageTree modifiedTree) {
		// Get the covering sets for this services
		final Set<Set<ServiceDistance>> coveringSets = bCompositionData.getCoveringSets(service);

		{
			final SearchID searchID = bCompositionMessage.getSearchID();
			logger.trace("Peer " + peerID + " processing messages for active search: " + searchID);
		}
		sendBCompositionMessages(service, coveringSets, bCompositionMessage);
	}

	private void checkCompositionFinished(final BCompositionMessage bCompositionMessage, final Service service, final MessageTree modifiedTree) {

		logger.trace("Peer " + peerID + " INIT service " + service + " received the message");
		// check for message tree completion
		if (modifiedTree.isComplete()) {
			final Set<Service> composition = modifiedTree.getServices();

			logger.trace("Peer " + peerID + " tree is complete. Composition: " + composition);
			final Service goalService = commonCompositionSearch.getGoalService(composition);
			if (Utility.connected(service, goalService))
				notifyComposition(bCompositionMessage.getSearchID(), composition);
		}
	}

	protected void notifyComposition(final SearchID searchID, final Set<Service> services) {
		commonCompositionSearch.notifyComposition(searchID, services);
	}
}
