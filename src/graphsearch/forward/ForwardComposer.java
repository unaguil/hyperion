package graphsearch.forward;

import graphcreation.GraphCreator;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.collisionbased.sdg.SDG;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;
import graphsearch.forward.forwardCompositionTable.ForwardCompositionData;
import graphsearch.forward.message.CompositionModificationMessage;
import graphsearch.forward.message.FCompositionMessage;
import graphsearch.forward.message.InvalidCompositionsMessage;
import graphsearch.shortestpathnotificator.ShortestPathCalculator;
import graphsearch.util.Utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import util.logger.Logger;

import peer.Peer;
import peer.peerid.PeerID;

public class ForwardComposer {

	protected final ForwardCompositionData fCompositionData;

	protected final CommonCompositionSearch commonCompositionSearch;

	protected final GraphCreator gCreator;

	protected final Peer peer;

	private final Logger logger = Logger.getLogger(ForwardComposer.class);

	public ForwardComposer(final ForwardCompositionData fCompositionData, final CommonCompositionSearch commonCompositionSearch) {
		this.fCompositionData = fCompositionData;
		this.commonCompositionSearch = commonCompositionSearch;
		this.gCreator = commonCompositionSearch.getGraphCreator();
		this.peer = commonCompositionSearch.getPeer();
	}

	public void newSuccessors(final Map<Service, Set<ServiceDistance>> newSuccessors) {
		logger.debug("Peer " + peer.getPeerID() + " detected new successors " + newSuccessors);	
		for (final Entry<Service, Set<ServiceDistance>> entry : newSuccessors.entrySet()) {
			final Service service = entry.getKey();
			// Only local services are taken into account
			if (gCreator.getSDG().isLocal(service)) {
				
					logger.trace("Peer " + peer.getPeerID() + " service " + service + " has new successors " + entry.getValue());
				if (Utility.isINITService(service)) {
					// the INIT node is always covered. Send the start
					// composition message to new successors
					
						logger.trace("Peer " + peer.getPeerID() + " the service is an INIT service. Sending start messages.");
					initFComposition(entry.getValue(), service);
				} else {
					// the service is a not an INIT service -> check if the
					// service is covered and forward its composition message
					final Map<SearchID, List<FCompositionMessage>> receivedMessages = fCompositionData.getReceivedMessages(service);
					for (final SearchID searchID : receivedMessages.keySet())
						processForwardCompositionMessages(entry.getValue(), service, searchID);
				}
			}
		}
	}

	private void initFComposition(final Set<ServiceDistance> successors, final Service startService) {
		final SearchID searchID = fCompositionData.getInitRelatedSearch(startService);
		if (searchID != null) {
			final int maxTTL = fCompositionData.getMaxTTL(searchID);
			final long searchTime = fCompositionData.getRemainingInitTime(searchID);

			final FCompositionMessage fCompositionMessage = new FCompositionMessage(searchID, startService, successors, maxTTL, searchTime);
			final Set<Service> services = Utility.getServices(successors);
			forwardCompositionMessage(fCompositionMessage, services);
		}
	}

	public void forwardCompositionMessage(final FCompositionMessage fCompositionMessage, final Set<Service> services) {
		if (!services.isEmpty()) {
			logger.debug("Peer " + peer.getPeerID() + " forward composition search " + fCompositionMessage.getSearchID() + " for successors " + services + " from service " + fCompositionMessage.getSourceService());
			gCreator.forwardMessage(fCompositionMessage, services);
		}
	}

	public static int getTTL(final List<FCompositionMessage> messages) {
		final Collection<Integer> values = new ArrayList<Integer>();
		for (final FCompositionMessage fCompositionMessage : messages)
			values.add(Integer.valueOf(fCompositionMessage.getTTL()));
		return Collections.max(values).intValue();
	}

