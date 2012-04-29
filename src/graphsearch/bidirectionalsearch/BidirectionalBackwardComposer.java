package graphsearch.bidirectionalsearch;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.backward.BackwardComposer;
import graphsearch.backward.MessageTree;
import graphsearch.backward.backwardCompositionTable.BackwardCompositionData;
import graphsearch.backward.message.BCompositionMessage;
import graphsearch.forward.ForwardComposer;
import graphsearch.forward.forwardCompositionTable.ForwardCompositionData;
import graphsearch.forward.message.FCompositionMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import util.logger.Logger;

public class BidirectionalBackwardComposer extends BackwardComposer {

	private final ForwardCompositionData fCompositionData;

	private final Logger logger = Logger.getLogger(BidirectionalBackwardComposer.class);

	public BidirectionalBackwardComposer(final BackwardCompositionData bCompositionData, final ForwardCompositionData fCompositionData, final BidirectionalSearch bidirectionalSearch) {
		super(bCompositionData, bidirectionalSearch);

		this.fCompositionData = fCompositionData;
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

	private Set<Service> checkForwardMessages(final BCompositionMessage bCompositionMessage, final Service service, final MessageTree modifiedTree) {

		logger.trace("Peer " + peerID + " checking forward messages for service " + service + " in search " + bCompositionMessage.getSearchID());

		final Set<Service> exploredAncestors = new HashSet<Service>();

		if (modifiedTree.isComplete()) {
			final SearchID searchID = bCompositionMessage.getSearchID();
			if (fCompositionData.areAllInputsCovered(searchID, service)) {

				logger.trace("Peer " + peerID + " all inputs covered for service " + service + " in search " + bCompositionMessage.getSearchID());
				final Map<Service, Set<ServiceDistance>> distanceBetweenServices = new HashMap<Service, Set<ServiceDistance>>();

				final Set<ServiceDistance> successors = gCreator.getSuccessors(service);

				final Set<Service> directSuccessors = getSuccessors(modifiedTree.getServices(), service);

				// remove non direct successors
				for (final Iterator<ServiceDistance> it = successors.iterator(); it.hasNext();) {
					final ServiceDistance sDistance = it.next();
					if (!directSuccessors.contains(sDistance.getService()))
						it.remove();
				}

				final FCompositionMessage mergedForwardComposition = ForwardComposer.mergeReceivedMessages(service, searchID, peerID, successors, fCompositionData, logger);

				// add forward composition
				exploredAncestors.addAll(mergedForwardComposition.getComposition());
				Util.addServiceDistances(distanceBetweenServices, mergedForwardComposition.getSuccessorDistances());

				// add backward composition
				exploredAncestors.addAll(modifiedTree.getSuccessors());
				Util.addServiceDistances(distanceBetweenServices, modifiedTree.getAncestorDistances());

				((BidirectionalSearch) commonCompositionSearch).notifyComposition(searchID, distanceBetweenServices, peerID, gCreator.getPSearch().getDisseminationLayer().getTaxonomy());
			} else
				logger.trace("Peer " + peerID + " all inputs NOT covered for service " + service + " in search " + bCompositionMessage.getSearchID());
		} else
			logger.trace("Peer " + peerID + " backward message tree not completed for search " + bCompositionMessage.getSearchID());

		return exploredAncestors;
	}

	@Override
	protected void propagateBCompositionMessage(final BCompositionMessage bCompositionMessage, final Service service, final MessageTree modifiedTree) {
		// Get the covering sets for this services
		final Set<Set<ServiceDistance>> coveringSets = bCompositionData.getCoveringSets(service);

		final Set<Service> exploredAncestors = checkForwardMessages(bCompositionMessage, service, modifiedTree);

		// remove those covering sets which are conformed only by explored
		// ancestors
		for (final Iterator<Set<ServiceDistance>> it = coveringSets.iterator(); it.hasNext();) {
			final Set<Service> coveringSet = Util.getAllServices(it.next());
			if (exploredAncestors.containsAll(coveringSet))
				it.remove();
		}

		{
			final SearchID searchID = bCompositionMessage.getSearchID();
			logger.trace("Peer " + peerID + " processing messages for active search: " + searchID);
		}

		sendBCompositionMessages(service, coveringSets, bCompositionMessage);
	}

	@Override
	protected void notifyComposition(final SearchID searchID, final Set<Service> services) {
	}
}
