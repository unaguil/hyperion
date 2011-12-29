package graphsearch.commonCompositionSearch;

import graphcreation.GraphCreationListener;
import graphcreation.GraphCreator;
import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.CollisionGraphCreator;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;
import graphsearch.CompositionListener;
import graphsearch.CompositionSearch;
import graphsearch.SearchID;
import graphsearch.compositionData.CompositionData;
import graphsearch.compositionData.localSearchesTable.LocalSearchesTable.SearchStatus;
import graphsearch.compositionData.localSearchesTable.SearchExpiredListener;
import graphsearch.connectionsFilter.ConnectionsFilter;
import graphsearch.shortestpathnotificator.ShortestPathListener;
import graphsearch.shortestpathnotificator.ShortestPathNotificator;
import graphsearch.util.Utility;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multicast.MulticastMessageListener;
import peer.CommunicationLayer;
import peer.Peer;
import peer.RegisterCommunicationLayerException;
import peer.message.BroadcastMessage;
import util.logger.Logger;
import config.Configuration;

public abstract class CommonCompositionSearch implements CommunicationLayer, SearchExpiredListener, MulticastMessageListener, GraphCreationListener, CompositionSearch, ShortestPathListener {

	// a reference to the communication peer
	protected final Peer peer;

	// a reference to the graph creation layer
	protected final GraphCreator gCreator;

	// the listener used to notify compositions related events
	private final CompositionListener compositionListener;

	private ShortestPathNotificator shortestPathNotificator;

	// expiration checking time
	protected final long EXPIRATION_CHECK_TIME = 1000;

	// used to store composition data
	protected CompositionData compositionData;

	// default values
	protected int MAX_TTL = 5;
	protected long SEARCH_EXPIRATION = 10000;

	private final Logger logger = Logger.getLogger(CommonCompositionSearch.class);

	public CommonCompositionSearch(final Peer peer, final CompositionListener compositionListener, final GraphType graphType) {
		this.peer = peer;
		this.gCreator = new CollisionGraphCreator(peer, this, this, graphType);
		this.compositionListener = compositionListener;

		try {
			peer.addCommunicationLayer(this, new HashSet<Class<? extends BroadcastMessage>>());
		} catch (final RegisterCommunicationLayerException e) {
			logger.error("Peer " + peer.getPeerID() + " had problem registering communication layer: " + e.getMessage());
		}
	}

	public void disableMulticastLayer() {
		((CollisionGraphCreator)gCreator).disableMulticastLayer();
	}
	
	public void disableGraphCreationLayer() {
		((CollisionGraphCreator)gCreator).setDisabled();
	}

	@Override
	public void filterConnections(final Map<Service, Set<ServiceDistance>> foundRemoteSuccessors, final Map<Service, Set<ServiceDistance>> foundRemoteAncestors) {
		ConnectionsFilter.filter(foundRemoteSuccessors, foundRemoteAncestors);
	}

	public void notifyComposition(final SearchID searchID, final Set<Service> services, final int hops) {
		// create the extended service graph using the received services
		// TODO when the composition graph is created there could appear more
		// connections. Indirect paths exists among some services/nodes.
		final ExtendedServiceGraph composition = new ExtendedServiceGraph(gCreator.getPSearch().getDisseminationLayer().getTaxonomy());
		for (final Service service : services)
			composition.merge(service);

		logger.debug("Peer " + peer.getPeerID() + " received composition for search " + searchID);
		compositionListener.compositionFound(composition, searchID, hops);
	}

	public void notifyCompositionsLost(final SearchID searchID, final Set<Service> services) {
		final ExtendedServiceGraph invalidComposition = new ExtendedServiceGraph(gCreator.getPSearch().getDisseminationLayer().getTaxonomy());
		for (final Service service : services)
			invalidComposition.merge(service);

		logger.debug("Peer " + peer.getPeerID() + " received invalid composition for search " + searchID);
		compositionListener.compositionsLost(searchID, invalidComposition);
	}

