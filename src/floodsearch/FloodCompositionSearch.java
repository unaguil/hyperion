/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package floodsearch;

import floodsearch.message.FloodCompositionMessage;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.servicegraph.ServiceGraph;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;
import graphsearch.CompositionListener;
import graphsearch.CompositionSearch;
import graphsearch.SearchID;
import graphsearch.compositionData.ExpiredSearch;
import graphsearch.util.Utility;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import peer.CommunicationLayer;
import peer.Peer;
import peer.RegisterCommunicationLayerException;
import peer.conditionregister.ConditionRegister;
import peer.conditionregister.ConditionRegister.EqualityCondition;
import peer.message.BroadcastMessage;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import util.logger.Logger;
import config.Configuration;

public class FloodCompositionSearch implements CommunicationLayer, CompositionSearch, InitCompositionListener {
	
	private final Set<Service> localServices = new HashSet<Service>();
	
	private long MSG_INTERVAL = 3000;
	private short TTL = Short.MAX_VALUE;
	private long SEARCH_EXPIRATION = 10000;
	
	private RunningSearches runningSearches;
	
	protected final Peer peer; 
	
	private static final long CLEAN_RCV_MSGS = 60000;
	private final ConditionRegister<FloodCompositionMessage> receivedFCompositionMessages = new ConditionRegister<FloodCompositionMessage>(CLEAN_RCV_MSGS);
	
	private final Random r = new Random();
	
	private final int JITTER = 10; 
	
	private final Logger logger = Logger.getLogger(FloodCompositionSearch.class);
	
	@Override
	public void initFComposition(final Service initService, final SearchID searchID) {
		logger.debug("Peer " + peer.getPeerID() + " initializing composition " + searchID);
		if (peer.isInitialized()) {
			final FloodCompositionMessage fCompositionMessage = new FloodCompositionMessage(peer.getPeerID(), searchID);
			receivedFCompositionMessages.addEntry(fCompositionMessage);
			fCompositionMessage.addService(initService);
			checkLocalServices(fCompositionMessage);
		}
	} 
	
	public void checkLocalServices(final FloodCompositionMessage fCompositionMessage) {
		logger.debug("Peer " + peer.getPeerID() + " checking local services for composition " + fCompositionMessage.getSearchID());
		final Set<Service> localCoveredServices = getLocalCoveredServices(fCompositionMessage.getComposition());
		for (final Service localCoveredService : localCoveredServices) {
			if (Utility.isGoalService(localCoveredService)) {
				final RunningSearch runningSearch = runningSearches.getRunningSearch(fCompositionMessage.getSearchID());
				if (runningSearch != null && localCoveredService.equals(runningSearch.getGoalService())) {
					fCompositionMessage.addService(localCoveredService);
					notifyComposition(fCompositionMessage.getSearchID(), fCompositionMessage.getComposition(), fCompositionMessage.getHops(), runningSearch.getStartTime());
					runningSearches.stopSearch(fCompositionMessage.getSearchID());
				}
			} else {
				if (!fCompositionMessage.getComposition().contains(localCoveredService)) {
					final FloodCompositionMessage newFCompositionMessage = new FloodCompositionMessage(peer.getPeerID(), fCompositionMessage);
					newFCompositionMessage.addService(localCoveredService);
					sendFCompositionMessage(newFCompositionMessage);
				}
			}
		}
		
		if (localCoveredServices.isEmpty())
			sendFCompositionMessage(fCompositionMessage);
	}
	
	protected void sendFCompositionMessage(final FloodCompositionMessage fCompositionMessage) {
		fCompositionMessage.setHops((short) (fCompositionMessage.getHops() + 1));
		if (fCompositionMessage.getHops() < TTL) {
			try {
				Thread.sleep(r.nextInt(JITTER));
			} catch (InterruptedException e) {}
			peer.directBroadcast(fCompositionMessage);
		}
	}
	
	public void notifyComposition(final SearchID searchID, final Set<Service> services, final int hops, final long startTime) {
		final ExtendedServiceGraph composition = new ExtendedServiceGraph(getTaxonomy());
		for (final Service service : services)
			composition.merge(service);

		logger.debug("Peer " + peer.getPeerID() + " received composition for search " + searchID + " hops: " + hops + " time: " + (System.currentTimeMillis() - startTime));
		compositionListener.compositionFound(composition, searchID, hops);
	}
	
	private Set<Service> getLocalCoveredServices(final Set<Service> receivedComposition) {
		final Set<Service> coveredServices = new HashSet<Service>();
		final Set<Service> allLocalServices = new HashSet<Service>();
		allLocalServices.addAll(localServices);
		
		//add goal services
		allLocalServices.addAll(runningSearches.getGoalServices());
		
		for (final Service localService : allLocalServices) {
			if (isCoveredBy(localService, receivedComposition))
				coveredServices.add(localService);
		}
		logger.debug("Peer " + peer.getPeerID() + " local covered services " + coveredServices);
		return coveredServices;
	}
	
