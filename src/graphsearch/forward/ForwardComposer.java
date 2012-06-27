package graphsearch.forward;

import graphcreation.GraphCreator;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.commonCompositionSearch.CommonCompositionSearch;
import graphsearch.forward.forwardCompositionTable.ForwardCompositionData;
import graphsearch.forward.message.FCompositionMessage;
import graphsearch.util.Utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import peer.Peer;
import peer.peerid.PeerID;
import util.logger.Logger;

public class ForwardComposer {

	protected final ForwardCompositionData fCompositionData;

	protected final CommonCompositionSearch commonCompositionSearch;

	protected final GraphCreator gCreator;

	protected final Peer peer;
	
	private final boolean directBroadcast;
	
	private final boolean multiplePaths;

	private final Logger logger = Logger.getLogger(ForwardComposer.class);

	public ForwardComposer(final ForwardCompositionData fCompositionData, final CommonCompositionSearch commonCompositionSearch, final boolean directBroadcast, final boolean multiplePaths) {
		this.fCompositionData = fCompositionData;
		this.commonCompositionSearch = commonCompositionSearch;
		this.gCreator = commonCompositionSearch.getGraphCreator();
		this.peer = commonCompositionSearch.getPeer();
		this.directBroadcast = directBroadcast;
		this.multiplePaths = multiplePaths;
	}

	public void newSuccessors(final Map<Service, Set<ServiceDistance>> newSuccessors) {
		logger.debug("Peer " + peer.getPeerID() + " detected new successors " + newSuccessors);
		for (final Entry<Service, Set<ServiceDistance>> entry : newSuccessors.entrySet()) {
			final Service service = entry.getKey();
			final Set<ServiceDistance> successors = entry.getValue();
			logger.trace("Peer " + peer.getPeerID() + " service " + service + " has new successors " + entry.getValue());
			if (Utility.isINITService(service)) {
				// the INIT node is always covered. Send the start
				// composition message to new successors
				logger.trace("Peer " + peer.getPeerID() + " the service is an INIT service. Sending start messages.");
				newInitSuccessors(successors, service, fCompositionData.getInitRelatedSearch(service));
			} else {
				// the service is a not an INIT service -> check if the
				// service is covered and forward its composition message
				final Map<SearchID, Set<FCompositionMessage>> receivedMessages = fCompositionData.getReceivedMessages(service);
				for (final SearchID searchID : receivedMessages.keySet()) {
					if (!receivedMessages.get(searchID).isEmpty()) {
						final Set<ServiceDistance> notForwardedSuccessors = filterForwardedSuccessors(successors, searchID);
						processForwardCompositionMessages(notForwardedSuccessors, service, searchID);
					}
				}
			}
		}
	}

	public void initServiceAlreadyAdded(final Service initService, final SearchID searchID) {
		newInitSuccessors(gCreator.getSuccessors(initService), initService, searchID);
	}

	private void addAllForwardedSuccessors(final SearchID searchID, final Set<ServiceDistance> successors) {
		for (final ServiceDistance successor : successors)
			fCompositionData.addForwardedSuccessor(searchID, successor.getService());
	}

	private Set<ServiceDistance> notForwardedSuccessors(final SearchID searchID, final Set<ServiceDistance> successors) {
		final Set<ServiceDistance> notForwardedSuccessors = new HashSet<ServiceDistance>();
		for (final ServiceDistance successor : successors) {
			if (!fCompositionData.wasAlreadyForwarded(searchID, successor.getService()))
				notForwardedSuccessors.add(successor);
		}
		return notForwardedSuccessors;
	}

	private Set<ServiceDistance> filterForwardedSuccessors(final Set<ServiceDistance> successors, final SearchID searchID) {
		if (!directBroadcast || multiplePaths) {
			final Set<ServiceDistance> notForwardedSuccessors = notForwardedSuccessors(searchID, successors);
			addAllForwardedSuccessors(searchID, notForwardedSuccessors);
			return notForwardedSuccessors;
		}
		
		return successors;
	}

	private void newInitSuccessors(final Set<ServiceDistance> successors, final Service startService, final SearchID searchID) {
		if (searchID != null) {
			logger.debug("Peer " + peer.getPeerID() + " init service of " + searchID + " has new successors " + successors);
			final int maxTTL = fCompositionData.getMaxTTL(searchID);
			final long searchTime = fCompositionData.getRemainingInitTime(searchID);

			final Set<ServiceDistance> notForwardedSuccessors = filterForwardedSuccessors(successors, searchID);
			final FCompositionMessage fCompositionMessage = new FCompositionMessage(searchID, startService, notForwardedSuccessors, maxTTL, searchTime);
			final Set<Service> services = Utility.getServices(notForwardedSuccessors);
			forwardCompositionMessage(fCompositionMessage, services);
		}
	}

