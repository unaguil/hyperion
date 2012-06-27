package graphcreation.collisionbased.sdg;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import multicast.search.Route;
import peer.peerid.PeerID;
import serialization.xml.XMLSerializable;
import taxonomy.parameter.Parameter;

public interface SDG extends XMLSerializable {

	/**
	 * Adds the passed service to the SDG.
	 * 
	 * @param service
	 *            the service to add
	 * @throws exception
	 *             is thrown when a non local service is passed
	 */
	public void addLocalService(Service service) throws NonLocalServiceException;

	/**
	 * Removes the passed local service from the SDG
	 * 
	 * @param service
	 *            the service to remove
	 */
	public Set<ServiceDistance> removeLocalService(Service service);

	/**
	 * Connects a set of remote services with a local one
	 * 
	 * @param localService
	 *            the connected local service
	 * @param remoteServices
	 *            the remote successors of this service
	 * @param collisionNode
	 *            the node which detected the collision
	 * @throws exception
	 *             is thrown when a non local service is passed
	 */
	public Set<ServiceDistance> connectServices(Service localService, Set<ServiceDistance> remoteServices, PeerID collisionNode);

	/**
	 * Gets all the successors of the passed service
	 * 
	 * @param localService
	 *            the service whose successors are obtained
	 * @return the successors of the specified service
	 */
	public Set<ServiceDistance> getSuccessors(Service localService);

	/**
	 * Gets all the ancestors of the passed service
	 * 
	 * @param localService
	 *            the service whose ancestors are obtained
	 * @return the ancestors of the specified service
	 */
	public Set<ServiceDistance> getAncestors(Service localService);

	public Set<ServiceDistance> getLocalAncestors(Service remoteService);

	public Set<ServiceDistance> getLocalSuccessors(Service remoteService);

	/**
	 * Find those local services whose parameters are compatible with the
	 * searched ones
	 * 
	 * @param parameters
	 *            the parameters whose services are obtained
	 * @return the services which have some of the specified parameters
	 */
	public Set<Service> findLocalCompatibleServices(Set<Parameter> parameters);

	/**
	 * Gets a service using its service identifier.
	 * 
	 * @param serviceID
	 *            the identifier of the service to obtain
	 * @return the related service, null if the service does not exist
	 */
	public Service getService(String serviceID);

	/**
	 * Gets those collision nodes which provide access to the passed service
	 * 
	 * @param service
	 *            the service whose collision nodes are obtained
	 * @return the collision nodes which give access to the specified service
	 */
	public Set<PeerID> getThroughCollisionNodes(Service service);

	/**
	 * Gets all remote services which are connected with the specified service
	 * 
	 * @param the
	 *            service whose remote connected services are obtained
	 * @return the set of remote connected services
	 */
	public Set<ServiceDistance> getRemoteConnectedServices(Service service);

	/**
	 * Checks if the passed service is contained in the SDK
	 * 
	 * @param service
	 *            the service id to check
	 * @return true if the service is contained in the SDG, false otherwise
	 */
	public boolean contains(Service service);
	
	public Route getShortestRoute(PeerID destination);

	public void removeRemoteService(Service remoteService);

	public void checkServices(Set<PeerID> lostDestinations, Map<Service, Set<Service>> lostAncestors, Map<Service, Set<Service>> lostSuccessors);

	public void removeIndirectRoute(PeerID dest, PeerID collisionNode);

	public Set<ServiceDistance> getInaccesibleServices();

	public Set<Route> getRoutes(PeerID peerID);
	
	public List<Route> getDirectRoutes(PeerID ID);
	
	public List<Route> getIndirectRoutes(PeerID ID);
}