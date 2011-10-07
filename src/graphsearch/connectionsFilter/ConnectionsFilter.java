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