	protected void processForwardCompositionMessages(final Set<ServiceDistance> successors, final Service service, final SearchID searchID) {
		
			logger.trace("Peer " + peer.getPeerID() + " processing messages for active search: " + searchID + " service: " + service);
		if (fCompositionData.areAllInputsCovered(searchID, service)) {
			
				logger.trace("Peer " + peer.getPeerID() + " covered all inputs for service " + service);

			final FCompositionMessage mergedForwardMessage = mergeReceivedMessages(successors, service, searchID, peer.getPeerID(), fCompositionData, logger);

			if (Utility.isGoalService(service)) {
				
					logger.trace("Peer " + peer.getPeerID() + " reached GOAL service " + service + " for search: " + searchID + " composition : " + mergedForwardMessage.getComposition());
				notifyComposition(mergedForwardMessage.getSearchID(), mergedForwardMessage.getComposition());
			} else // send the forward composition message if TTL and search
			// remaining time is greater than 0
			if (mergedForwardMessage.getTTL() > 0 && mergedForwardMessage.getRemainingTime() > 0) {
				// Remove those successors which are GOAL services not
				// compatible with the current search
				final Set<Service> services = Utility.getServices(successors);
				final Set<Service> validSuccessors = getValidSuccessors(services, mergedForwardMessage.getComposition());
				forwardCompositionMessage(mergedForwardMessage, validSuccessors);
			} else 
				logger.trace("Peer " + peer.getPeerID() + " discarded search message due to TTL or search expiration");
		} else 
			logger.trace("Peer " + peer.getPeerID() + " not fully covered service " + service);
	}

	public static FCompositionMessage mergeReceivedMessages(final Set<ServiceDistance> successors, final Service service, final SearchID searchID, final PeerID peerID, final ForwardCompositionData fCompositionData, final Logger logger) {
		final List<FCompositionMessage> receivedMessages = fCompositionData.getReceivedMessages(service).get(searchID);
		final FCompositionMessage fCompositionMessage = new FCompositionMessage(searchID, service, successors, getTTL(receivedMessages) - 1, fCompositionData.getRemainingTime(searchID));
		// Merge all received compositions
		
			logger.trace("Peer " + peerID + " merging all received compositions: " + receivedMessages.size() + " for service " + service + " in search: " + searchID);
		for (final FCompositionMessage receivedMessage : receivedMessages)
			fCompositionMessage.join(receivedMessage);
		return fCompositionMessage;
	}

	protected void notifyComposition(final SearchID searchID, final Set<Service> services) {
		commonCompositionSearch.notifyComposition(searchID, services);
	}

	protected Set<Service> getValidSuccessors(final Set<Service> successors, final Set<Service> composition) {
		final Set<Service> validSuccessors = new HashSet<Service>();
		final Service initService = commonCompositionSearch.getInitService(composition);
		for (final Service successor : successors)
			if ((Utility.isGoalService(successor) && Utility.connected(initService, successor)) || !Utility.isGoalService(successor))
				validSuccessors.add(successor);
		return validSuccessors;
	}

	public void receivedFComposition(final FCompositionMessage fCompositionMessage) {
		logger.debug("Peer " + peer.getPeerID() + " received forward composition search " + fCompositionMessage.getSearchID() + " from service " + fCompositionMessage.getSourceService() + " to services " + fCompositionMessage.getDestServices());

		for (final ServiceDistance sDistance : fCompositionMessage.getDestServices()) {
			final Service service = sDistance.getService();
			
				logger.trace("Peer " + peer.getPeerID() + " checking service " + service);
			final SDG sdg = gCreator.getSDG();
			if (sdg.isLocal(service)) {
				
					logger.trace("Peer " + peer.getPeerID() + " added composition message to message table");

				// Updating received messages for current service node
				fCompositionData.addReceivedMessage(service, fCompositionMessage);

				// Propagate messages
				final SearchID searchID = fCompositionMessage.getSearchID();
				processForwardCompositionMessages(gCreator.getSDG().getSuccessors(service), service, searchID);
			}
		}
	}

