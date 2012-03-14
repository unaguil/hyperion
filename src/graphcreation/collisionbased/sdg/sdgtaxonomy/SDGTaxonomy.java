package graphcreation.collisionbased.sdg.sdgtaxonomy;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.collisionbased.sdg.NonLocalServiceException;
import graphcreation.collisionbased.sdg.SDG;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import multicast.ParameterSearch;
import multicast.Util;
import multicast.search.Route;
import peer.peerid.PeerID;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;
import util.logger.Logger;

/**
 * This class manages the SDG graph.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class SDGTaxonomy implements SDG {

	//a set of known indirect routes
	private final Set<IndirectRoute> indirectRoutes = new HashSet<IndirectRoute>();

	// the local service dependency graph
	private final ExtendedServiceGraph eServiceGraph;

	// the id of the peer which hosts the service table
	private final PeerID peerID;
	
	private final ParameterSearch pSearch;

	// the used taxonomy
	private final Taxonomy taxonomy;

	private final Logger logger = Logger.getLogger(SDGTaxonomy.class);

	/**
	 * Constructor of the service SDG.
	 * 
	 * @param peerID
	 *            the id of the peer which hosts the SDG
	 * @param taxonomy
	 *            the taxonomy used for service calculation
	 */
	public SDGTaxonomy(final PeerID peerID, final ParameterSearch pSearch, final Taxonomy taxonomy) {
		this.peerID = peerID;
		this.taxonomy = taxonomy;
		this.pSearch = pSearch;
		eServiceGraph = new ExtendedServiceGraph(taxonomy);
	}

	@Override
	public void addLocalService(final Service service) throws NonLocalServiceException {
		if (isLocal(service))
			eServiceGraph.merge(service);
		else
			throw new NonLocalServiceException();
	}

	@Override
	public Set<ServiceDistance> connectServices(final Service localService, final Set<ServiceDistance> remoteServices, final PeerID collisionNode) {
		final Set<ServiceDistance> addedServices = new HashSet<ServiceDistance>();
		
		for (final ServiceDistance remoteService : remoteServices) {
			if (!eServiceGraph.contains(remoteService.getService())) {
				eServiceGraph.merge(remoteService.getService());
				addedServices.add(remoteService);
			}
				
			IndirectRoute route = new IndirectRoute(remoteService.getService().getPeerID(), collisionNode, remoteService.getDistance());
			indirectRoutes.add(route);
		}
		
		return addedServices; 
	}

	@Override
	public Set<ServiceDistance> getSuccessors(final Service service) {
		final Set<ServiceDistance> successors = new HashSet<ServiceDistance>();
		if (isLocal(service)) {
			ServiceNode serviceNode = eServiceGraph.getServiceNode(service);
			for (final ServiceNode successorNode : eServiceGraph.getSuccessors(serviceNode, false)) {
				if (isLocal(successorNode.getService()))
					successors.add(new ServiceDistance(successorNode.getService(), new Integer(0)));
				else
					successors.add(new ServiceDistance(successorNode.getService(), getDistance(successorNode.getService().getPeerID())));
			}
		}
		return successors;
	}
	
	private Integer getDistance(final PeerID dest) {
		final Route route = getRoute(dest);
		if (route != null)
			return new Integer(route.getDistance());
		return null;
	}

	@Override
	public Set<ServiceDistance> getAncestors(final Service service) {
		final Set<ServiceDistance> ancestors = new HashSet<ServiceDistance>();
		if (isLocal(service)) {
			ServiceNode serviceNode = eServiceGraph.getServiceNode(service);
			for (final ServiceNode ancestorNode : eServiceGraph.getAncestors(serviceNode, false)) {
				if (isLocal(ancestorNode.getService()))
					ancestors.add(new ServiceDistance(ancestorNode.getService(), new Integer(0)));
				else
					ancestors.add(new ServiceDistance(ancestorNode.getService(), getDistance(ancestorNode.getService().getPeerID())));
			}
		}
		return ancestors;
	}

	@Override
	public Set<ServiceDistance> removeLocalService(final Service service) {
		if (eServiceGraph.removeService(service))
			return removeNonLocalDisconnectedServices();
		return Collections.emptySet();
	}

	@Override
	public void removeRemoteService(final Service service) {
		eServiceGraph.removeService(service);
	}

	@Override
	public Set<Service> findLocalCompatibleServices(final Set<Parameter> parameters) {
		final Set<Service> services = new HashSet<Service>();
		for (final Service service : eServiceGraph.getServices())
			if (isLocal(service) && isCompatible(service, parameters))
				services.add(service);
		return services;
	}

	private boolean isCompatible(final Service service, final Set<Parameter> parameters) {
		for (final Parameter p : parameters)
			if (p instanceof InputParameter) {
				for (final InputParameter input : service.getInputParams())
					if (taxonomy.subsumes(p.getID(), input.getID()))
						return true;
			} else if (p instanceof OutputParameter)
				for (final OutputParameter output : service.getOutputParams())
					if (taxonomy.subsumes(p.getID(), output.getID()))
						return true;
		return false;
	}
	
	private void removeIndirectRoutesThrough(final Set<PeerID> peers) {
		for (final PeerID peer : peers) {
			removeIndirectRouteThrough(peer);
		}
	}
	
	private void removeIndirectRouteThrough(final PeerID collisionNode) {
		for (final Iterator<IndirectRoute> it = indirectRoutes.iterator(); it.hasNext(); ) {
			IndirectRoute route = it.next(); 
			if (route.getThrough().equals(collisionNode))
				it.remove();
		}
	}
	
	@Override
	public void removeIndirectRoute(final PeerID dest, final PeerID collisionNode) {
		for (final Iterator<IndirectRoute> it = indirectRoutes.iterator(); it.hasNext(); ) {
			IndirectRoute route = it.next(); 
			if (route.getThrough().equals(collisionNode) && route.getDest().equals(dest))
				it.remove();
		}
	}
	
	private boolean knowsRouteTo(final PeerID destination) {
		return knowsIndirectRouteTo(destination) || pSearch.knowsRouteTo(destination);
	}
	
	private boolean knowsIndirectRouteTo(final PeerID destination) {
		for (final IndirectRoute route : indirectRoutes) {
			if (route.getDest().equals(destination))
				return true;
		}
		return false;
	}
	
	@Override
	public Set<ServiceDistance> getInaccesibleServices() {
		final Set<ServiceDistance> inaccesibleServices = new HashSet<ServiceDistance>();
		for (final Service service : eServiceGraph.getServices()) {
			if (!knowsRouteTo(service.getPeerID()))
				inaccesibleServices.add(new ServiceDistance(service, null));
		}
		
		return inaccesibleServices;
	}
	
	@Override
	public void checkServices(final Set<PeerID> lostDestinations, final Map<Service, Set<Service>> lostAncestors, final Map<Service, Set<Service>> lostSuccessors) {
		removeIndirectRoutesThrough(lostDestinations);
		Set<ServiceDistance> lostServices = getInaccesibleServices();
		logger.trace("Peer " + peerID + " lost services: " + lostServices);

		for (final ServiceDistance remoteService : lostServices) {
			for (final ServiceDistance successor : getLocalSuccessors(remoteService.getService())) {
				if (!lostAncestors.containsKey(successor.getService()))
					lostAncestors.put(successor.getService(), new HashSet<Service>());
				lostAncestors.get(successor.getService()).add(remoteService.getService());
			}
		}

		for (final ServiceDistance remoteService : lostServices) {
			for (final ServiceDistance ancestor : getLocalAncestors(remoteService.getService())) {
				if (!lostSuccessors.containsKey(ancestor.getService()))
					lostSuccessors.put(ancestor.getService(), new HashSet<Service>());
				lostSuccessors.get(ancestor.getService()).add(remoteService.getService());
			}
		}
		
		for (final ServiceDistance lostService : lostServices) 
			removeRemoteService(lostService.getService());
		
		logger.trace("Peer " + peerID + " created new SDG" + this.toString());
	}

	@Override
	public Service getService(final String serviceID) {
		return eServiceGraph.getService(serviceID);
	}

	@Override
	public String toString() {
		return eServiceGraph.toString();
	}

	@Override
	public boolean isLocal(final Service service) {
		return service.getPeerID().equals(peerID);
	}
	
	@Override
	public Set<PeerID> getThroughCollisionNodes(final Service service) {
		final Set<PeerID> collisionNodes = new HashSet<PeerID>();
		for (final IndirectRoute route : indirectRoutes) {
			if (route.getDest().equals(service.getPeerID()))
				collisionNodes.add(route.getThrough());
		}
		return collisionNodes;
	}
	
	private Set<IndirectRoute> getIndirectRoutes(final PeerID destination) {
		final Set<IndirectRoute> routes = new HashSet<IndirectRoute>();
		for (final IndirectRoute indirectRoute : indirectRoutes) {
			if (indirectRoute.getDest().equals(destination))
				routes.add(indirectRoute);
		}
		return routes;
	}

	@Override
	public Route getRoute(final PeerID destination) {
		final List<Route> availableRoutes = new ArrayList<Route>();
		availableRoutes.addAll(getIndirectRoutes(destination));
		
		final Route directRoute = pSearch.getRoute(destination);
		if (directRoute != null)
			availableRoutes.add(directRoute);
		
		return Util.getShortestRoute(availableRoutes);
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		eServiceGraph.saveToXML(os);
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
		eServiceGraph.readFromXML(is);
	}

	@Override
	public Set<ServiceDistance> getRemoteConnectedServices(final Service service) {
		final Set<ServiceDistance> connectedServices = new HashSet<ServiceDistance>();

		for (final ServiceDistance sDistance : getSuccessors(service))
			if (!isLocal(sDistance.getService()))
				connectedServices.add(sDistance);
		
		for (final ServiceDistance sDistance : getAncestors(service))
			if (!isLocal(sDistance.getService()))
				connectedServices.add(sDistance);

		return connectedServices;
	}

	// removes all remote services which are not connected to local services
	private Set<ServiceDistance> removeNonLocalDisconnectedServices() {
		Set<ServiceDistance> removedRemotedServices = new HashSet<ServiceDistance>();
		for (final Service s : eServiceGraph.getServices()) {
			if (!isLocal(s) && eServiceGraph.isDisconnected(s)) {
				eServiceGraph.removeService(s);
				removedRemotedServices.add(new ServiceDistance(s, getDistance(s.getPeerID())));
			}
		}
		return removedRemotedServices;
	}

	@Override
	public boolean hasService(final Service service) {
		return getService(service.getID()) != null;
	}

	@Override
	public Set<ServiceDistance> getLocalAncestors(final Service remoteService) {
		final Set<ServiceDistance> localAncestors = new HashSet<ServiceDistance>();
		
		final ServiceNode serviceNode = eServiceGraph.getServiceNode(remoteService); 
		for (final ServiceNode ancestor : eServiceGraph.getAncestors(serviceNode, false)) {
			if (isLocal(ancestor.getService()))
				localAncestors.add(new ServiceDistance(ancestor.getService(), getDistance(ancestor.getService().getPeerID())));
		}
		
		return localAncestors;
	}

	@Override
	public Set<ServiceDistance> getLocalSuccessors(final Service remoteService) {
		Set<ServiceDistance> localSuccesors = new HashSet<ServiceDistance>();
		
		final ServiceNode serviceNode = eServiceGraph.getServiceNode(remoteService);
		for (final ServiceNode succesor : eServiceGraph.getSuccessors(serviceNode, false)) {
			if (isLocal(succesor.getService()))
				localSuccesors.add(new ServiceDistance(succesor.getService(), getDistance(succesor.getService().getPeerID())));
		}
		
		return localSuccesors;
	}
}
