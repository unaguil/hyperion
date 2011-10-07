package multicast.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.ParameterSearch;
import multicast.ParameterSearchListener;
import multicast.search.message.GeneralizeSearchMessage;
import multicast.search.message.RemoteMessage;
import multicast.search.message.RemoteMulticastMessage;
import multicast.search.message.RemoveParametersMessage;
import multicast.search.message.RemoveRouteMessage;
import multicast.search.message.SearchMessage;
import multicast.search.message.SearchMessage.SearchType;
import multicast.search.message.SearchResponseMessage;
import multicast.search.unicastTable.UnicastTable;

import org.apache.log4j.Logger;

import peer.CommunicationLayer;
import peer.Peer;
import peer.RegisterCommunicationLayerException;
import peer.conditionregister.ConditionRegister;
import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.parameter.Parameter;
import config.Configuration;
import detection.NeighborEventsListener;
import dissemination.DistanceChange;
import dissemination.ParameterDisseminator;
import dissemination.TableChangedListener;
import dissemination.newProtocol.ParameterTableUpdater;

/**
 * This class implements the parameter search layer. It includes methods for
 * parameter search, remote messages sending unicast and multicast.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class ParameterSearchImpl implements CommunicationLayer, NeighborEventsListener, TableChangedListener, ParameterSearch {

	// reference to the lower layer
	private final ParameterDisseminator pDisseminator;

	// the communication peer
	private final Peer peer;

	// listener for parameter search events
	private final ParameterSearchListener searchListener;

	// delegated listener for parameter table changes
	private final TableChangedListener tableChangedListener;

	// A reference to the route table
	private UnicastTable uTable;

	// Configuration properties
	private int MAX_TTL = 5; // Default value

	private final int CLEAN_REC_MSG = 5000;

	private final ConditionRegister<MessageID> receivedMessages = new ConditionRegister<MessageID>(CLEAN_REC_MSG);

	private final Logger logger = Logger.getLogger(ParameterSearchImpl.class);

	/**
	 * Constructor of the parameter search.
	 * 
	 * @param peer
	 *            the communication peer
	 * @param searchListener
	 *            the listener for search events
	 * @param tableChangedListener
	 *            the listener for events related to table changes
	 */
	public ParameterSearchImpl(final Peer peer, final ParameterSearchListener searchListener, final TableChangedListener tableChangedListener) {
		this.peer = peer;
		this.tableChangedListener = tableChangedListener;
		this.pDisseminator = new ParameterTableUpdater(peer, this, this);
		this.searchListener = searchListener;

		final Set<Class<? extends BroadcastMessage>> messageClasses = new HashSet<Class<? extends BroadcastMessage>>();
		messageClasses.add(SearchMessage.class);
		messageClasses.add(RemoteMulticastMessage.class);
		messageClasses.add(SearchResponseMessage.class);
		messageClasses.add(RemoveParametersMessage.class);
		messageClasses.add(RemoveRouteMessage.class);
		messageClasses.add(GeneralizeSearchMessage.class);

		try {
			peer.addCommunicationLayer(this, messageClasses);
		} catch (final RegisterCommunicationLayerException rce) {
			rce.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * multicast.search.PSearch#addLocalParameter(taxonomy.parameter.Parameter)
	 */
	@Override
	public boolean addLocalParameter(final Parameter parameter) {
		return pDisseminator.addLocalParameter(parameter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * multicast.search.PSearch#removeLocalParameter(taxonomy.parameter.Parameter
	 * )
	 */
	@Override
	public boolean removeLocalParameter(final Parameter parameter) {
		return pDisseminator.removeLocalParameter(parameter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multicast.search.PSearch#commit()
	 */
	@Override
	public void commit() {
		pDisseminator.commit();
	}

	@Override
	public void appearedNeighbors(final PeerIDSet neighbors) {
		repropagateSearches(neighbors);
	}

	private void repropagateSearches(final PeerIDSet neighbors) {
		// Active searches are resent to new nodes
		
			logger.trace("Peer " + peer.getPeerID() + " propagating searches for new neighbors " + neighbors);
		for (final SearchMessage searchMessage : uTable.getActiveSearches())
			propagateSearchMessage(searchMessage);
	}

	@Override
	public void dissapearedNeighbors(final PeerIDSet neighbors) {
		final Set<MessageID> lostRoutes = new HashSet<MessageID>();
		for (final PeerID neighbor : neighbors)
			lostRoutes.addAll(uTable.getRoutesThrough(neighbor));

		sendRemoveRouteMessage(lostRoutes);
	}

	@Override
	public PayloadMessage parametersChanged(final PeerID neighbor, final Set<Parameter> addedParameters, final Set<Parameter> removedParameters, final Set<Parameter> removedLocalParameters, final Map<Parameter, DistanceChange> changedParameters,
			final PayloadMessage payload) {
		// Check if the added parameters are local

		final Set<Parameter> localAddedParameters = new HashSet<Parameter>();
		final Set<Parameter> nonLocalAddedParameters = new HashSet<Parameter>();
		for (final Parameter addedParameter : addedParameters)
			// Check if parameter is local
			if (pDisseminator.isLocalParameter(addedParameter))
				localAddedParameters.add(addedParameter);
			else
				nonLocalAddedParameters.add(addedParameter);

		if (!localAddedParameters.isEmpty())
			processLocalAddedParameters(localAddedParameters);

		if (!nonLocalAddedParameters.isEmpty())
			processNonLocalAddedParameters(nonLocalAddedParameters);

		if (!removedLocalParameters.isEmpty())
			processLocalRemovedParameters(removedLocalParameters);

		return tableChangedListener.parametersChanged(neighbor, addedParameters, removedParameters, removedLocalParameters, changedParameters, payload);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multicast.search.PSearch#sendCancelSearchMessage(java.util.Set)
	 */
	@Override
	public void sendCancelSearchMessage(final Set<Parameter> parameters) {
		logger.debug("Peer " + peer.getPeerID() + " canceling searches for parameters " + parameters);

		final Set<MessageID> routeIDs = new HashSet<MessageID>();
		for (final Parameter removedParameter : parameters) {
			final Set<SearchMessage> activeSearches = uTable.getActiveSearches(removedParameter, peer.getPeerID());
			for (final SearchMessage activeSearch : activeSearches)
				routeIDs.add(activeSearch.getRemoteMessageID());
		}

		
			logger.trace("Peer " + peer.getPeerID() + " canceling searches " + routeIDs + " parameters " + parameters);
		sendRemoveParametersMessage(parameters, routeIDs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multicast.search.PSearch#sendGeneralizeSearchMessage(java.util.Set)
	 */
	@Override
	public void sendGeneralizeSearchMessage(final Set<Parameter> generalizedParameters) {
		logger.debug("Peer " + peer.getPeerID() + " enqueuing generalize search for parameters " + generalizedParameters);
		final Set<MessageID> routeIDs = new HashSet<MessageID>();
		for (final Parameter generalizedParameter : generalizedParameters) {
			final Set<SearchMessage> activeSearches = uTable.getSubsumedActiveSearches(generalizedParameter, peer.getPeerID(), pDisseminator.getTaxonomy());
			for (final SearchMessage activeSearch : activeSearches)
				if (activeSearch.getSearchType().equals(SearchType.Generic))
					routeIDs.add(activeSearch.getRemoteMessageID());
		}
		
			logger.trace("Peer " + peer.getPeerID() + " generalizing searches " + routeIDs + " with parameters " + generalizedParameters);
		sendGeneralizeSearchMessage(generalizedParameters, routeIDs);
	}

	private void processLocalRemovedParameters(final Set<Parameter> localRemovedParameters) {
		// Group removal messages by route
		final Set<MessageID> routeIDs = new HashSet<MessageID>();
		for (final Parameter removedParameter : localRemovedParameters)
			routeIDs.addAll(uTable.getLocalParameterRoutes(removedParameter));

		sendRemoveParametersMessage(localRemovedParameters, routeIDs);
	}

	private void processLocalAddedParameters(final Set<Parameter> localAddedParameters) {
		final Map<SearchMessage, Set<Parameter>> parametersTable = new HashMap<SearchMessage, Set<Parameter>>();
		for (final Parameter localParameter : localAddedParameters) {
			// Get active searches searching for this parameter
			final Set<SearchMessage> activeSearches = uTable.getActiveSearches(localParameter);
			for (final SearchMessage activeSearch : activeSearches) {
				if (!parametersTable.containsKey(activeSearch))
					parametersTable.put(activeSearch, new HashSet<Parameter>());
				parametersTable.get(activeSearch).add(localParameter);
			}
		}

		// Notify peers with search response messages
		for (final Entry<SearchMessage, Set<Parameter>> entry : parametersTable.entrySet()) {
			final SearchMessage searchMessage = entry.getKey();
			
				logger.trace("Peer " + peer.getPeerID() + " enqueued search message action" + searchMessage);
			sendSearchResponseMessage(searchMessage.getSource(), entry.getValue(), null, searchMessage.getRemoteMessageID());
		}
	}

	private void processNonLocalAddedParameters(final Set<Parameter> nonLocalAddedParameters) {
		final Set<SearchMessage> propagatedSearches = new HashSet<SearchMessage>();
		// Find all active searches containing the added parameters
		for (final SearchMessage searchMessage : uTable.getActiveSearches()) {
			final Set<Parameter> searchedParameters = new HashSet<Parameter>(searchMessage.getSearchedParameters());
			// Get the intersection
			searchedParameters.retainAll(nonLocalAddedParameters);
			if (!searchedParameters.isEmpty())
				propagatedSearches.add(searchMessage);
		}

		// Propagate all searches
		for (final SearchMessage searchMessage : propagatedSearches)
			propagateSearchMessage(searchMessage);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multicast.search.PSearch#getDisseminationLayer()
	 */
	@Override
	public ParameterDisseminator getDisseminationLayer() {
		return pDisseminator;
	}

	@Override
	public void init() {
		try {
			// Configure internal properties
			final String maxTTL = Configuration.getInstance().getProperty("parameterSearch.searchMessageTTL");
			MAX_TTL = Integer.parseInt(maxTTL);
			logger.info("Peer " + peer.getPeerID() + " set MAX_TTL " + MAX_TTL);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		uTable = new UnicastTable(peer.getPeerID());

		receivedMessages.start();
	}

	@Override
	public void messageReceived(final BroadcastMessage message, final long receptionTime) {
		if (message instanceof SearchMessage) {
			final SearchMessage searchMessage = (SearchMessage) message;

			// check that this message was not previously propagated by this
			// peer
			if (searchMessage.getPreviousSender().equals(peer.getPeerID()))
				return;

			processSearchMessage((SearchMessage) message);

		} else if (message instanceof RemoteMulticastMessage) {
			final RemoteMulticastMessage remoteMulticastMessage = (RemoteMulticastMessage) message;

			// check that message was not previously received
			if (receivedMessages.contains(remoteMulticastMessage.getRemoteMessageID()))
				return;

			receivedMessages.addEntry(remoteMulticastMessage.getRemoteMessageID());

			processMulticastMessage(remoteMulticastMessage);
		} else if (message instanceof RemoveRouteMessage)
			processRemoveRouteMessage((RemoveRouteMessage) message);
		else if (message instanceof RemoveParametersMessage)
			processRemoveParametersMessage((RemoveParametersMessage) message);
		else if (message instanceof GeneralizeSearchMessage)
			processGeneralizeSearchMessage((GeneralizeSearchMessage) message);
	}

	// sends a remote multicast message. This message is routed to multiple
	// remote destinations.
	/*
	 * (non-Javadoc)
	 * 
	 * @see multicast.search.PSearch#sendMulticastMessage(peer.PeerIDSet,
	 * message.BroadcastMessage)
	 */
	@Override
	public void sendMulticastMessage(final PeerIDSet destinations, final PayloadMessage payload) {
		final RemoteMulticastMessage msg = new RemoteMulticastMessage(destinations, payload, peer.getPeerID());
		
			logger.trace("Peer " + peer.getPeerID() + " sending remote multicast message " + msg + " to " + destinations);
		processMulticastMessage(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multicast.search.PSearch#sendUnicastMessage(peer.PeerID,
	 * message.BroadcastMessage)
	 */
	@Override
	public void sendUnicastMessage(final PeerID destination, final PayloadMessage payload) {
		final RemoteMulticastMessage msg = new RemoteMulticastMessage(new PeerIDSet(Collections.singleton(destination)), payload, peer.getPeerID());
		
			logger.trace("Peer " + peer.getPeerID() + " sending remote unicast message " + msg + " to " + destination);
		processMulticastMessage(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see multicast.search.PSearch#knowsSearchRouteTo(peer.PeerID)
	 */
	@Override
	public boolean knowsSearchRouteTo(final PeerID dest) {
		return uTable.knowsSearchRouteTo(dest);
	}

	@Override
	public int getDistanceTo(final PeerID peerID) {
		return uTable.getDistanceTo(peerID);
	}

	@Override
	public void stop() {
		receivedMessages.stopAndWait();
	}

	private void acceptMulticastMessage(final RemoteMulticastMessage multicastMessage) {
		
			logger.trace("Peer " + peer.getPeerID() + " accepted multicast message " + multicastMessage);
		final PayloadMessage payload = multicastMessage.getPayload();
		searchListener.multicastMessageAccepted(multicastMessage.getSource(), payload.copy(), multicastMessage.getDistance());
	}

	// this method is called when a search message is accepted by the current
	// node
	private void acceptSearchMessage(final SearchMessage searchMessage, final Set<Parameter> foundParameters) {
		
			logger.trace("Peer " + peer.getPeerID() + " accepted search message " + searchMessage);
		logger.debug("Peer " + peer.getPeerID() + " accepted " + searchMessage.getType() + " " + searchMessage.getMessageID());

		// Call listener and get user response payload
		final PayloadMessage response = searchListener.searchMessageReceived(searchMessage);

		// Send response to source node including payload
		sendSearchResponseMessage(searchMessage.getSource(), foundParameters, response, searchMessage.getRemoteMessageID());
	}

	private void acceptSearchResponseMessage(final SearchResponseMessage searchResponseMessage) {
		logger.debug("Peer " + peer.getPeerID() + " found parameters " + searchResponseMessage.getParameters() + " in node " + searchResponseMessage.getSource() + " searchID " + searchResponseMessage.getRespondedRouteID());
		searchListener.parametersFound(searchResponseMessage);
	}

	private int getNewDistance(final RemoteMessage remoteMessage) {
		return remoteMessage.getDistance() + 1;
	}

	// process the multicast messages in the current node
	private void processMulticastMessage(final RemoteMulticastMessage multicastMessage) {
		
			logger.trace("Peer " + peer.getPeerID() + " processing multicast message " + multicastMessage);
		// Check if the current peer is valid to pass through the multicast
		// message
		if (!multicastMessage.getThroughPeers().contains(peer.getPeerID()))
			return;

		// Update route table if message contains route information
		if (multicastMessage instanceof SearchResponseMessage)
			uTable.updateUnicastTable((SearchResponseMessage) multicastMessage);

		// Check if message must be accepted
		boolean messageAccepted = false;
		if (multicastMessage.getRemoteDestinations().contains(peer.getPeerID())) {
			messageAccepted = true;

			if (multicastMessage instanceof SearchResponseMessage) {
				acceptSearchResponseMessage((SearchResponseMessage) multicastMessage);
				return;
			}

			// Remove current destination from destination list
			multicastMessage.removeRemoteDestination(peer.getPeerID());
		}

		// Remove invalid destinations. A destination is invalid if cannot be
		// reached from current peer according to route table
		for (final PeerID destination : multicastMessage.getRemoteDestinations())
			if (!uTable.knowsRouteTo(destination))
				multicastMessage.removeRemoteDestination(destination);

		// Check if more destinations are available after purging invalid ones
		if (!multicastMessage.getRemoteDestinations().isEmpty()) {
			// Get the new list of neighbors used to send the message
			final PeerIDSet throughPeers = new PeerIDSet();
			for (final PeerID destination : multicastMessage.getRemoteDestinations()) {
				final PeerID through = uTable.getNeighbor(destination);
				throughPeers.addPeer(through);
			}

			if (multicastMessage instanceof SearchResponseMessage) {
				final SearchResponseMessage newSearchResponseMessage = new SearchResponseMessage((SearchResponseMessage) multicastMessage, peer.getPeerID(), throughPeers.iterator().next(), getNewDistance(multicastMessage));
				
					logger.trace("Peer " + peer.getPeerID() + " multicasting search response " + newSearchResponseMessage);
				peer.enqueueBroadcast(newSearchResponseMessage);
			} else {
				final RemoteMulticastMessage newRemoteMulticastMessage = new RemoteMulticastMessage(multicastMessage, peer.getPeerID(), throughPeers, getNewDistance(multicastMessage));
				
					logger.trace("Peer " + peer.getPeerID() + " multicasting message " + newRemoteMulticastMessage);
				peer.enqueueBroadcast(newRemoteMulticastMessage);
			}
		} else 
			logger.trace("Peer " + peer.getPeerID() + " discarded multicast message " + multicastMessage + ". No more valid destinations.");

		// Finally accept message by current node if needed
		if (messageAccepted)
			acceptMulticastMessage(multicastMessage);
	}

	private void processRemoveParametersMessage(final RemoveParametersMessage removeParametersMessage) {
		
			logger.trace("Peer " + peer.getPeerID() + " processing remove parameters message " + removeParametersMessage);

		final Set<MessageID> removedSearchRoutes = new HashSet<MessageID>();
		final Set<MessageID> removedParameterRoutes = new HashSet<MessageID>();

		final Map<MessageID, Set<Parameter>> lostParameters = new HashMap<MessageID, Set<Parameter>>();
		final Map<MessageID, Set<Parameter>> canceledParameterSearch = new HashMap<MessageID, Set<Parameter>>();
		final Map<MessageID, MessageID> associatedRoutes = new HashMap<MessageID, MessageID>();

		boolean notify = false;
		for (final MessageID routeID : removeParametersMessage.getRouteIDs()) {
			final boolean searchRoute = uTable.isSearchRoute(routeID);
			final boolean existed = uTable.isRoute(routeID);

			final MessageID searchRouteID = uTable.getAssociatedSearchRoute(routeID);
			if (searchRouteID != null)
				associatedRoutes.put(searchRouteID, routeID);

			final Set<Parameter> removedParameters = uTable.removeParameters(removeParametersMessage.getParameters(), routeID);

			// If parameters were removed notify listeners and neighbors
			if (!removedParameters.isEmpty()) {
				if (searchRoute)
					canceledParameterSearch.put(routeID, removedParameters);
				else {
					// notify only parameters which are currently searched
					removedParameters.retainAll(uTable.getSearchedParameters());

					if (!removedParameters.isEmpty())
						lostParameters.put(searchRouteID, removedParameters);
				}

				// Check if route was removed
				if (existed && !uTable.isRoute(routeID))
					if (searchRoute)
						removedSearchRoutes.add(routeID);
					else
						removedParameterRoutes.add(routeID);

				notify = true;
			}
		}

		if (notify) {
			notifyRouteListeners(removeParametersMessage.getSource(), removedSearchRoutes, removedParameterRoutes, lostParameters, canceledParameterSearch, associatedRoutes);

			final RemoveParametersMessage newRemoveParametersMessage = new RemoveParametersMessage(removeParametersMessage, peer.getPeerID(), getNewDistance(removeParametersMessage));
			
				logger.trace("Peer " + peer.getPeerID() + " sending remove parameters message " + newRemoveParametersMessage);
			peer.enqueueBroadcast(newRemoveParametersMessage);
		}
	}

	private void notifyRouteListeners(final PeerID source, final Set<MessageID> removedSearchRoutes, final Set<MessageID> removedParameterRoutes, final Map<MessageID, Set<Parameter>> lostParameters,
			final Map<MessageID, Set<Parameter>> canceledParameterSearch, final Map<MessageID, MessageID> associatedRoutes) {
		if (!lostParameters.isEmpty()) {
			logger.debug("Peer " + peer.getPeerID() + " lost parameters " + lostParameters + " in node " + source);
			
				logger.trace("Peer " + peer.getPeerID() + " removedParameterRoutes " + removedParameterRoutes);
			searchListener.changedParameterRoutes(lostParameters, removedParameterRoutes, associatedRoutes);
		}

		if (!canceledParameterSearch.isEmpty()) {
			
				logger.trace("Peer " + peer.getPeerID() + " canceledParameterSearch " + canceledParameterSearch);
			
				logger.trace("Peer " + peer.getPeerID() + " removedSearchRoutes " + removedSearchRoutes);
			searchListener.changedSearchRoutes(canceledParameterSearch, removedSearchRoutes);
		}
	}

	private void processGeneralizeSearchMessage(final GeneralizeSearchMessage generalizeSearchMessage) {
		
			logger.trace("Peer " + peer.getPeerID() + " processing generalize search message " + generalizeSearchMessage);

		boolean notifyNeighbors = false;

		for (final MessageID routeID : generalizeSearchMessage.getRouteIDs()) {
			final Map<Parameter, Parameter> generalizations = uTable.generalizeSearch(generalizeSearchMessage.getParameters(), routeID, pDisseminator.getTaxonomy());
			// If parameters were affected, propagate message
			if (!generalizations.isEmpty()) {
				notifyNeighbors = true;

				// Send response messages for those parameters not previously
				// responded
				// Check if the peer provides any of the new searched parameters
				final Set<Parameter> foundParameters = new HashSet<Parameter>();
				for (final Parameter p : generalizations.values()) {
					final Set<Parameter> subsumedParameters = pDisseminator.subsumesLocalParameter(p);
					foundParameters.addAll(subsumedParameters);
				}

				// However remove all those parameters which already found with
				// the previous parameter values
				for (final Parameter p : generalizations.keySet()) {
					final Set<Parameter> subsumedParameters = pDisseminator.subsumesLocalParameter(p);
					foundParameters.removeAll(subsumedParameters);
				}

				// Notify new found parameters
				if (!foundParameters.isEmpty()) {
					final SearchMessage searchMessage = uTable.getActiveSearch(routeID);
					acceptSearchMessage(searchMessage, foundParameters);
				}
			}
		}

		if (notifyNeighbors) {
			final GeneralizeSearchMessage newGeneralizeSearchMessage = new GeneralizeSearchMessage(generalizeSearchMessage, peer.getPeerID(), getNewDistance(generalizeSearchMessage));
			
				logger.trace("Peer " + peer.getPeerID() + " sending generalize search message " + newGeneralizeSearchMessage);
			peer.enqueueBroadcast(newGeneralizeSearchMessage);
			
				logger.trace("Peer " + peer.getPeerID() + " sent generalize search message " + newGeneralizeSearchMessage);
		}
	}

	// Processes a remove route message
	private void processRemoveRouteMessage(final RemoveRouteMessage removeRouteMessage) {
		
			logger.trace("Peer " + peer.getPeerID() + " processing remove route message " + removeRouteMessage);

		final Map<MessageID, Set<Parameter>> lostParameters = new HashMap<MessageID, Set<Parameter>>();
		final Map<MessageID, Set<Parameter>> canceledParameterSearch = new HashMap<MessageID, Set<Parameter>>();

		final Set<MessageID> removedParameterRoutes = new HashSet<MessageID>();
		final Set<MessageID> removedSearchRoutes = new HashSet<MessageID>();

		final Map<MessageID, MessageID> associatedRoutes = new HashMap<MessageID, MessageID>();

		boolean notify = false;
		for (final MessageID routeID : removeRouteMessage.getLostRoutes()) {
			final boolean searchRoute = uTable.isSearchRoute(routeID);

			final MessageID searchRouteID = uTable.getAssociatedSearchRoute(routeID);
			if (searchRouteID != null)
				associatedRoutes.put(searchRouteID, routeID);

			if (searchRoute) {
				final Set<Parameter> canceledParameters = uTable.getSearchedParameters(routeID);
				final PeerID lostDestination = uTable.removeRoute(routeID, removeRouteMessage.getSender());
				if (!lostDestination.equals(PeerID.VOID_PEERID)) {
					canceledParameterSearch.put(routeID, canceledParameters);
					removedSearchRoutes.add(routeID);
					notify = true;
				}

			} else {
				final Set<Parameter> removedParameters = uTable.getParameters(routeID);
				final PeerID lostDestination = uTable.removeRoute(routeID, removeRouteMessage.getSender());
				if (!lostDestination.equals(PeerID.VOID_PEERID)) {
					removedParameters.retainAll(uTable.getSearchedParameters());

					// get the search route associated with this route
					if (!removedParameters.isEmpty())
						lostParameters.put(routeID, removedParameters);
					removedParameterRoutes.add(searchRouteID);
					notify = true;
				}
			}
		}

		// Propagate current active searches
		repropagateSearches(peer.getDetector().getCurrentNeighbors());

		if (notify) {
			notifyRouteListeners(removeRouteMessage.getSource(), removedSearchRoutes, removedParameterRoutes, lostParameters, canceledParameterSearch, associatedRoutes);

			final RemoveRouteMessage newRemoveRouteMessage = new RemoveRouteMessage(removeRouteMessage, peer.getPeerID(), getNewDistance(removeRouteMessage));
			
				logger.trace("Peer " + peer.getPeerID() + " sending remove route message " + newRemoveRouteMessage);
			peer.enqueueBroadcast(newRemoveRouteMessage);
		}
	}

	private void processSearchMessage(final SearchMessage searchMessage) {
		
			logger.trace("Peer " + peer.getPeerID() + " processing search message " + searchMessage);

		// Update route table with the information contained in the message
		if (uTable.updateUnicastTable(searchMessage)) {
			// If the current peer provides any of the searched parameters
			// accept the message
			final Set<Parameter> foundParameters = new HashSet<Parameter>();
			for (final Parameter p : searchMessage.getSearchedParameters())
				if (searchMessage.getSearchType().equals(SearchType.Exact)) {
					if (pDisseminator.isLocalParameter(p))
						foundParameters.add(p);
				} else if (searchMessage.getSearchType().equals(SearchType.Generic)) {
					final Set<Parameter> subsumedParameters = pDisseminator.subsumesLocalParameter(p);
					foundParameters.addAll(subsumedParameters);
				}

			if (!foundParameters.isEmpty())
				acceptSearchMessage(searchMessage, foundParameters);

			propagateSearchMessage(searchMessage);
		}
	}

	// Sends a message which searches for specified parameters
	/*
	 * (non-Javadoc)
	 * 
	 * @see multicast.search.PSearch#sendSearchMessage(java.util.Set,
	 * message.BroadcastMessage,
	 * multicast.search.message.SearchMessage.SearchType)
	 */
	@Override
	public void sendSearchMessage(final Set<Parameter> parameters, final PayloadMessage payload, final SearchType searchType) {
		final SearchMessage msg = new SearchMessage(parameters, payload, peer.getPeerID(), MAX_TTL, 0, searchType);
		logger.debug("Peer " + peer.getPeerID() + " started search for parameters " + parameters + " searchID " + msg.getRemoteMessageID());
		
			logger.trace("Peer " + peer.getPeerID() + " searching parameters with message " + msg);
		processSearchMessage(msg);
	}

	// Sends a message which generalizes the specified parameters
	private void sendRemoveParametersMessage(final Set<Parameter> parameters, final Set<MessageID> routeIDs) {
		final RemoveParametersMessage removeParametersMessage = new RemoveParametersMessage(parameters, routeIDs, peer.getPeerID());
		processRemoveParametersMessage(removeParametersMessage);
	}

	private void sendGeneralizeSearchMessage(final Set<Parameter> parameters, final Set<MessageID> routeIDs) {
		final GeneralizeSearchMessage generalizeSearchMessage = new GeneralizeSearchMessage(parameters, routeIDs, peer.getPeerID());
		processGeneralizeSearchMessage(generalizeSearchMessage);
	}

	// sends a search response message to a the specified destination,
	// indicating that the following parameters have been found
	private void sendSearchResponseMessage(final PeerID destination, final Set<Parameter> parameters, final PayloadMessage payload, final MessageID respondedRouteID) {
		final SearchResponseMessage searchResponseMessage = new SearchResponseMessage(destination, parameters, payload, peer.getPeerID(), respondedRouteID);
		
			logger.trace("Peer " + peer.getPeerID() + " sending search response message " + searchResponseMessage);
		logger.debug("Peer " + peer.getPeerID() + " sending response message to search " + searchResponseMessage.getRespondedRouteID());
		processMulticastMessage(searchResponseMessage);
	}

	// sends a remove route message
	private void sendRemoveRouteMessage(final Set<MessageID> lostRoutes) {
		final RemoveRouteMessage removeRouteMessage = new RemoveRouteMessage(peer.getPeerID(), lostRoutes);
		processRemoveRouteMessage(removeRouteMessage);
	}

	// Called when search is a propagated search
	private void propagateSearchMessage(final SearchMessage searchMessage) {
		
			logger.trace("Peer " + peer.getPeerID() + " propagating search " + searchMessage);
		// Create a copy of the message for broadcasting using the current node
		// as the new sender. This message responds to the received one
		final SearchMessage newMsg = new SearchMessage(searchMessage, peer.getPeerID(), getNewDistance(searchMessage));
		for (final Parameter p : searchMessage.getSearchedParameters()) {
			final int tableDistance = pDisseminator.getEstimatedDistance(p);
			final int previousDistance = searchMessage.getPreviousDistance(p);

			if (previousDistance <= tableDistance && !(previousDistance == 0 && tableDistance == 0)) {
				// The message is getting closer to a parameter. TTL is restored
				newMsg.restoreTTL(p, MAX_TTL);
				
					logger.trace("Peer " + peer.getPeerID() + " restored message " + newMsg + " TTL. Parameter " + p + " going from " + previousDistance + " to " + tableDistance);
			} else // The message is getting farer (or there is no information)
			// from the parameter and the message was not received from the
			// current node
			if (!searchMessage.getSource().equals(peer.getPeerID())) {
				newMsg.decTTL(p);
				
					logger.trace("Peer " + peer.getPeerID() + " decremented TTL of message " + newMsg + " to " + newMsg.getTTL(p) + ". Parameter " + p + " going from " + previousDistance + " to " + tableDistance);
			}

			// Set the message parameter distance as known by the current node
			// table.
			newMsg.setCurrentDistance(p, tableDistance);
		}

		// If the message TTL is greater than zero message is sent, else it is
		// thrown
		if (newMsg.hasTTL()) {
			peer.enqueueBroadcast(newMsg);
			
				logger.trace("Peer " + peer.getPeerID() + " enqueued search message " + newMsg);
			return;
		}

		
			logger.trace("Peer " + peer.getPeerID() + " has thrown message " + newMsg + " due TTL");
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		uTable.saveToXML(os);
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
	}
}
