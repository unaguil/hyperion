package graphcreation;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;

import java.util.Set;

import multicast.ParameterSearch;
import peer.message.PayloadMessage;
import serialization.xml.XMLSerializable;
import taxonomy.parameter.InputParameter;

public interface GraphCreator extends XMLSerializable {

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
	 * Provides a reference to the routing layer.
	 * 
	 * @return a reference to the routing layer
	 */
	public ParameterSearch getPSearch();

	public Service getService(String serviceID);

	public boolean isLocal(Service service);

	public Set<ServiceDistance> getAncestors(Service service);

	public Set<ServiceDistance> getSuccessors(Service service);

	public Set<InputParameter> getConnectedInputs(Service service, Service ancestor);
}