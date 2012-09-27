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
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package graphcreation;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;

import java.util.Set;

import multicast.ParameterSearch;
import peer.message.BroadcastMessage;
import serialization.xml.XMLSerializable;
import taxonomy.Taxonomy;

public interface GraphCreator extends XMLSerializable {
	
	public enum GraphType { FORWARD, BACKWARD, BIDIRECTIONAL }

	public void forwardMessage(BroadcastMessage payload, Set<Service> destinations, boolean directBroadcast, boolean multiplePaths);

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

	public Set<ServiceDistance> getAncestors(Service service);

	public Set<ServiceDistance> getSuccessors(Service service);

	public boolean containsLocalService(Service service);

	public Taxonomy getTaxonomy();
}