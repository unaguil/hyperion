package graphsearch.peer;

import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;
import graphsearch.CompositionListener;
import graphsearch.CompositionSearch;
import graphsearch.SearchID;
import graphsearch.backward.BackwardCompositionSearch;
import graphsearch.bidirectionalsearch.BidirectionalSearch;
import graphsearch.forward.ForwardCompositionSearch;
import graphsearch.util.Utility;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.BasicPeer;
import peer.PeerID;
import util.logger.Logger;

public abstract class Peer extends BasicPeer implements CompositionListener {

	private static final String SERVICES_DIR = "services";

	private final CompositionSearch compositionSearch;

	private ServiceList findServices;

	private final Map<SearchID, List<ExtendedServiceGraph>> receivedCompositions = new HashMap<SearchID, List<ExtendedServiceGraph>>();

	private final Logger myLogger = Logger.getLogger(Peer.class);

	private static final String COMPOSE_SERVICE = "composeService";

	private static final String ADD_SERVICE = "addService";
	private static final String REMOVE_SERVICE = "removeService";

	public enum SearchMode {
		FORWARD, BACKWARD, BIDIRECTIONAL
	}

	public Peer(final SearchMode searchMode) {
		switch (searchMode) {
		case BACKWARD:
			compositionSearch = new BackwardCompositionSearch(this, this);
			break;
		case BIDIRECTIONAL:
			compositionSearch = new BidirectionalSearch(this, this);
			break;
		default:
			compositionSearch = new ForwardCompositionSearch(this, this);
			break;
		}
	}

	@Override
	protected boolean peerCommands(final String command, final String[] args) {
		if (command.equals(COMPOSE_SERVICE)) {
			if (args.length == 1) {
				final int index = Integer.parseInt(args[0]);
				final Service searchedService = findServices.getService(index);
				final SearchID searchID = compositionSearch.startComposition(searchedService);
				receivedCompositions.put(searchID, new ArrayList<ExtendedServiceGraph>());
				myLogger.debug("Peer " + getPeerID() + " started composition search " + searchID);
				return true;
			}
			myLogger.error("Peer " + this.getPeerID() + " " + COMPOSE_SERVICE + " must have one argument");
		}
		if (command.equals(ADD_SERVICE)) {
			if (args.length > 0) {
				final ServiceList services = new ServiceList();
				for (final String arg : args) {
					final int index = Integer.parseInt(arg);
					final Service service = findServices.getService(index);
					if (service != null)
						services.addService(service);
					else
						myLogger.error("Peer " + this.getPeerID() + " service with index " + index + " not found.");
				}
				compositionSearch.manageLocalServices(services, new ServiceList());
				return true;
			}
			myLogger.error("Peer " + this.getPeerID() + " " + ADD_SERVICE + " had no arguments");
		} else if (command.equals(REMOVE_SERVICE)) {
			if (args.length > 0) {
				final ServiceList services = new ServiceList();
				for (final String arg : args) {
					final Service service = compositionSearch.getService(arg + ":" + this.getPeerID());
					if (service != null)
						services.addService(service);
					else
						myLogger.error("Peer " + this.getPeerID() + " service with index " + arg + " not found.");
				}
				compositionSearch.manageLocalServices(new ServiceList(), services);
				return true;
			}
			myLogger.error("Peer " + this.getPeerID() + " " + REMOVE_SERVICE + " had no arguments");
		}
		return false;
	}

	@Override
	public void loadData() {
		try {
			findServices = new ServiceList(SERVICES_DIR + File.separator + "Services.xml", getPeerID());

			final String xmlPath = getServicesFilePath(getPeerID());
			final ServiceList sList = new ServiceList(xmlPath, getPeerID());
			compositionSearch.manageLocalServices(sList, new ServiceList());

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private String getServicesFilePath(final PeerID peerID) {
		return SERVICES_DIR + File.separator + "Services" + peerID + ".xml";
	}

	private String getCompositionFilePath(final PeerID peerID) {
		return TEMP_DIR + File.separator + "Graph" + peerID + ".xml";
	}

	@Override
	public void printOutputs() {
		// merge all received compositions and obtain solution
		if (!receivedCompositions.isEmpty()) {
			final List<ExtendedServiceGraph> compositions = receivedCompositions.values().iterator().next();
			if (!compositions.isEmpty()) {
				final ExtendedServiceGraph finalComposition = new ExtendedServiceGraph(compositions.get(0).getTaxonomy());

				for (final ExtendedServiceGraph composition : compositions)
					finalComposition.merge(composition);

				final String xmlPath = getCompositionFilePath(getPeerID());
				try {
					final FileOutputStream f = new FileOutputStream(xmlPath);
					finalComposition.saveToXML(f);
					f.close();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void compositionFound(final ExtendedServiceGraph composition, final SearchID searchID) {
		myLogger.debug("Peer " + getPeerID() + " received composition for search " + searchID);
		receivedCompositions.get(searchID).add(composition);
	}

	@Override
	public void compositionsLost(final SearchID searchID, final ExtendedServiceGraph invalidComposition) {
		myLogger.debug("Peer " + getPeerID() + " received invalid composition for search " + searchID);

		// remove invalid composition from received ones
		for (final Iterator<ExtendedServiceGraph> it = receivedCompositions.get(searchID).iterator(); it.hasNext();) {
			final ExtendedServiceGraph composition = it.next();
			for (final Service service : invalidComposition.getServices())
				composition.removeService(service);

			// check if composition is valid
			boolean valid = false;
			for (final Service service : composition.getServices())
				// check if composition contains a GOAL service
				if (Utility.isGoalService(service))
					valid = true;

			if (!valid)
				it.remove();
		}
	}

	@Override
	public void compositionModified(final SearchID searchID, final Set<Service> removedServices) {
		myLogger.info("Peer " + getPeerID() + " received modification for composition " + searchID + " removed services: " + removedServices);
		for (final ExtendedServiceGraph composition : receivedCompositions.get(searchID))
			for (final Service service : removedServices)
				composition.removeService(service);
	}

	@Override
	public void compositionTimeExpired(final SearchID searchID) {
		myLogger.info("Peer " + getPeerID() + " search " + searchID + " expired. " + receivedCompositions.get(searchID).size() + " compositions received");
	}
}
