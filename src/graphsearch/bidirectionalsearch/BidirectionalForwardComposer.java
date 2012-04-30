package graphsearch.bidirectionalsearch;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.backward.MessageTree;
import graphsearch.backward.backwardCompositionTable.BackwardCompositionData;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;
import graphsearch.forward.ForwardComposer;
import graphsearch.forward.forwardCompositionTable.ForwardCompositionData;
import graphsearch.forward.message.FCompositionMessage;
import graphsearch.util.Utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import util.logger.Logger;

class BidirectionalForwardComposer extends ForwardComposer {

	private final BackwardCompositionData bCompositionData;

	private final Logger logger = Logger.getLogger(BidirectionalForwardComposer.class);

	public BidirectionalForwardComposer(final ForwardCompositionData fCompositionData, final BackwardCompositionData bCompositionData, final CommonCompositionSearch commonCompositionSearch) {
		super(fCompositionData, commonCompositionSearch, commonCompositionSearch.isDirectBroadcast());

		this.bCompositionData = bCompositionData;
	}

	@Override
	protected boolean processForwardCompositionMessages(final Set<ServiceDistance> successors, final Service service, final SearchID searchID) {
		logger.trace("Peer " + peer.getPeerID() + " processing messages for active search: " + searchID + " service: " + service);
		if (fCompositionData.areAllInputsCovered(searchID, service)) {

			logger.trace("Peer " + peer.getPeerID() + " covered all inputs for service " + service);

			final FCompositionMessage mergedForwardMessage = mergeReceivedMessages(service, searchID, peer.getPeerID(), successors, fCompositionData, logger);

			if (!Utility.isGoalService(service)) {
				final Set<Service> exploredSuccessors = checkBackwardMessages(mergedForwardMessage, successors);

				// send the forward composition message if TTL and search
				// remaining time is greater than 0
				if (mergedForwardMessage.getTTL() > 0 && mergedForwardMessage.getRemainingTime() > 0) {
					// Remove those successors which are GOAL services not
					// compatible with the current search
					final Set<Service> services = Utility.getServices(successors);
					final Set<Service> validSuccessors = getValidSuccessors(services, mergedForwardMessage.getComposition());
					// remove those services which were already explored by
					// backward search
					validSuccessors.removeAll(exploredSuccessors);

					forwardCompositionMessage(mergedForwardMessage, validSuccessors);
					return true;
				}
				logger.trace("Peer " + peer.getPeerID() + " discarded search message due to TTL or search expiration");
			}
		}
		logger.trace("Peer " + peer.getPeerID() + " not fully covered service " + service);
		return false;
	}

	private Set<Service> checkBackwardMessages(final FCompositionMessage fCompositionMessage, final Set<ServiceDistance> successors) {

		logger.trace("Peer " + peer.getPeerID() + " checking received backward messages for search " + fCompositionMessage.getSearchID());
		final Set<MessageTree> completeTrees = bCompositionData.getCompleteTrees(fCompositionMessage.getSearchID(), fCompositionMessage.getSourceService());

		final Set<Service> exploredSuccessors = new HashSet<Service>();

		if (!completeTrees.isEmpty()) {

			logger.trace("Peer " + peer.getPeerID() + " found complete trees for search " + fCompositionMessage.getSearchID());

			final Map<Service, Set<ServiceDistance>> distanceBetweenServices = new HashMap<Service, Set<ServiceDistance>>();

			final Set<Service> backwardCompositionDirectSuccessors = new HashSet<Service>();

			for (final MessageTree completeTree : completeTrees) {
				// obtain which successors have been explored by the backward
				// search
				exploredSuccessors.addAll(completeTree.getSuccessors());

				// add backward service distances
				final Map<Service, Set<ServiceDistance>> serviceDistances = completeTree.getAncestorDistances();

				Util.addServiceDistances(distanceBetweenServices, serviceDistances);

				final Set<Service> directSuccessors = getSuccessors(completeTree.getServices(), fCompositionMessage.getSourceService());
				backwardCompositionDirectSuccessors.addAll(directSuccessors);
			}

			// add forward composition removing those services which are not
			// direct successors (according to the backward composition) of the
			// current service
			for (final Service service : fCompositionMessage.getComposition())
				if (backwardCompositionDirectSuccessors.contains(service))
					exploredSuccessors.add(service);

			Util.addServiceDistances(distanceBetweenServices, fCompositionMessage.getSuccessorDistances());

			for (final Iterator<ServiceDistance> it = distanceBetweenServices.get(fCompositionMessage.getSourceService()).iterator(); it.hasNext();) {
				final ServiceDistance sDistance = it.next();
				if (!backwardCompositionDirectSuccessors.contains(sDistance.getService()))
					it.remove();
			}

			// composition is only notified if some successor participates in
			// composition as direct successors of current service
			final Set<Service> directSuccessors = getSuccessors(Util.getAllServices(distanceBetweenServices), fCompositionMessage.getSourceService());

			boolean notify = false;
			for (final ServiceDistance sDistance : successors)
				if (directSuccessors.contains(sDistance.getService())) {
					notify = true;
					break;
				}

			if (notify) {
				final SearchID searchID = fCompositionMessage.getSearchID();
				((BidirectionalSearch) commonCompositionSearch).notifyComposition(searchID, distanceBetweenServices, peer.getPeerID(), gCreator.getPSearch().getDisseminationLayer().getTaxonomy());
			}
		} else
			logger.trace("Peer " + peer.getPeerID() + " complete trees NOT found for search " + fCompositionMessage.getSearchID());

		return exploredSuccessors;
	}

	private Set<Service> getSuccessors(final Set<Service> services, final Service service) {
		final Set<Service> successors = new HashSet<Service>();

		final ExtendedServiceGraph eServiceGraph = new ExtendedServiceGraph(gCreator.getPSearch().getDisseminationLayer().getTaxonomy());

		for (final Service s : services)
			eServiceGraph.merge(s);

		for (final ServiceNode sNode : eServiceGraph.getSuccessors(eServiceGraph.getServiceNode(service), false))
			successors.add(sNode.getService());

		return successors;

	}

	@Override
	protected void notifyComposition(final SearchID searchID, final Set<Service> services, final int hops, final long searchTime) {
	}
}
