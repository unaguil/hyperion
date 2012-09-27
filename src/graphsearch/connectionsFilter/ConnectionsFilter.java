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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package graphsearch.connectionsFilter;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.util.Utility;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ConnectionsFilter {

	public static void filter(final Map<Service, Set<ServiceDistance>> foundRemoteSuccessors, final Map<Service, Set<ServiceDistance>> foundRemoteAncestors) {
		filterConnections(foundRemoteSuccessors);
		filterConnections(foundRemoteAncestors);
	}

	private static void filterConnections(final Map<Service, Set<ServiceDistance>> connections) {
		// Filter those connections among INIT and GOAL services form different
		// compositions
		for (final Iterator<Entry<Service, Set<ServiceDistance>>> itServices = connections.entrySet().iterator(); itServices.hasNext();) {
			final Entry<Service, Set<ServiceDistance>> entry = itServices.next();
			final Service service = entry.getKey();
			if (Utility.isGoalService(service) || Utility.isINITService(service))
				for (final Iterator<ServiceDistance> itSuccessors = entry.getValue().iterator(); itSuccessors.hasNext();) {
					final Service successor = itSuccessors.next().getService();
					if (Utility.isGoalService(successor) || Utility.isINITService(successor) && !Utility.sameComposition(service, successor))
						itSuccessors.remove();
				}

			// If connection collection is empty after removal -> remove entry
			if (entry.getValue().isEmpty())
				itServices.remove();
		}
	}
}
