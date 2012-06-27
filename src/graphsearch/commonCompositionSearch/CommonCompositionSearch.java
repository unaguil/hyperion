package graphsearch.commonCompositionSearch;

import floodsearch.InitCompositionListener;
import floodsearch.RunningSearches;
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
import graphsearch.compositionData.ExpiredSearch;
import graphsearch.connectionsFilter.ConnectionsFilter;
import graphsearch.shortestpathnotificator.ShortestPathListener;
import graphsearch.shortestpathnotificator.ShortestPathNotificator;
import graphsearch.util.Utility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multicast.MulticastMessageListener;
import peer.CommunicationLayer;
import peer.Peer;
import peer.RegisterCommunicationLayerException;
import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import taxonomy.Taxonomy;
import util.logger.Logger;
import config.Configuration;

public abstract class CommonCompositionSearch implements CommunicationLayer, MulticastMessageListener, GraphCreationListener, CompositionSearch, ShortestPathListener, InitCompositionListener {

	// a reference to the communication peer
	protected final ReliableBroadcastPeer peer;

	// a reference to the graph creation layer
	protected final GraphCreator gCreator;

	// the listener used to notify compositions related events
	private final CompositionListener compositionListener;

	private ShortestPathNotificator shortestPathNotificator;

	// expiration checking time
	protected final long EXPIRATION_CHECK_TIME = 1000;

	// used to store composition data
	protected CompositionData compositionData;
	
	protected Map<Service, SearchID> preparedCompositions = new HashMap<Service, SearchID>();

	// default values
	protected short MAX_TTL = Short.MAX_VALUE;
	protected long SEARCH_EXPIRATION = 10000;
	
	protected long MSG_INTERVAL = 0;
	
	protected boolean DIRECT_BROADCAST = false;
	protected boolean MULTIPLE_PATHS = false; 

	private RunningSearches runningSearches;

	private final Logger logger = Logger.getLogger(CommonCompositionSearch.class);

