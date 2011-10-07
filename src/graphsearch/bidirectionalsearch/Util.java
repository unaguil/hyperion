package graphsearch.bidirectionalsearch;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Util {

	public static void addServiceDistances(final Map<Service, Set<ServiceDistance>> distanceBetweenServices, final Map<Service, Set<ServiceDistance>> serviceDistances) {
		for (final Entry<Service, Set<ServiceDistance>> entry : serviceDistances.entrySet()) {
			final Service service = entry.getKey();
			if (!distanceBetweenServices.containsKey(service))
				distanceBetweenServices.put(service, new HashSet<ServiceDistance>());
			distanceBetweenServices.get(service).addAll(entry.getValue());
		}
	}

	public static Set<Service> getAllServices(final Set<ServiceDistance> serviceDistances) {
		final Set<Service> services = new HashSet<Service>();
		for (final ServiceDistance sDistance : serviceDistances)
			services.add(sDistance.getService());
		return services;
	}

	public static Set<Service> getAllServices(final Map<Service, Set<ServiceDistance>> distanceBetweenServices) {
		final Set<Service> services = new HashSet<Service>();

		services.addAll(distanceBetweenServices.keySet());

		for (final Set<ServiceDistance> serviceDistances : distanceBetweenServices.values())
			for (final ServiceDistance sDistance : serviceDistances)
				services.add(sDistance.getService());

		return services;
	}
}