	public void processDisappearedAncestors(final Map<Service, Set<Service>> lostAncestors, final Map<SearchID, Set<Service>> invalidCompositions) {
		final Map<SearchID, Service> invalidLocalServices = new HashMap<SearchID, Service>();

		final Map<SearchID, Set<Service>> removedServices = new HashMap<SearchID, Set<Service>>();
		final Map<SearchID, FCompositionMessage> compositionMessages = new HashMap<SearchID, FCompositionMessage>();

		// remove those messages received from the disappeared ancestors
		for (final Entry<Service, Set<Service>> entry : lostAncestors.entrySet()) {
			final Service localService = entry.getKey();
			final Set<SearchID> affectedSearches = new HashSet<SearchID>();
			for (final Service ancestor : entry.getValue())
				affectedSearches.addAll(fCompositionData.removeMessagesReceivedFrom(localService, ancestor));

			// obtain how compositions are modified
			for (final SearchID searchID : affectedSearches)
				if (!fCompositionData.areAllInputsCovered(searchID, localService))
					invalidLocalServices.put(searchID, localService);
				else {
					if (!removedServices.containsKey(searchID))
						removedServices.put(searchID, new HashSet<Service>());
					removedServices.get(searchID).addAll(lostAncestors.get(localService));

					final FCompositionMessage mergedForwardMessage = mergeReceivedMessages(gCreator.getSDG().getSuccessors(localService), localService, searchID, peer.getPeerID(), fCompositionData, logger);
					compositionMessages.put(searchID, mergedForwardMessage);
				}
		}

		if (!invalidLocalServices.isEmpty()) {
			final InvalidCompositionsMessage invalidCompositionsMessage = new InvalidCompositionsMessage(peer.getPeerID());
			for (final Entry<SearchID, Service> entry : invalidLocalServices.entrySet()) {
				final SearchID searchID = entry.getKey();
				final Service invalidLocalService = entry.getValue();

				Set<Service> invalidComposition;
				if (invalidCompositions.containsKey(searchID))
					invalidComposition = invalidCompositions.get(searchID);
				else
					invalidComposition = new HashSet<Service>();

				invalidComposition.add(invalidLocalService);

				// check if invalid service is a GOAL service
				if (Utility.isGoalService(invalidLocalService))
					commonCompositionSearch.notifyCompositionsLost(searchID, invalidComposition);
				else {
					invalidCompositionsMessage.addInvalidLocalService(searchID, invalidLocalService, gCreator.getSDG().getSuccessors(invalidLocalService));
					invalidCompositionsMessage.addInvalidComposition(searchID, invalidComposition);
				}
			}

			forwardInvalidCompositionsMessage(invalidCompositionsMessage, invalidCompositionsMessage.getSuccessors());
		}

		if (!removedServices.isEmpty())
			// notify composition modification
			for (final Entry<SearchID, Set<Service>> entry : removedServices.entrySet()) {
				final SearchID searchID = entry.getKey();
				notifyCompositionModification(searchID, entry.getValue(), compositionMessages.get(searchID));
			}
	}

	private void notifyCompositionModification(final SearchID searchID, final Set<Service> removedServices, final FCompositionMessage fCompositionMessage) {
		
			logger.trace("Peer " + peer.getPeerID() + " notifying composition modification " + searchID);
		final Map<Service, Set<ServiceDistance>> distanceBetweenServices = fCompositionMessage.getSuccessorDistances();
		final List<Service> notificationPath = ShortestPathCalculator.findShortestPath(distanceBetweenServices, peer.getPeerID(), gCreator.getPSearch().getDisseminationLayer().getTaxonomy());

		final CompositionModificationMessage compositionModificationMessage = new CompositionModificationMessage(peer.getPeerID(), searchID, removedServices, distanceBetweenServices, notificationPath);

		// process the notification message
		commonCompositionSearch.getShortestPathNotificator().processShortestPathNotificationMessage(compositionModificationMessage);
	}

	public void lostAncestors(final Map<Service, Set<Service>> lostAncestors) {
		
			logger.trace("Peer " + peer.getPeerID() + " lost ancestors " + lostAncestors);

		processDisappearedAncestors(lostAncestors, new HashMap<SearchID, Set<Service>>());
	}

	private void forwardInvalidCompositionsMessage(final InvalidCompositionsMessage compositionModificationMessage, final Set<Service> services) {
		
			logger.trace("Peer " + peer.getPeerID() + " forwarding invalid composition message " + compositionModificationMessage + " for successors " + services);
		gCreator.forwardMessage(compositionModificationMessage, services);
	}

	public void receivedInvalidComposition(final InvalidCompositionsMessage invalidCompositionMessage) {
		final Map<Service, Set<Service>> lostAncestors = invalidCompositionMessage.getLostAncestors();
		processDisappearedAncestors(lostAncestors, invalidCompositionMessage.getInvalidCompositions());
	}

	public void newAncestors(Map<Service, Set<ServiceDistance>> newAncestors) {
		logger.debug("Peer " + peer.getPeerID() + " detected new ancestors " + newAncestors);		
	}
}
