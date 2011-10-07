package graphcreation.collisionbased.sdg;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.util.Set;

import peer.message.MessageID;
import peer.peerid.PeerID;
import serialization.xml.XMLSerialization;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.Parameter;

public interface SDG extends XMLSerialization {

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
	public void removeLocalService(Service service);

	/**
	 * Connects a set of remote services with a local one
	 * 
	 * @param localService
	 *            the connected local service
	 * @param remoteSuccessors
	 *            the remote successors of this service
	 * @param remoteAncestors
	 *            the remote ancestors of this service
	 * @param collisionNode
	 *            the node which detected the collision
	 * @throws exception
	 *             is thrown when a non local service is passed
	 */
	public void connectRemoteServices(Service localService, Set<ServiceDistance> remoteSuccessors, Set<ServiceDistance> remoteAncestors, PeerID collisionNode) throws NonLocalServiceException;

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

	/**
	 * Gets the successors of the passed service
	 * 
	 * @param localService
	 *            the service whose successors are obtained
	 * @return the successors of the specified service
	 */
	public Set<ServiceDistance> getSuccessors(Service localService, PeerID collisionPeerID);

	/**
	 * Gets the ancestors of the passed service
	 * 
	 * @param localService
	 *            the service whose ancestors are obtained
	 * @return the ancestors of the specified service
	 */
	public Set<ServiceDistance> getAncestors(Service localService, PeerID collisionPeerID);

	public Set<ServiceDistance> getLocalAncestors(Service remoteService, PeerID collisionPeerID);

	public Set<ServiceDistance> getLocalSuccessors(Service remoteService, PeerID collisionPeerID);

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
	 * Removes the services obtained from the specified route
	 * 
	 * @param routeID
	 *            the route whose services are removed
	 * @return the
	 */
	public void removeServicesFromRoute(MessageID routeID);

	/**
	 * Gets a service using its service identifier.
	 * 
	 * @param serviceID
	 *            the identifier of the service to obtain
	 * @return the related service, null if the service does not exist
	 */
	public Service getService(String serviceID);

	/**
	 * Checks whether the specified services is local or not to the peer which
	 * hosts the SDG
	 * 
	 * @param service
	 *            the service to check whether it is local or no
	 * @return true if the service is local, false otherwise
	 */
	public boolean isLocal(Service service);

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
	 * Gets the input parameters of the specified service which are connected to
	 * the passed ancestor
	 * 
	 * @param service
	 *            the service whose connected inputs are obtained
	 * @param ancestor
	 *            the ancestor which the inputs are connected to
	 * @return the connected inputs of the service
	 */
	public Set<InputParameter> getConnectedInputs(Service service, Service ancestor);

	/**
	 * Checks if the passed service is contained in the SDK
	 * 
	 * @param service
	 *            the service id to check
	 * @return true if the service is contained in the SDG, false otherwise
	 */
	public boolean hasService(Service service);

	void removeServiceConnectedBy(Service service, PeerID peerID);

	public Set<ServiceDistance> servicesConnectedThrough(MessageID routeID);
}