	public void notifyCompositionModified(final SearchID searchID, final Set<Service> removedServices) {
		logger.debug("Peer " + peer.getPeerID() + " received modification for composition " + searchID + " removed services: " + removedServices);
		compositionListener.compositionModified(searchID, removedServices);
	}

	public Service getGoalService(final Set<Service> composition) {
		Service goalService = null;
		for (final Service service : composition)
			if (Utility.isGoalService(service)) {
				goalService = service;
				break;
			}
		return goalService;
	}

	// the received composition must contain an INIT service whose ID and source
	// peer are equal to those of the GOAL service
	public Service getInitService(final Set<Service> composition) {
		Service initService = null;
		for (final Service service : composition)
			if (Utility.isINITService(service)) {
				initService = service;
				break;
			}
		return initService;
	}

	@Override
	public void expiredSearches(final Set<SearchID> searches) {
		for (final SearchID searchID : searches)
			compositionListener.compositionTimeExpired(searchID);
	}

	@Override
	public Service getService(final String serviceID) {
		return gCreator.getService(serviceID);
	}

	@Override
	public void manageLocalServices(final ServiceList addedServices, final ServiceList removedServices) {
		gCreator.manageLocalServices(addedServices, removedServices);
	}

	@Override
	public void init() {
		try {
			// Configure internal properties
			final String maxTTL = Configuration.getInstance().getProperty("graphsearch.maxTTL");
			MAX_TTL = Integer.parseInt(maxTTL);
			logger.info("Peer " + peer.getPeerID() + " set MAX_TTL to : " + MAX_TTL);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		try {
			final String maxSearchTime = Configuration.getInstance().getProperty("graphsearch.searchExpiration");
			SEARCH_EXPIRATION = Long.parseLong(maxSearchTime);
			logger.info("Peer " + peer.getPeerID() + " search expiration time: " + SEARCH_EXPIRATION);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		shortestPathNotificator = new ShortestPathNotificator(peer.getPeerID(), gCreator, this);
	}

	@Override
	public void stop() {
		compositionData.stopAndWait();
	}

	@Override
	public SearchID startComposition(final Service searchedService) {
		final SearchID searchID = new SearchID(peer.getPeerID());
		logger.debug("Peer " + peer.getPeerID() + " started composition search " + searchID);
		logger.debug("Peer " + peer.getPeerID() + " finding composition for service " + searchedService);
		// the search is added to the search table as waiting
		compositionData.addWaitingSearch(searchID);
		startComposition(searchedService, MAX_TTL, SEARCH_EXPIRATION, searchID);
		return searchID;
	}

	@Override
	public boolean isRunningSearch(final SearchID searchID) {
		return compositionData.getSearchStatus(searchID).equals(SearchStatus.RUNNING);
	}

	protected void startComposition(final Service service, final int maxTTL, final long maxTime, final SearchID searchID) {
		logger.trace("Peer " + peer.getPeerID() + " starting composition process: " + searchID + " of service: " + service);
		final Service initService = Utility.createInitService(service, peer.getPeerID());
		final Service goalService = Utility.createGoalService(service, peer.getPeerID());

		// save the INIT and goal services with the current searchID
		compositionData.addRunningSearch(searchID, initService, goalService, maxTTL, maxTime);

		// Add INIT and GOAL services to current node
		final ServiceList addedServices = new ServiceList();
		addedServices.addService(initService);
		addedServices.addService(goalService);

		logger.trace("Peer " + peer.getPeerID() + " added INIT service " + initService);

		logger.trace("Peer " + peer.getPeerID() + " added GOAL service " + goalService);
		gCreator.manageLocalServices(addedServices, new ServiceList());
	}

	public GraphCreator getGraphCreator() {
		return gCreator;
	}

	public Peer getPeer() {
		return peer;
	}

	public ShortestPathNotificator getShortestPathNotificator() {
		return shortestPathNotificator;
	}
}
