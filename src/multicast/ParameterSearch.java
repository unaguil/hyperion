package multicast;

import java.util.List;
import java.util.Set;

import multicast.search.Route;
import multicast.search.message.SearchMessage.SearchType;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import serialization.xml.XMLSerializable;
import taxonomy.parameter.Parameter;
import dissemination.ParameterDisseminator;

public interface ParameterSearch extends XMLSerializable {

	/**
	 * Adds a local parameter to the local parameter table. After finalizing a
	 * change set commit() must called in order to propagate the changes.
	 * 
	 * @param parameter
	 *            the parameter to add.
	 * @return true if the parameter was correctly added, false otherwise
	 */
	public boolean addLocalParameter(Parameter parameter);

	/**
	 * Removes a local parameter from the local parameter table. After
	 * finalizing a change set commit() must called in order to propagate the
	 * changes.
	 * 
	 * @param parameter
	 *            the parameter to remove
	 * @return true if the parameter was correctly remove
	 */
	public boolean removeLocalParameter(Parameter parameter);

	/**
	 * Commits the changes performed using addLocalEntry() or removeLocalEntry()
	 * methods.
	 */
	public void commit();

	/**
	 * Enqueues a cancel search message for further sending.
	 * 
	 * @param parameters
	 *            the canceled parameters
	 * @param payload
	 *            the payload of the message
	 */
	public void sendCancelSearchMessage(Set<Parameter> parameters);

	/**
	 * Enqueues a generalize search message
	 * 
	 * @param generalizedParameters
	 *            the set of generalized parameters
	 */
	public void sendGeneralizeSearchMessage(Set<Parameter> generalizedParameters);

	public ParameterDisseminator getDisseminationLayer();

	// sends a remote multicast message. This message is routed to multiple
	// remote destinations.
	public void sendMulticastMessage(PeerIDSet destinations, PayloadMessage payload);
	
	public void sendMulticastMessage(PeerIDSet destinations, PayloadMessage payload, int distance);

	/**
	 * Sends a remote unicast message. This message is routed to a unique remote
	 * destination.
	 * 
	 * @param destination
	 *            the remote destination of the unicast message
	 * @param payload
	 *            the payload contained in the message
	 */
	public void sendUnicastMessage(PeerID destination, PayloadMessage payload);

	// Sends a message which searches for specified parameters with default TTLs
	public void sendSearchMessageDefaultTTL(Set<Parameter> parameters, PayloadMessage payload, SearchType searchType);
	
	public void sendSearchMessage(Set<SearchedParameter> searchedParameters, PayloadMessage payload, SearchType searchType);

	public Route getRoute(PeerID destination);

	boolean knowsRouteTo(PeerID dest);

	public List<? extends Route> getAllRoutes();
}