package graphcreation;

import graphcreation.collisionbased.sdg.SDG;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;

import java.util.Set;

import multicast.ParameterSearch;
import peer.message.PayloadMessage;

public interface GraphCreator {

	public void forwardMessage(PayloadMessage payload, Set<Service> destinations);

	/**
	 * Intended for upper layers usage
	 * 
	 * @param locallyAddedServices
	 *            the services to be added
	 * @param locallyRemovedServices
	 *            the services to be removed
	 */
	public void manageLocalServices(ServiceList locallyAddedServices, ServiceList locallyRemovedServices);

	/**
	 * Gets the service dependency graph of this node.
	 * 
	 * @return the service dependency graph of this node
	 */
	public SDG getSDG();

	/**
	 * Provides a reference to the routing layer.
	 * 
	 * @return a reference to the routing layer
	 */
	public ParameterSearch getPSearch();

}