	public CommonCompositionSearch(final ReliableBroadcastPeer peer, final CompositionListener compositionListener, final GraphType graphType) {
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

	public void notifyComposition(final SearchID searchID, final Set<Service> services, final int hops, final long startingTime) {
		// create the extended service graph using the received services
		// TODO when the composition graph is created there could appear more
		// connections. Indirect paths exists among some services/nodes.
		final ExtendedServiceGraph composition = new ExtendedServiceGraph(getTaxonomy());
		for (final Service service : services)
			composition.merge(service);

		logger.debug("Peer " + peer.getPeerID() + " received composition for search " + searchID + " hops: " + hops + " time: " + (System.currentTimeMillis() - startingTime));
		compositionListener.compositionFound(composition, searchID, hops);
		
		if (MSG_INTERVAL > 0)
			runningSearches.stopSearch(searchID);
	}

	@Override
	public Taxonomy getTaxonomy() {
		return gCreator.getPSearch().getDisseminationLayer().getTaxonomy();
	}

	public void notifyCompositionsLost(final SearchID searchID, final Set<Service> services) {
		final ExtendedServiceGraph invalidComposition = new ExtendedServiceGraph(getTaxonomy());
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
	public void expiredSearches(final Set<ExpiredSearch> expiredSearches) {
		final ServiceList removedServices = new ServiceList();
		for (final ExpiredSearch expiredSearch : expiredSearches) {
			if (!expiredSearch.wasPrepared()) {
				removedServices.addService(expiredSearch.getInitService());
				removedServices.addService(expiredSearch.getGoalService());
				compositionListener.compositionTimeExpired(expiredSearch.getSearchID());
			}
		}
		
		manageLocalServices(new ServiceList(), removedServices);
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
			MAX_TTL = Short.parseShort(maxTTL);
			logger.info("Peer " + peer.getPeerID() + " set MAX_TTL to : " + MAX_TTL);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		try {
			final String maxSearchTime = Configuration.getInstance().getProperty("graphsearch.searchExpiration");
			SEARCH_EXPIRATION = Long.parseLong(maxSearchTime);
			logger.info("Peer " + peer.getPeerID() + " SEARCH_EXPIRATION set to " + SEARCH_EXPIRATION);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}
		
		try {
			final String msgInterval = Configuration.getInstance().getProperty("graphsearch.msgInterval");
			MSG_INTERVAL = Long.parseLong(msgInterval);
			logger.info("Peer " + peer.getPeerID() + " MSG_INTERVAL set to " + MSG_INTERVAL);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}
		
		try {
			final String directBroadcast = Configuration.getInstance().getProperty("graphsearch.directBroadcast");
			DIRECT_BROADCAST = Boolean.parseBoolean(directBroadcast);
			logger.info("Peer " + peer.getPeerID() + " DIRECT_BROADCAST set to " + DIRECT_BROADCAST);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}
		
		try {
			final String multiplePaths = Configuration.getInstance().getProperty("graphsearch.multiplePaths");
			MULTIPLE_PATHS = Boolean.parseBoolean(multiplePaths);
			logger.info("Peer " + peer.getPeerID() + " MULTIPLE_PATHS set to " + MULTIPLE_PATHS);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}
		
		shortestPathNotificator = new ShortestPathNotificator(peer.getPeerID(), gCreator, this, DIRECT_BROADCAST, MULTIPLE_PATHS);
		
		runningSearches = new RunningSearches(SEARCH_EXPIRATION, true);
		runningSearches.start();
	}

	@Override
	public void stop() {
		runningSearches.stopAndWait();
		compositionData.stopAndWait();
	}

	@Override
	public SearchID startComposition(final Service searchedService) {
		boolean wasPrepared = true;
		if (!wasCompositionPrepared(searchedService)) {
			prepareComposition(searchedService);
			wasPrepared = false;
		}
		
		final SearchID searchID = new SearchID(peer.getPeerID());
		logger.debug("Peer " + peer.getPeerID() + " started composition search " + searchID);
		// the search is added to the search table as waiting
		compositionData.addWaitingSearch(searchID);
		startComposition(searchedService, MAX_TTL, SEARCH_EXPIRATION, searchID, wasPrepared);
		return searchID;
	}
	
	protected boolean wasCompositionPrepared(final Service searchedService) {
		return preparedCompositions.containsKey(searchedService);
	}
	
	@Override
	public SearchID prepareComposition(final Service searchedService) {
		if (preparedCompositions.containsKey(searchedService)) {
			logger.trace("Peer " + peer.getPeerID() + " already prepared composition for service " + searchedService);
			return preparedCompositions.get(searchedService);
		}
		
		final SearchID searchID = new SearchID(peer.getPeerID());
		preparedCompositions.put(searchedService, searchID);
		
		logger.trace("Peer " + peer.getPeerID() + " preparing composition for service " + searchedService + " with searchID " + searchID);
		final Service initService = Utility.createInitService(searchedService, searchID);
		final Service goalService = Utility.createGoalService(searchedService, searchID);
		
		// Add INIT and GOAL services to current node
		final ServiceList addedServices = new ServiceList();
		addedServices.addService(initService);
		addedServices.addService(goalService);

		logger.trace("Peer " + peer.getPeerID() + " added INIT service " + initService);

		logger.trace("Peer " + peer.getPeerID() + " added GOAL service " + goalService);
		manageLocalServices(addedServices, new ServiceList());
		return searchID;
	}
	
	protected Service getInitService(final Service preparedComposition) {
		final SearchID searchID = preparedCompositions.get(preparedComposition);
		return Utility.createInitService(preparedComposition, searchID);
	}
	
	protected Service getGoalService(final Service preparedComposition) {
		final SearchID searchID = preparedCompositions.get(preparedComposition);
		return Utility.createInitService(preparedComposition, searchID);
	}
	
	protected void startComposition(final Service service, final int maxTTL, final long maxTime, final SearchID searchID, final boolean wasPrepared) {		
		// save the INIT and goal services with the current searchID
		compositionData.addRunningSearch(searchID, getInitService(service), getGoalService(service), maxTTL, maxTime, wasPrepared);
		runningSearches.addRunningSearch(searchID, getInitService(service), getGoalService(service), service, this, MSG_INTERVAL);
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
	
	@Override
	public void lostSuccessors(final Map<Service, Set<Service>> lostSuccessors) {
		logger.debug("Peer " + peer.getPeerID() + " lost successors " + lostSuccessors);
	}

	@Override
	public void lostAncestors(final Map<Service, Set<Service>> lostAncestors) {
		logger.debug("Peer " + peer.getPeerID() + " lost successors " + lostAncestors);
	}

	public boolean isDirectBroadcast() {
		return DIRECT_BROADCAST;
	}

	public boolean useMultiplePaths() {
		return MULTIPLE_PATHS;
	}
}