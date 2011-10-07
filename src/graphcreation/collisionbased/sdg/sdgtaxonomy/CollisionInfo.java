package graphcreation.collisionbased.sdg.sdgtaxonomy;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import peer.peerid.PeerID;

class CollisionInfo {

	private final Set<ServiceDistance> services = new HashSet<ServiceDistance>();

	// a table which contains the real remote successors of a service
	private final Map<Service, Set<ServiceDistance>> connectedSuccessors = new HashMap<Service, Set<ServiceDistance>>();

	// a table which contains the real remote ancestors of a service
	private final Map<Service, Set<ServiceDistance>> connectedAncestors = new HashMap<Service, Set<ServiceDistance>>();

	private final PeerID peerID;

	public CollisionInfo(final PeerID peerID) {
		this.peerID = peerID;
	}

	public void add(final ServiceDistance remoteService) {
		services.add(remoteService);
	}

	public void addAncestor(final Service localService, final ServiceDistance remoteAncestor) {
		if (!connectedAncestors.containsKey(localService))
			connectedAncestors.put(localService, new HashSet<ServiceDistance>());
		connectedAncestors.get(localService).add(remoteAncestor);
	}

	public void addSuccessor(final Service localService, final ServiceDistance remoteSuccessor) {
		if (!connectedSuccessors.containsKey(localService))
			connectedSuccessors.put(localService, new HashSet<ServiceDistance>());
		connectedSuccessors.get(localService).add(remoteSuccessor);
	}

	public Set<ServiceDistance> getSuccessors(final Service service) {
		final Set<ServiceDistance> successors = new HashSet<ServiceDistance>();
		if (connectedSuccessors.containsKey(service))
			successors.addAll(connectedSuccessors.get(service));

		return successors;
	}

	public Set<ServiceDistance> getAncestors(final Service service) {
		final Set<ServiceDistance> ancestors = new HashSet<ServiceDistance>();
		if (connectedAncestors.containsKey(service))
			ancestors.addAll(connectedAncestors.get(service));

		return ancestors;
	}

	public boolean isLocal(final Service service) {
		return service.getPeerID().equals(peerID);
	}

	public Set<ServiceDistance> getServices() {
		return services;
	}

	public void removeService(final Service service) {
		services.remove(new ServiceDistance(service, Integer.valueOf(0)));

		if (isLocal(service)) {
			// local services entries are removed from remote connected tables
			connectedAncestors.remove(service);
			connectedSuccessors.remove(service);
		} else {
			// remote services are removed from remote connected tables entries
			for (final Set<ServiceDistance> remoteAncestors : connectedAncestors.values())
				remoteAncestors.remove(new ServiceDistance(service, Integer.valueOf(0)));

			for (final Set<ServiceDistance> remoteSuccessors : connectedSuccessors.values())
				remoteSuccessors.remove(new ServiceDistance(service, Integer.valueOf(0)));
		}
	}

	public Set<ServiceDistance> getRemoteConnectedServices(final Service service) {
		final Set<ServiceDistance> connectedServices = new HashSet<ServiceDistance>();

		if (connectedSuccessors.containsKey(service))
			connectedServices.addAll(connectedSuccessors.get(service));

		if (connectedAncestors.containsKey(service))
			connectedServices.addAll(connectedAncestors.get(service));

		return connectedServices;
	}

	public boolean isEmpty() {
		return services.isEmpty();
	}

	@Override
	public String toString() {
		return "S:" + services + " CA:" + connectedAncestors + " CS:" + connectedSuccessors;
	}

	public Set<ServiceDistance> getLocalAncestors(final Service remoteService) {
		final Set<ServiceDistance> localAncestors = new HashSet<ServiceDistance>();

		for (final Service localService : connectedSuccessors.keySet())
			for (final ServiceDistance remoteSuccessor : connectedSuccessors.get(localService))
				if (remoteSuccessor.getService().equals(remoteService))
					localAncestors.add(new ServiceDistance(localService, remoteSuccessor.getDistance()));

		return localAncestors;
	}

	public Set<ServiceDistance> getLocalSuccessors(final Service remoteService) {
		final Set<ServiceDistance> localSuccessors = new HashSet<ServiceDistance>();

		for (final Service localService : connectedAncestors.keySet())
			for (final ServiceDistance remoteAncestor : connectedAncestors.get(localService))
				if (remoteAncestor.getService().equals(remoteService))
					localSuccessors.add(new ServiceDistance(localService, remoteAncestor.getDistance()));

		return localSuccessors;
	}
}
