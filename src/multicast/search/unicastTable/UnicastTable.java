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
import java.util.Map.Entry;
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

import multicast.search.message.SearchMessage;
import multicast.search.message.SearchMessage.SearchType;
import multicast.search.message.SearchResponseMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import peer.message.MessageID;
import peer.peerid.PeerID;
import serialization.xml.XMLSerialization;
import taxonomy.Taxonomy;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;
import util.logger.Logger;

/**
 * This class contains the information about routes. It maintains a table of
 * destinations and the next hop to reach them.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class UnicastTable implements XMLSerialization {

	class Route implements Comparable<Route> {

		// the destination of the route
		private final PeerID dest;

		// the next hop of the route
		private final PeerID neighbor;

		// the id of the route
		private final MessageID routeID;

		// if the route is local to the host peer
		private final boolean local;

		// distance to the destination peer
		private final int distance;

		public Route(final PeerID dest, final PeerID neighbor, final MessageID routeID, final boolean local, final int distance) {
			this.dest = dest;
			this.neighbor = neighbor;
			this.routeID = routeID;
			this.local = local;
			this.distance = distance;
		}

		public PeerID getNeighbor() {
			return neighbor;
		}

		public PeerID getDest() {
			return dest;
		}

		public MessageID getRouteID() {
			return routeID;
		}

		public boolean isLocal() {
			return local;
		}

		public int getDistance() {
			return distance;
		}

		@Override
		public String toString() {
			return "(D:" + dest + " N:" + neighbor + ")";
		}

		@Override
		public boolean equals(final Object o) {
			if (!(o instanceof Route))
				return false;

			final Route route = (Route) o;

			return routeID.equals(route.routeID);
		}

		@Override
		public int hashCode() {
			return routeID.hashCode();
		}

		@Override
		public int compareTo(final Route route) {
			return routeID.compareTo(route.routeID);
		}
	}

	class ResponseRoute extends Route {

		// the route this one responds to
		private final MessageID respondedRouteID;

		public ResponseRoute(final PeerID dest, final PeerID neighbor, final MessageID routeID, final boolean local, final MessageID respondedRouteID, final int distance) {
			super(dest, neighbor, routeID, local, distance);
			this.respondedRouteID = respondedRouteID;
		}

		public MessageID getRespondedRouteID() {
			return respondedRouteID;
		}
	}

	// defines two types of routes: search or parameter
	private enum RouteType {
		PARAMETER, SEARCH
	}

	// Strings used for XML serialization
	private final static String ROUTE_DEST_ATTRIB = "dest";
	private final static String ROUTE_PARAM_ATTRIB = "parameter";
	private final static String ROUTE_TAG = "route";
	private final static String ROUTE_NEIGHBOR_ATTRIB = "through";
	private final static String ROUTE_TYPE_ATTRIB = "type";
	private final static String UNICAST_TABLE_TAG = "unicastTable";

	// table which contains known parameter routes. It relates a parameter with
	// all the nodes providing it
	private final Map<ResponseRoute, Set<Parameter>> parameterRoutes = new HashMap<ResponseRoute, Set<Parameter>>();

	// table which contains known search routes. It relates a parameter with all
	// the nodes searching for it
	private final Map<Route, Set<Parameter>> searchRoutes = new HashMap<Route, Set<Parameter>>();

	// the list contains the active searches for the current node. It is related
	// with a search route
	private final List<SearchMessage> activeSearches = new ArrayList<SearchMessage>();

	// the identification of the peer which hosts the unicast table
	private final PeerID peerID;

	private final Logger logger = Logger.getLogger(UnicastTable.class);

	/**
	 * Constructor of the unicast table
	 * 
	 * @param peerID
	 *            the peer which hosts the unicast table
	 */
	public UnicastTable(final PeerID peerID) {
		this.peerID = peerID;
	}

	/**
	 * Updates the route table using the information provided by the search
	 * message. It also add the passed search message as an active search.
	 * 
	 * @param searchMessage
	 *            the search message used to update the unicast table.
	 * @return true if the search message updated the unicast table, false
	 *         otherwise
	 */
	public boolean updateUnicastTable(final SearchMessage searchMessage) {
		// Check if the received search message was not previously received (it
		// is contained in the active searches)
		if (!isActiveSearch(searchMessage)) {
			final boolean local = searchMessage.getSource().equals(peerID);

			// Add the search to the current active ones
			activeSearches.add(searchMessage);

			for (final Parameter p : searchMessage.getSearchedParameters())
				// add a new route to reach source through the neighbor node
				// which sent the message
				addSearchRoute(searchMessage.getRemoteMessageID(), p, searchMessage.getSource(), searchMessage.getSender(), local, searchMessage.getDistance());

			logger.trace("Peer " + peerID + " utable " + this);

			return true;
		}

		logger.trace("Peer " + peerID + " discarded search message " + searchMessage + " because it was an active search");

		return false;
	}

	/**
	 * Updates the route table using the information provided by the response
	 * message.
	 * 
	 * @param searchResponseMessage
	 *            the search response message used to update the unicast table.
	 */
	public void updateUnicastTable(final SearchResponseMessage searchResponseMessage) {
		final boolean local = searchResponseMessage.getSource().equals(peerID);

		for (final Parameter p : searchResponseMessage.getParameters())
			// add a new route to reach source through the neighbor node which
			// sent the message
			addParameterRoute(searchResponseMessage.getRemoteMessageID(), p, searchResponseMessage.getSource(), searchResponseMessage.getSender(), local, searchResponseMessage.getRespondedRouteID(), searchResponseMessage.getDistance());
	}

	/**
	 * Get the route identifiers which are starting in current host for the
	 * passed parameter
	 * 
	 * @param the
	 *            set of route identifiers starting in the current host for the
	 *            passed parameter
	 */
	public Set<MessageID> getLocalParameterRoutes(final Parameter p) {
		final Set<MessageID> localParameterRoutes = new HashSet<MessageID>();
		for (final Route route : parameterRoutes.keySet())
			if (parameterRoutes.get(route).contains(p) && route.isLocal())
				localParameterRoutes.add(route.getRouteID());

		return localParameterRoutes;
	}

	/**
	 * Removes a local parameter route
	 */

	/**
	 * Gets the identification of the routes going to the passed destination
	 * 
	 * @param dest
	 *            the destination whose route identifications are obtained
	 * @return the route identifications of the passed destination
	 */
	public Set<MessageID> getRouteIDs(final PeerID dest) {
		final Set<MessageID> routeIDs = new HashSet<MessageID>();
		for (final Route route : getAllRoutes())
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
		for (final Route route : getAllRoutes())
			if (route.getNeighbor().equals(neighbor))
				routeIDs.add(route.getRouteID());

		return routeIDs;
	}

	/**
	 * Gets the active searches
	 * 
	 * @return the list of active searches
	 */
	public List<SearchMessage> getActiveSearches() {
		return Collections.unmodifiableList(activeSearches);
	}

	/**
	 * Checks if the table knows a route to passed node
	 * 
	 * @param dest
	 *            the node to check if there is a known route to reached it
	 * @return true if the table contains a route to the node, false otherwise
	 */
	public boolean knowsRouteTo(final PeerID dest) {
		for (final Route route : getAllRoutes())
			if (route.getDest().equals(dest))
				return true;

		return false;
	}

	public int getDistanceTo(final PeerID dest) {
		for (final Route route : getAllRoutes())
			if (route.getDest().equals(dest))
				return route.getDistance();

		return 0;
	}

	/**
	 * Checks if the table contains the specified route.
	 * 
	 * @param the
	 *            route identifier
	 * @return true if the table contains the route, false otherwise
	 */
	public boolean isRoute(final MessageID routeID) {
		return isSearchRoute(routeID) || isParameterRoute(routeID);
	}

	/**
	 * Checks if the table contains the specified route as local.
	 * 
	 * @param the
	 *            route identifier
	 * @return true if the table contains the route as local, false otherwise
	 */
	public boolean isLocalRoute(final MessageID routeID) {
		return isLocalSearchRoute(routeID) || isLocalParameterRoute(routeID);
	}

	/**
	 * Checks if the table knows a search route to passed node
	 * 
	 * @param dest
	 *            the node to check if there is a known search route to reached
	 *            it
	 * @return true if the table contains a search route to the node, false
	 *         otherwise
	 */
	public boolean knowsSearchRouteTo(final PeerID dest) {
		for (final Route route : getAllRoutes())
			if (route.getDest().equals(dest) && isSearchRoute(route.getRouteID()))
				return true;

		return false;
	}

	/**
	 * Gets the set of search messages which were searching for the passed
	 * parameter
	 * 
	 * @param the
	 *            searched parameter
	 * @return the set of search messages which were searching for the passed
	 *         parameter
	 */
	public Set<SearchMessage> getActiveSearches(final Parameter parameter) {
		final Set<SearchMessage> searchMessages = new HashSet<SearchMessage>();
		for (final SearchMessage searchMessage : activeSearches)
			if (searchMessage.getSearchedParameters().contains(parameter))
				searchMessages.add(searchMessage);
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
	public Set<SearchMessage> getActiveSearches(final Parameter parameter, final PeerID peer) {
		final Set<SearchMessage> searchMessages = new HashSet<SearchMessage>();
		for (final SearchMessage searchMessage : activeSearches)
			if (searchMessage.getSearchedParameters().contains(parameter) && searchMessage.getSource().equals(peer))
				searchMessages.add(searchMessage);
		return searchMessages;
	}

	/**
	 * Gets the set of search messages which were searching a parameter subsumed
	 * by the specified one and are originated in the specified peer
	 * 
	 * @param the
	 *            parameter which subsumes the searched ones
	 * @param the
	 *            peer which originated the search message
	 * @param taxonomy
	 *            the taxonomy used to check for subsumtion
	 * @return the set of active searches
	 */
	public Set<SearchMessage> getSubsumedActiveSearches(final Parameter parameter, final PeerID peer, final Taxonomy taxonomy) {
		final Set<SearchMessage> searchMessages = new HashSet<SearchMessage>();
		for (final SearchMessage searchMessage : activeSearches)
			if (searchMessage.getSource().equals(peer))
				for (final Parameter p : searchMessage.getSearchedParameters())
					// Check for subsumtion excluding equality
					if (!parameter.equals(p) && taxonomy.subsumes(parameter.getID(), p.getID()))
						searchMessages.add(searchMessage);
		return searchMessages;
	}

	/**
	 * Gets the set of parameters which are being searched by the current node
	 * 
	 * @return the parameters which are being searched by the current node
	 */
	public Set<Parameter> getSearchedParameters() {
		final Set<Parameter> searchedParameters = new HashSet<Parameter>();
		for (final SearchMessage activeSearch : activeSearches)
			if (activeSearch.getSource().equals(peerID))
				searchedParameters.addAll(activeSearch.getSearchedParameters());

		return searchedParameters;
	}

	/**
	 * Gets the neighbor which the passed destination peer is reached through
	 * 
	 * @param destination
	 *            the peer to get its route
	 * @return the neighbor through which the passed peer is reached, VOID peer
	 *         if destination is unknown
	 */
	public PeerID getNeighbor(final PeerID destination) {
		// Find the first route to the destination
		for (final Route route : getAllRoutes())
			if (route.getDest().equals(destination))
				return route.getNeighbor();

		return PeerID.VOID_PEERID;
	}

	/**
	 * Removes a route completely from search routes and parameter routes
	 * 
	 * @param routeID
	 *            the identification of the route to remove
	 * @param the
	 *            neighbor which sent the route removal message
	 * @return the destination of the removed route
	 */
	public PeerID removeRoute(final MessageID routeID, final PeerID neighbor) {
		if (isParameterRoute(routeID))
			for (final Iterator<ResponseRoute> it = parameterRoutes.keySet().iterator(); it.hasNext();) {
				final Route route = it.next();
				if (route.getRouteID().equals(routeID)) {
					it.remove();
					return route.getDest();
				}
			}

		if (isSearchRoute(routeID))
			for (final Iterator<Route> it = searchRoutes.keySet().iterator(); it.hasNext();) {
				final Route route = it.next();
				// Search routes are only removed if the message comes from the
				// neighbor which sent the route creation or the same node
				if (route.getRouteID().equals(routeID) && (route.getNeighbor().equals(neighbor) || neighbor.equals(peerID))) {
					it.remove();
					removeActiveSearches(route.getDest());

					// Remove associated response route if exists
					final Set<ResponseRoute> associatedResponseRoutes = getAssociatedResponseRoutes(routeID);
					for (final ResponseRoute associatedResponseRoute : associatedResponseRoutes)
						parameterRoutes.remove(associatedResponseRoute);
					return route.getDest();
				}
			}

		return PeerID.VOID_PEERID;
	}

	private Set<ResponseRoute> getAssociatedResponseRoutes(final MessageID searchRoute) {
		final Set<ResponseRoute> associatedResponseRoutes = new HashSet<ResponseRoute>();
		for (final ResponseRoute responseRoute : parameterRoutes.keySet())
			if (responseRoute.getRespondedRouteID().equals(searchRoute))
				associatedResponseRoutes.add(responseRoute);

		return associatedResponseRoutes;
	}

	/**
	 * Removes the specified parameters from the passed route
	 * 
	 * @param parameters
	 *            the set of parameters to remove
	 * @param routeID
	 *            the route identifier
	 * @return the parameters which have been removed
	 */
	public Set<Parameter> removeParameters(final Set<Parameter> parameters, final MessageID routeID) {
		final Set<Parameter> removedParameters = new HashSet<Parameter>();
		if (isParameterRoute(routeID)) {
			final Route route = getParameterRoute(routeID);

			for (final Parameter p : parameters)
				if (parameterRoutes.get(route).remove(p))
					removedParameters.add(p);

			if (parameterRoutes.get(route).isEmpty())
				parameterRoutes.remove(route);
		} else if (isSearchRoute(routeID)) {
			final Route route = getSearchRoute(routeID);

			for (final Parameter p : parameters)
				if (searchRoutes.get(route).removeAll(parameters))
					removedParameters.add(p);

			final SearchMessage activeSearch = getActiveSearch(routeID);
			activeSearch.removeParameters(parameters);

			// Remove parameters from associated route
			final Set<ResponseRoute> associatedResponseRoutes = getAssociatedResponseRoutes(routeID);
			for (final ResponseRoute associatedResponseRoute : associatedResponseRoutes)
				removeParameters(parameters, associatedResponseRoute.getRouteID());

			if (searchRoutes.get(route).isEmpty()) {
				searchRoutes.remove(route);
				removeActiveSearch(routeID);
			}
		} else {
			final SearchMessage activeSearch = getActiveSearch(routeID);
			if (activeSearch != null) {
				activeSearch.removeParameters(parameters);

				removedParameters.addAll(parameters);

				if (activeSearch.getSearchedParameters().isEmpty())
					removeActiveSearch(routeID);
			}

			// Remove parameters from associated route
			final Set<ResponseRoute> associatedResponseRoutes = getAssociatedResponseRoutes(routeID);
			for (final ResponseRoute associatedResponseRoute : associatedResponseRoutes)
				removeParameters(parameters, associatedResponseRoute.getRouteID());
		}

		return removedParameters;
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
		final SearchMessage searchMessage = getActiveSearch(routeID);
		if (searchMessage != null && searchMessage.getSearchType().equals(SearchType.Generic)) {
			final Map<Parameter, Parameter> results = searchMessage.generalizeParameters(generalizations, taxonomy);
			final Route searchRoute = getSearchRoute(searchMessage.getRemoteMessageID());
			// Update the search route with the new parameters
			for (final Entry<Parameter, Parameter> entry : results.entrySet()) {
				searchRoutes.get(searchRoute).remove(entry.getKey());
				searchRoutes.get(searchRoute).add(entry.getValue());
			}
			return results;
		}
		return new HashMap<Parameter, Parameter>();
	}

	/**
	 * Gets the active search associated with the specified routeID
	 * 
	 * @param routeID
	 *            the identifier of the search route
	 * @return the active search associated with the specified routeID
	 */
	public SearchMessage getActiveSearch(final MessageID routeID) {
		for (final SearchMessage searchMessage : activeSearches)
			if (searchMessage.getRemoteMessageID().equals(routeID))
				return searchMessage;
		return null;
	}

	private ResponseRoute getParameterRoute(final MessageID routeID) {
		for (final ResponseRoute route : parameterRoutes.keySet())
			if (route.getRouteID().equals(routeID))
				return route;
		return null;
	}

	private Route getSearchRoute(final MessageID routeID) {
		for (final Route route : searchRoutes.keySet())
			if (route.getRouteID().equals(routeID))
				return route;
		return null;
	}

	/**
	 * Checks if the specified route identifier is a parameter route
	 * 
	 * @param routeID
	 *            the route identifier
	 * @return true if the route is a parameter route, false otherwise
	 */
	public boolean isParameterRoute(final MessageID routeID) {
		return parameterRoutes.containsKey(new Route(PeerID.VOID_PEERID, PeerID.VOID_PEERID, routeID, false, 0));
	}

	/**
	 * Checks if the specified route identifier is a search route
	 * 
	 * @param routeID
	 *            the route identifier
	 * @return true if the route is a search route, false otherwise
	 */
	public boolean isSearchRoute(final MessageID routeID) {
		return searchRoutes.containsKey(new Route(PeerID.VOID_PEERID, PeerID.VOID_PEERID, routeID, false, 0));
	}

	/**
	 * Checks if the specified route identifier is a search route and is local
	 * 
	 * @param routeID
	 *            the route identifier to check
	 * @return true if the route is a local search route, false otherwise
	 */
	public boolean isLocalSearchRoute(final MessageID routeID) {
		final Route route = new Route(PeerID.VOID_PEERID, PeerID.VOID_PEERID, routeID, false, 0);
		for (final Route r : searchRoutes.keySet())
			if (r.equals(route) && r.isLocal())
				return true;
		return false;
	}

	/**
	 * Checks if the specified route identifier is a parameter route and is
	 * local
	 * 
	 * @param routeID
	 *            the route identifier to check
	 * @return true if the route is a local parameter route, false otherwise
	 */
	public boolean isLocalParameterRoute(final MessageID routeID) {
		final Route route = new Route(PeerID.VOID_PEERID, PeerID.VOID_PEERID, routeID, false, 0);
		for (final Route r : parameterRoutes.keySet())
			if (r.equals(route) && r.isLocal())
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

		final NodeList routeList = document.getElementsByTagName(ROUTE_TAG);
		for (int i = 0; i < routeList.getLength(); i++) {
			final Element e = (Element) routeList.item(i);
			final String dest = e.getAttribute(ROUTE_DEST_ATTRIB);
			final String through = e.getAttribute(ROUTE_NEIGHBOR_ATTRIB);
			final String param = e.getAttribute(ROUTE_PARAM_ATTRIB);
			final RouteType type = RouteType.valueOf(e.getAttribute(ROUTE_TYPE_ATTRIB));
			try {
				if (type.equals(RouteType.SEARCH))
					addSearchRoute(new MessageID(new PeerID(dest), 0), ParameterFactory.createParameter(param), new PeerID(dest), new PeerID(through), false, 0);
				else if (type.equals(RouteType.PARAMETER))
					addParameterRoute(new MessageID(new PeerID(dest), 0), ParameterFactory.createParameter(param), new PeerID(dest), new PeerID(through), false, new MessageID(new PeerID(dest), 0), 0);
			} catch (final InvalidParameterIDException ipe) {
				throw new IOException(ipe);
			}
		}
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
		for (final Route route : searchRoutes.keySet())
			if (!route.isLocal())
				for (final Parameter p : searchRoutes.get(route)) {
					final Element routeElement = doc.createElement(ROUTE_TAG);
					routeElement.setAttribute(ROUTE_DEST_ATTRIB, route.getDest().toString());
					routeElement.setAttribute(ROUTE_NEIGHBOR_ATTRIB, route.getNeighbor().toString());
					routeElement.setAttribute(ROUTE_PARAM_ATTRIB, p.toString());
					routeElement.setAttribute(ROUTE_TYPE_ATTRIB, RouteType.SEARCH.toString());
					root.appendChild(routeElement);
				}

		for (final Route route : parameterRoutes.keySet())
			if (!route.isLocal())
				for (final Parameter p : parameterRoutes.get(route)) {
					final Element routeElement = doc.createElement(ROUTE_TAG);
					routeElement.setAttribute(ROUTE_DEST_ATTRIB, route.getDest().toString());
					routeElement.setAttribute(ROUTE_NEIGHBOR_ATTRIB, route.getNeighbor().toString());
					routeElement.setAttribute(ROUTE_PARAM_ATTRIB, p.toString());
					routeElement.setAttribute(ROUTE_TYPE_ATTRIB, RouteType.PARAMETER.toString());
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
	public boolean equals(final Object o) {
		if (!(o instanceof UnicastTable))
			return false;

		final UnicastTable unicastTable = (UnicastTable) o;

		if (!getParameterRoutesParameters().equals(unicastTable.getParameterRoutesParameters()))
			return false;

		if (!getSearchRoutesParameters().equals(unicastTable.getSearchRoutesParameters()))
			return false;

		boolean parameterEquals = true;
		if (!(parameterRoutes.isEmpty() && unicastTable.parameterRoutes.isEmpty()))
			for (final Parameter p : getParameterRoutesParameters()) {
				final List<Route> localParameterRoutes = getParameterRoutes(p);
				final List<Route> otherParameterRoutes = unicastTable.getParameterRoutes(p);

				Collections.sort(localParameterRoutes);
				Collections.sort(otherParameterRoutes);
				parameterEquals = parameterEquals && equals(localParameterRoutes, otherParameterRoutes);
			}

		boolean searchEquals = true;
		if (!(searchRoutes.isEmpty() && unicastTable.searchRoutes.isEmpty()))
			for (final Parameter p : getSearchRoutesParameters()) {
				final List<Route> localSearchRoutes = getSearchRoutes(p);
				final List<Route> otherSearchRoutes = unicastTable.getSearchRoutes(p);

				Collections.sort(localSearchRoutes);
				Collections.sort(otherSearchRoutes);
				searchEquals = searchEquals && equals(localSearchRoutes, otherSearchRoutes);
			}

		return parameterEquals && searchEquals;
	}

	private boolean equals(final List<Route> list1, final List<Route> list2) {
		if (list1.size() != list2.size())
			return false;

		final Iterator<Route> it = list2.iterator();
		for (final Route route : list1) {
			final Route route2 = it.next();
			if (!route.getDest().equals(route2.getDest()) || !route.getNeighbor().equals(route2.getNeighbor()))
				return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + parameterRoutes.hashCode();
		result = 37 * result + searchRoutes.hashCode();

		return result;
	}

	@Override
	public String toString() {
		final Map<Parameter, Set<Route>> searchRoutesGroupedByParameter = getSearchRoutesGroupedByParameter();
		final Map<Parameter, Set<Route>> parameterRoutesGroupedByParameter = getParameterRoutesGroupedByParameter();

		final StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("[");
		strBuilder.append(RouteType.SEARCH + ": ");
		for (final Entry<Parameter, Set<UnicastTable.Route>> entry : searchRoutesGroupedByParameter.entrySet()) {
			strBuilder.append("" + entry.getKey().toString() + ": ");
			for (final Route route : entry.getValue())
				strBuilder.append("(" + route.getDest() + " -> " + route.getNeighbor() + ")");
		}
		strBuilder.append(" " + RouteType.PARAMETER + ": ");
		for (final Entry<Parameter, Set<UnicastTable.Route>> entry : parameterRoutesGroupedByParameter.entrySet()) {
			strBuilder.append("" + entry.getKey().toString() + ": ");
			for (final Route route : entry.getValue())
				strBuilder.append("(" + route.getDest() + " -> " + route.getNeighbor() + ")");
		}
		strBuilder.append("]");

		return strBuilder.toString();
	}

	public Map<Parameter, Set<Route>> getSearchRoutesGroupedByParameter() {
		final Map<Parameter, Set<Route>> searchRoutesGroupedByParameter = new HashMap<Parameter, Set<Route>>();

		for (final Route route : searchRoutes.keySet())
			for (final Parameter p : searchRoutes.get(route)) {
				if (!searchRoutesGroupedByParameter.containsKey(p))
					searchRoutesGroupedByParameter.put(p, new HashSet<Route>());
				if (!route.isLocal())
					searchRoutesGroupedByParameter.get(p).add(route);
			}

		return searchRoutesGroupedByParameter;
	}

	public Map<Parameter, Set<Route>> getParameterRoutesGroupedByParameter() {
		final Map<Parameter, Set<Route>> parameterRoutesGroupedByParameter = new HashMap<Parameter, Set<Route>>();

		for (final Route route : parameterRoutes.keySet())
			for (final Parameter p : parameterRoutes.get(route)) {
				if (!parameterRoutesGroupedByParameter.containsKey(p))
					parameterRoutesGroupedByParameter.put(p, new HashSet<Route>());
				if (!route.isLocal())
					parameterRoutesGroupedByParameter.get(p).add(route);
			}

		return parameterRoutesGroupedByParameter;
	}

	// remove all active searches coming from the passed source
	private void removeActiveSearches(final PeerID dest) {
		// Remove the associated active search
		for (final Iterator<SearchMessage> it = activeSearches.iterator(); it.hasNext();) {
			final SearchMessage searchMessage = it.next();
			if (searchMessage.getSource().equals(dest))
				it.remove();
		}
	}

	private void removeActiveSearch(final MessageID routeID) {
		for (final Iterator<SearchMessage> it = activeSearches.iterator(); it.hasNext();) {
			final SearchMessage activeSearch = it.next();
			if (activeSearch.getRemoteMessageID().equals(routeID))
				it.remove();
		}
	}

	// Adds a new known parameter route
	private void addParameterRoute(final MessageID routeID, final Parameter parameter, final PeerID dest, final PeerID neighbor, final boolean local, final MessageID respondedRouteID, final int distance) {

		final ResponseRoute route = new ResponseRoute(dest, neighbor, routeID, local, respondedRouteID, distance);

		// Add the route to parameter routes creating an entry if does not
		// already exist
		if (!parameterRoutes.containsKey(route))
			parameterRoutes.put(route, new HashSet<Parameter>());

		parameterRoutes.get(route).add(parameter);
	}

	// Adds a new known search route.
	private void addSearchRoute(final MessageID routeID, final Parameter parameter, final PeerID dest, final PeerID neighbor, final boolean local, final int distance) {
		final Route route = new Route(dest, neighbor, routeID, local, distance);

		// Add the route to parameter routes creating an entry if does not
		// already exist
		if (!searchRoutes.containsKey(route))
			searchRoutes.put(route, new HashSet<Parameter>());

		searchRoutes.get(route).add(parameter);
	}

	private List<Route> getParameterRoutes(final Parameter p) {
		final List<Route> routes = new ArrayList<Route>();
		for (final Route route : parameterRoutes.keySet())
			if (parameterRoutes.get(route).contains(p) && !route.isLocal())
				routes.add(route);
		return routes;
	}

	private List<Route> getSearchRoutes(final Parameter p) {
		final List<Route> routes = new ArrayList<Route>();
		for (final Route route : searchRoutes.keySet())
			if (searchRoutes.get(route).contains(p) && !route.isLocal())
				routes.add(route);
		return routes;
	}

	private Set<Route> getAllRoutes() {
		final Set<Route> routes = new HashSet<Route>(parameterRoutes.keySet());
		routes.addAll(searchRoutes.keySet());
		return routes;
	}

	private Set<Parameter> getParameterRoutesParameters() {
		final Set<Parameter> totalParameters = new HashSet<Parameter>();
		for (final Set<Parameter> parameters : parameterRoutes.values())
			totalParameters.addAll(parameters);
		return totalParameters;
	}

	private Set<Parameter> getSearchRoutesParameters() {
		final Set<Parameter> totalParameters = new HashSet<Parameter>();
		for (final Set<Parameter> parameters : searchRoutes.values())
			totalParameters.addAll(parameters);
		return totalParameters;
	}

	// checks if the message search message is contained in the active searches
	// list
	private boolean isActiveSearch(final SearchMessage searchMessage) {
		return activeSearches.contains(searchMessage);
	}

	public Set<Parameter> getParameters(final MessageID routeID) {
		for (final ResponseRoute route : parameterRoutes.keySet())
			if (route.getRouteID().equals(routeID))
				return parameterRoutes.get(route);
		return new HashSet<Parameter>();
	}

	public Set<Parameter> getSearchedParameters(final MessageID routeID) {
		final Route searchRoute = getSearchRoute(routeID);
		return searchRoutes.get(searchRoute);
	}

	public MessageID getAssociatedSearchRoute(final MessageID routeID) {
		if (isParameterRoute(routeID)) {
			final ResponseRoute parameterRoute = getParameterRoute(routeID);
			return parameterRoute.getRespondedRouteID();
		}
		return null;
	}
}
