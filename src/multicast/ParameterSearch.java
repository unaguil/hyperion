package multicast;

import java.util.List;
import java.util.Set;

import multicast.search.Route;
import multicast.search.message.SearchMessage.SearchType;
import peer.message.BroadcastMessage;
import peer.peerid.PeerID;
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
	public void sendMulticastMessage(Set<PeerID> destinations, BroadcastMessage payload, boolean directBroadcast);
	
	public void sendMulticastMessage(Set<PeerID> destinations, BroadcastMessage payload, int distance, boolean directBroadcast);

	// Sends a message which searches for specified parameters with default TTLs
	public void sendSearchMessageDefaultTTL(Set<Parameter> parameters, BroadcastMessage payload, SearchType searchType);
	
	public void sendSearchMessage(Set<SearchedParameter> searchedParameters, BroadcastMessage payload, SearchType searchType);

	public Route getRoute(PeerID destination);

	boolean knowsRouteTo(PeerID dest);

	public List<? extends Route> getAllRoutes();

	public Set<? extends PeerID> getKnownDestinations();
}