	private void initFComposition(final Set<ServiceDistance> successors, final Service startService, final SearchID searchID) {
		logger.debug("Peer " + peer.getPeerID() + " initializing " + searchID + " init with successors " + successors);
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
			gCreator.forwardMessage(fCompositionMessage, services, commonCompositionSearch.isDirectBroadcast(), commonCompositionSearch.useMultiplePaths());
		}
	}

	public static int getTTL(final Collection<FCompositionMessage> messages) {
		final Collection<Integer> values = new ArrayList<Integer>();
		for (final FCompositionMessage fCompositionMessage : messages)
			values.add(Integer.valueOf(fCompositionMessage.getTTL()));
		return Collections.max(values).intValue();
	}

	protected boolean processForwardCompositionMessages(final Set<ServiceDistance> successors, final Service service, final SearchID searchID) {
		logger.trace("Peer " + peer.getPeerID() + " processing messages for active search: " + searchID + " service: " + service);
		if (fCompositionData.areAllInputsCovered(searchID, service)) {
			logger.trace("Peer " + peer.getPeerID() + " covered all inputs for service " + service);
			forwardMergedComposition(successors, service, searchID);
			return true;
		}
		logger.trace("Peer " + peer.getPeerID() + " not fully covered service " + service);
		return false;
	}

	private void forwardMergedComposition(final Set<ServiceDistance> successors, final Service service, final SearchID searchID) {
		final FCompositionMessage mergedForwardMessage = mergeReceivedMessages(service, searchID, peer.getPeerID(), successors, fCompositionData, logger);
		if (Utility.isGoalService(service)) {
			logger.trace("Peer " + peer.getPeerID() + " reached GOAL service " + service + " for search: " + searchID + " composition : " + mergedForwardMessage.getComposition());
			notifyComposition(mergedForwardMessage.getSearchID(), mergedForwardMessage.getComposition(), mergedForwardMessage.getHops(), fCompositionData.getStartingTime(searchID));
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
	}

	public static FCompositionMessage mergeReceivedMessages(final Service service, final SearchID searchID, final PeerID peerID, final Set<ServiceDistance> successors, final ForwardCompositionData fCompositionData, final Logger logger) {
		final Set<FCompositionMessage> receivedMessages = fCompositionData.getReceivedMessages(service).get(searchID);
		final FCompositionMessage fCompositionMessage = new FCompositionMessage(searchID, service, successors, getTTL(receivedMessages) - 1, fCompositionData.getRemainingTime(searchID));
		// Merge all received compositions

		logger.trace("Peer " + peerID + " merging all received compositions: " + receivedMessages.size() + " for service " + service + " in search: " + searchID);
		for (final FCompositionMessage receivedMessage : receivedMessages)
			fCompositionMessage.join(receivedMessage);
		return fCompositionMessage;
	}

	protected void notifyComposition(final SearchID searchID, final Set<Service> services, final int hops, final long searchTime) {
		commonCompositionSearch.notifyComposition(searchID, services, hops, searchTime);
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
			if (gCreator.containsLocalService(service)) {
				logger.trace("Peer " + peer.getPeerID() + " added composition message to message table");

				// Updating received messages for current service node				
				if (fCompositionData.addReceivedMessage(service, fCompositionMessage)) {
					// Propagate messages
					final SearchID searchID = fCompositionMessage.getSearchID();
					final Set<ServiceDistance> successors = gCreator.getSuccessors(service);
					processForwardCompositionMessages(successors, service, searchID);
				}
			}
		}
	}

	public void lostAncestors(final Map<Service, Set<Service>> lostAncestors) {
		logger.trace("Peer " + peer.getPeerID() + " lost ancestors " + lostAncestors);
	}

	public void newAncestors(final Map<Service, Set<ServiceDistance>> newAncestors) {
		logger.debug("Peer " + peer.getPeerID() + " detected new ancestors " + newAncestors);
	}

	public void initFComposition(final Service initService, final SearchID searchID) {
		initFComposition(gCreator.getSuccessors(initService), initService, searchID);
	}
}
