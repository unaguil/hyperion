package multicast.search.unicastTable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import multicast.SearchedParameter;
import multicast.Util;
import multicast.search.Route;
import multicast.search.message.SearchMessage;
import multicast.search.message.SearchMessage.SearchType;
import multicast.search.message.SearchResponseMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.peerid.PeerID;
import serialization.xml.XMLSerializable;
import taxonomy.Taxonomy;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;
import util.logger.Logger;
import detection.NeighborDetector;

/**
 * This class contains the information about routes. It maintains a table of
 * destinations and the next hop to reach them.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class UnicastTable implements XMLSerializable {
		
	// Strings used for XML serialization
	private final static String SEARCH_TAG = "search";
	private final static String SEARCH_PEER_ATTRIB = "peer";
	private final static String PARAMETER_TAG = "parameter";
	private final static String PARAMETER_VALUE_ATTRIB = "value";
	
	private final static String ROUTE_DEST_ATTRIB = "dest";
	private final static String ROUTE_TAG = "route";
	private final static String ROUTE_NEIGHBOR_ATTRIB = "through";

	private final static String UNICAST_TABLE_TAG = "unicastTable";

	// table which contains normal routes (non-search).
	private final List<BroadcastRoute> routes = new ArrayList<BroadcastRoute>();

	// the list contains the active searches for the current node. It is related
	// with a search route
	private final List<SearchMessage> activeSearches = new ArrayList<SearchMessage>();
	
	//TODO remove foundParameters table
	private final Map<PeerID, Set<Parameter>> foundParameters = new HashMap<PeerID, Set<Parameter>>();

	// the identification of the peer which hosts the unicast table
	private final PeerID peerID;
	
	private final NeighborDetector nDetector;
	
	private final MessageID defaultRouteID;

	private final Logger logger = Logger.getLogger(UnicastTable.class);

	/**
	 * Constructor of the unicast table
	 * 
	 * @param peerID
	 *            the peer which hosts the unicast table
	 */
	public UnicastTable(final PeerID peerID, final NeighborDetector nDetector) {
		this.peerID = peerID;
		this.nDetector = nDetector;
		this.defaultRouteID = new MessageID(peerID, MessageIDGenerator.getNewID());
	}
	
	private Map<PeerID, Set<Parameter>> createActiveSearchesMap(final List<SearchMessage> activeSearchList) {
		final Map<PeerID, Set<Parameter>> activeSearchesMap = new HashMap<PeerID, Set<Parameter>>();
		for (final SearchMessage activeSearch : activeSearchList) {
			if (!activeSearchesMap.containsKey(activeSearch.getSource()))
				activeSearchesMap.put(activeSearch.getSource(), new HashSet<Parameter>());
			activeSearchesMap.get(activeSearch.getSource()).addAll(activeSearch.getSearchedParameters());
		}
		return activeSearchesMap;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof UnicastTable))
			return false;

		final UnicastTable unicastTable = (UnicastTable) o;

		if (this.routes.containsAll(unicastTable.routes)) {
			final Map<PeerID, Set<Parameter>> thisMap = createActiveSearchesMap(this.activeSearches);
			final Map<PeerID, Set<Parameter>> otherMap = createActiveSearchesMap(unicastTable.activeSearches);
			
			return mapEquality(thisMap, otherMap);
		}
		return false;
	}

	private boolean mapEquality(Map<PeerID, Set<Parameter>> thisMap, Map<PeerID, Set<Parameter>> otherMap) {
		if (thisMap.isEmpty() && otherMap.isEmpty())
			return true;
		
		if (thisMap.size() != otherMap.size())
			return false;
		for (final PeerID key : thisMap.keySet()) {
			if (!otherMap.containsKey(key))
				return false;
			
			if (!thisMap.get(key).equals(otherMap.get(key)))
				return false;
		}
		
		return true;
	}

	/**
	 * Generalizes the parameters of the specified route
	 * 
	 * @param generalizations
	 *            the parameters to generalize
	 * @param routeID
	 *            the route to generalize
	 * @param taxonomy
	 *            the taxonomy used to check the generalizations
	 * @return the map of performed generalizations
	 */
	public Map<Parameter, Parameter> generalizeSearch(final Set<Parameter> generalizations, final MessageID routeID, final Taxonomy taxonomy) {
		if (isSearchRoute(routeID)) {
			final SearchMessage searchMessage = getSearch(routeID);
			if (searchMessage != null && searchMessage.getSearchType().equals(SearchType.Generic)) {
				final SearchMessage activeSearch = getSearch(searchMessage.getRemoteMessageID());
				return activeSearch.generalizeParameters(generalizations, taxonomy);
			}
		}
		return new HashMap<Parameter, Parameter>();
	}
	
	public SearchMessage getSearch(final MessageID routeID) {
		for (final SearchMessage searchMessage : getSearches())
			if (searchMessage.getRemoteMessageID().equals(routeID))
				return searchMessage;
		return null;
	}

	public List<SearchMessage> getActiveSearches() {
		return Collections.unmodifiableList(activeSearches);
	}

	public Set<SearchMessage> getSearches(final Parameter parameter, final Taxonomy taxonomy) {
		final Set<SearchMessage> searchMessages = new HashSet<SearchMessage>();
		for (final SearchMessage searchMessage : getSearches()) {
			for (final Parameter searchedParameter : searchMessage.getSearchedParameters()) {
				if (parameter.equals(searchedParameter) || taxonomy.subsumes(searchedParameter.getID(), parameter.getID()))
					searchMessages.add(searchMessage);
			}
		}
		return searchMessages;
	}

	/**
	 * Gets the set of search messages which were searching for the passed
	 * parameter and are originated in the specified peer
	 * 
	 * @param the
	 *            searched parameter
	 * @param the
	 *            peer which originated the search message
	 * @return the set of search messages
	 */
	public Set<SearchMessage> getSearches(final Parameter parameter, final PeerID peer) {
		final Set<SearchMessage> searchMessages = new HashSet<SearchMessage>();
		for (final SearchMessage searchMessage : getSearches())
			if (searchMessage.getSearchedParameters().contains(parameter) && searchMessage.getSource().equals(peer))
				searchMessages.add(searchMessage);
		return searchMessages;
	}
	
	public Route getRoute(final PeerID dest) {
		if (dest.equals(peerID))
			return new BroadcastRoute(dest, dest, defaultRouteID, 0);
		
		if (nDetector.getCurrentNeighbors().contains(dest))
			return new BroadcastRoute(dest, dest, defaultRouteID, 1);
		
		List<Route> availableRoutes = new ArrayList<Route>();
		// find the more recent route to destination
		for (final Route route : getAllRoutes())
			if (route.getDest().equals(dest))
				availableRoutes.add(route);
		
		if (availableRoutes.isEmpty())
			return null;
		
		return Util.getShortestRoute(availableRoutes); 
	}

	/**
	 * Gets the identification of the routes going to the passed destination
	 * 
	 * @param dest
	 *            the destination whose route identifications are obtained
	 * @return the route identifications of the passed destination
	 */
	public Set<MessageID> getRouteIDs(final PeerID dest) {
		final Set<MessageID> routeIDs = new HashSet<MessageID>();
		for (final BroadcastRoute route : getAllRoutes())
			if (route.getDest().equals(dest))
				routeIDs.add(route.getRouteID());

		return routeIDs;
	}

	/**
	 * Gets the identification of those routes passing through the specified
	 * neighbor
	 * 
	 * @param neighbor
	 *            the neighbor whose routes are obtained
	 * @return the set of routes passing through the specified neighbor
	 */
	public Set<MessageID> getRoutesThrough(final PeerID neighbor) {
		final Set<MessageID> routeIDs = new HashSet<MessageID>();
		for (final BroadcastRoute route : getAllRoutes())
			if (route.getThrough().equals(neighbor))
				routeIDs.add(route.getRouteID());

		return routeIDs;
	}

	/**
	 * Gets the set of parameters which are being searched by the current node
	 * 
	 * @return the parameters which are being searched by the current node
	 */
	public Set<Parameter> getSearchedParameters() {
		final Set<Parameter> searchedParameters = new HashSet<Parameter>();
		for (final SearchMessage activeSearch : getSearches())
			if (activeSearch.getSource().equals(peerID))
				searchedParameters.addAll(activeSearch.getSearchedParameters());

		return searchedParameters;
	}

	public Set<Parameter> getSearchedParameters(final MessageID routeID) {
		final SearchMessage activeSearch = getSearch(routeID);
		return activeSearch.getSearchedParameters();
	}

	public Set<SearchMessage> getSubsumedSearches(final Parameter parameter, final PeerID peer, final Taxonomy taxonomy) {
		final Set<SearchMessage> searchMessages = new HashSet<SearchMessage>();
		for (final SearchMessage searchMessage : getSearches())
			if (searchMessage.getSource().equals(peer))
				for (final Parameter p : searchMessage.getSearchedParameters())
					// Check for subsumtion excluding equality
					if (!parameter.equals(p) && taxonomy.subsumes(parameter.getID(), p.getID()))
						searchMessages.add(searchMessage);
		return searchMessages;
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + routes.hashCode();
		result = 37 * result + activeSearches.hashCode();
		return result;
	}
	
	public boolean isRoute(final MessageID routeID) {
		return routes.contains(new BroadcastRoute(PeerID.VOID_PEERID, PeerID.VOID_PEERID, routeID, 0));
	}

	/**
	 * Checks if the table knows a route to passed node
	 * 
	 * @param dest
	 *            the node to check if there is a known route to reached it
	 * @return true if the table contains a route to the node, false otherwise
	 */
	public boolean knowsRouteTo(final PeerID dest) {
		if (dest.equals(peerID))
			return true;
		
		if (nDetector.getCurrentNeighbors().contains(dest))
			return true;
		
		for (final Route route : getAllRoutes())
			if (route.getDest().equals(dest))
				return true;

		return false;
	}
	
	@Override
	public void readFromXML(final InputStream is) throws IOException {
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		Document document = null;
		try {
			document = docBuilder.parse(is);
		} catch (final SAXException e) {
			throw new IOException(e);
		}
		
		final NodeList searchList = document.getElementsByTagName(SEARCH_TAG);
		for (int i = 0; i < searchList.getLength(); i++) {
			final Element searchElement = (Element) searchList.item(i);
			final String peer = searchElement.getAttribute(SEARCH_PEER_ATTRIB);
			
			final NodeList parameterList = searchElement.getElementsByTagName(PARAMETER_TAG);
			
			final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
			for (int j = 0; j < parameterList.getLength(); j++) {
				final Element parameterElement = (Element) parameterList.item(j);
				final String parameter = parameterElement.getAttribute(PARAMETER_VALUE_ATTRIB);
				try {
					searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter(parameter), 0));
				} catch (final InvalidParameterIDException ipe) {
					throw new IOException(ipe);
				}
			} 

			SearchMessage activeSearch = new SearchMessage(new PeerID(peer), Collections.<PeerID> emptySet(), searchedParameters, null, 0, SearchType.Exact);
			activeSearches.add(activeSearch);
		}

		final NodeList routeList = document.getElementsByTagName(ROUTE_TAG);
		for (int i = 0; i < routeList.getLength(); i++) {
			final Element e = (Element) routeList.item(i);
			final String dest = e.getAttribute(ROUTE_DEST_ATTRIB);
			final String through = e.getAttribute(ROUTE_NEIGHBOR_ATTRIB);
			addRoute(new MessageID(new PeerID(dest), Integer.parseInt(through)), new PeerID(dest), new PeerID(through), 0);
		}
	}

	public Set<Parameter> removeParameters(final Set<Parameter> parameters, final MessageID routeID) {	
		final SearchMessage search = getSearch(routeID);
		
		if (isSearchRoute(routeID)) {
			final Set<Parameter> removedParameters = search.removeParameters(parameters);
			if (search.getSearchedParameters().isEmpty())
				removeSearch(routeID);
			
			logUTable();
			
			return removedParameters;
		}
		
		return Collections.emptySet();
	}

	public PeerID removeRoute(final MessageID routeID) {
		for (final Iterator<BroadcastRoute> it = routes.iterator(); it.hasNext();) {
			final BroadcastRoute route = it.next();
			if (route.getRouteID().equals(routeID)) {
				it.remove();
				final PeerID dest = route.getDest();
				logger.trace("Peer " + peerID + " removed route to " + dest);
				if (!knowsRouteTo(dest))
					foundParameters.remove(dest);
				return dest;
			}
		}
		return PeerID.VOID_PEERID;
	}
	
	public Map<PeerID, Set<Parameter>> getAlreadyFoundParameters() {
		return Collections.unmodifiableMap(foundParameters);
	}
	
	public boolean cancelSearch(final MessageID routeID, final PeerID neighbor) {
		SearchMessage activeSearch = getSearch(routeID);
		if (activeSearch.getSender().equals(neighbor) || neighbor.equals(peerID))
			return removeSearch(routeID);
		return false;
	}

	private void logUTable() {
		logger.trace("Peer " + peerID + " utable " + this);
	}
	
	public List<SearchMessage> getSearches() {
		return Collections.unmodifiableList(activeSearches);
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		final Element root = doc.createElement(UNICAST_TABLE_TAG);
		doc.appendChild(root);
		for (final SearchMessage knownSearches : getSearches()) {
			final Element searchElement = doc.createElement(SEARCH_TAG);
			searchElement.setAttribute(SEARCH_PEER_ATTRIB, knownSearches.getSource().toString());
			for (final Parameter p : knownSearches.getSearchedParameters()) {
				final Element parameterElement = doc.createElement(PARAMETER_TAG);
				parameterElement.setAttribute(PARAMETER_VALUE_ATTRIB, p.toString());
				searchElement.appendChild(parameterElement);
			}
			root.appendChild(searchElement);
		}

		for (final Route route : routes) {
			final Element routeElement = doc.createElement(ROUTE_TAG);
			routeElement.setAttribute(ROUTE_DEST_ATTRIB, route.getDest().toString());
			routeElement.setAttribute(ROUTE_NEIGHBOR_ATTRIB, route.getThrough().toString());
			root.appendChild(routeElement);
		}

		Transformer transformer = null;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (final TransformerConfigurationException tce) {
			throw new IOException(tce);
		}

		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(os);
		try {
			transformer.transform(source, result);
		} catch (final TransformerException te) {
			throw new IOException(te);
		}
	}

	@Override
	public String toString() {
		final StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("[");
		for (final SearchMessage activeSearch : getSearches())
			strBuilder.append(activeSearch.getSource() + ":" + activeSearch.getSearchedParameters() + " ");
		
		for (final Route route : routes)
			strBuilder.append("(" + route.getDest() + " -> " + route.getThrough() + ") ");
		
		strBuilder.append("]");

		return strBuilder.toString();
	}
	
	public boolean updateUnicastTable(final SearchMessage searchMessage) {
		// Check if the received search message was not previously received (it
		// is contained in the active searches)
		if (activeSearches.contains(searchMessage))
			return false;
		
		// Add the search to the current active ones
		activeSearches.add(searchMessage);
		updateTable(searchMessage);
		
		logUTable();
		return true;
	}

	private void updateTable(final SearchMessage searchMessage) {
		if (!searchMessage.getSource().equals(peerID))
			addRoute(searchMessage.getRemoteMessageID(), searchMessage.getSource(), searchMessage.getSender(), searchMessage.getDistance());
	}

	/**
	 * Updates the route table using the information provided by the response
	 * message.
	 * 
	 * @param searchResponseMessage
	 *            the search response message used to update the unicast table.
	 */
	public void updateUnicastTable(final SearchResponseMessage searchResponseMessage) {
		if (!searchResponseMessage.getSource().equals(peerID)) {
			addRoute(searchResponseMessage.getRemoteMessageID(), searchResponseMessage.getSource(), searchResponseMessage.getSender(), searchResponseMessage.getDistance());
			if (!foundParameters.containsKey(searchResponseMessage.getSource()))
				foundParameters.put(searchResponseMessage.getSource(), new HashSet<Parameter>());
			foundParameters.get(searchResponseMessage.getSource()).addAll(searchResponseMessage.getParameters());
		}
		
		logUTable();
	}

	private void addRoute(final MessageID routeID, final PeerID dest, final PeerID neighbor, final int distance) {
		final BroadcastRoute route = new BroadcastRoute(dest, neighbor, routeID, distance);
		if (!routes.contains(route)) {
			logger.trace("Peer " + peerID + " added route to " + dest);
			routes.add(route);
		}
	}

	public List<BroadcastRoute> getAllRoutes() {
		return Collections.unmodifiableList(routes);
	}

	private boolean removeSearch(final MessageID routeID) {		
		//check active searches
		final Set<SearchMessage> newActiveSearches = new HashSet<SearchMessage>();
		for (final Iterator<SearchMessage> activeSearchesIterator = activeSearches.iterator(); activeSearchesIterator.hasNext(); ) {
			final SearchMessage activeSearch = activeSearchesIterator.next();
			if (activeSearch.getRemoteMessageID().equals(routeID)) {						
				activeSearchesIterator.remove();
				
				activeSearches.addAll(newActiveSearches);
				
				logUTable();
				return true;
			}
		}
		return false;
	}
	
	public boolean isSearchRoute(final MessageID routeID) {
		for (final Iterator<SearchMessage> it = getSearches().iterator(); it.hasNext();) {
			final SearchMessage search = it.next();
			if (search.getRemoteMessageID().equals(routeID))
				return true;
		}
		return false;
	}
}