	private boolean isCoveredBy(final Service service, final Set<Service> services) {
		final ServiceGraph serviceGraph = new ServiceGraph(new BasicTaxonomy());
		serviceGraph.merge(services);
		serviceGraph.merge(service);
		return serviceGraph.isCovered(serviceGraph.getServiceNode(service));
	}
	
	private final CompositionListener compositionListener;
	
	public FloodCompositionSearch(final Peer peer, final CompositionListener compositionListener) {
		this.peer = peer;
		this.compositionListener = compositionListener;
		
		try {
			final Set<Class<? extends BroadcastMessage>> messageClasses = new HashSet<Class<? extends BroadcastMessage>>();
			messageClasses.add(FloodCompositionMessage.class);
			peer.addCommunicationLayer(this, messageClasses);
		} catch (final RegisterCommunicationLayerException e) {
			logger.error("Peer " + peer.getPeerID() + " had problem registering communication layer: " + e.getMessage());
		}
	}
	
	static class MyEqualityCondition implements EqualityCondition<FloodCompositionMessage> {
		@Override
		public boolean areEquals(FloodCompositionMessage messageA, FloodCompositionMessage messageB) {
			return messageA.getSearchID().equals(messageB.getSearchID()) && messageA.getVersion().equals(messageB.getVersion());
		}
	}
	
	private final static MyEqualityCondition myEqualityCondition = new MyEqualityCondition();

	@Override
	public void messageReceived(BroadcastMessage message, long receptionTime) {
		final FloodCompositionMessage fCompositionMessage = (FloodCompositionMessage)message;
		logger.debug("Peer " + peer.getPeerID() + " forward composition message received " + fCompositionMessage.getSearchID());
		if (!receivedFCompositionMessages.contains(fCompositionMessage, myEqualityCondition)) {
			receivedFCompositionMessages.addEntry(fCompositionMessage);
			checkLocalServices(fCompositionMessage);
		}
		else
			logger.debug("Peer " + peer.getPeerID() + " discarding message.  Version was already received");
	}

	@Override
	public void init() {
		try {
			final String maxSearchTime = Configuration.getInstance().getProperty("graphsearch.searchExpiration");
			SEARCH_EXPIRATION = Long.parseLong(maxSearchTime);
			logger.info("Peer " + peer.getPeerID() + " SEARCH_EXPIRATION set to: " + SEARCH_EXPIRATION);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading SEARCH_EXPIRATION: " + e.getMessage());
		}
		
		try {
			final String msgInterval = Configuration.getInstance().getProperty("floodsearch.msgInterval");
			MSG_INTERVAL = Long.parseLong(msgInterval);
			logger.info("Peer " + peer.getPeerID() + " MSG_INTERVAL set to: " + MSG_INTERVAL);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading MSG_INTERVAL: " + e.getMessage());
		}
		
		try {
			final String ttl = Configuration.getInstance().getProperty("floodsearch.TTL");
			TTL = Short.parseShort(ttl);
			logger.info("Peer " + peer.getPeerID() + " TTL set to: " + TTL);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading TTL: " + e.getMessage());
		}
		
		runningSearches = new RunningSearches(SEARCH_EXPIRATION, false);
		
		runningSearches.start();
	}

	@Override
	public void stop() {
		runningSearches.stopAndWait();
	}

	@Override
	public boolean merge(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
		return false;
	}

	@Override
	public SearchID startComposition(Service searchedService) {
		final SearchID searchID = new SearchID(peer.getPeerID());
		logger.debug("Peer " + peer.getPeerID() + " started composition search " + searchID);
		logger.debug("Peer " + peer.getPeerID() + " finding composition for service " + searchedService);
		final Service initService = Utility.createInitService(searchedService, searchID);
		final Service goalService = Utility.createGoalService(searchedService, searchID);
		runningSearches.addRunningSearch(searchID, searchedService, initService, goalService, this, MSG_INTERVAL);
		return searchID;
	}

	@Override
	public Service getService(String serviceID) {
		for (final Service localService : localServices) {
			if (localService.getID().equals(serviceID))
				return localService;
		}
		return null;
	}

	@Override
	public Taxonomy getTaxonomy() {
		return new BasicTaxonomy();
	}
	
	@Override
	public void manageLocalServices(final ServiceList addedServices, final ServiceList removedServices) {
		logger.debug("Peer " + peer.getPeerID() + " adding local services " + addedServices);
		localServices.addAll(addedServices.getServiceList());
	}

	@Override
	public void expiredSearches(Set<ExpiredSearch> searches) {
				
	}

	@Override
	public SearchID prepareComposition(Service searchedService) {
		return null;
	}
}
