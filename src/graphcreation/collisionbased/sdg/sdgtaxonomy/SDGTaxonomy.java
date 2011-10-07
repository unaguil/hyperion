package graphcreation.collisionbased.sdg.sdgtaxonomy;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.collisionbased.sdg.NonLocalServiceException;
import graphcreation.collisionbased.sdg.SDG;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.extendedServiceGraph.node.ConnectionNode;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import peer.PeerID;
import peer.message.MessageID;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

/**
 * This class manages the SDG graph.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class SDGTaxonomy implements SDG {

	// table containing which services are accessed through each search route
	// originated in a peer which detected a collision
	private final Map<PeerID, CollisionInfo> collisionInfoTable = new HashMap<PeerID, CollisionInfo>();

	// the local service dependency graph
	private final ExtendedServiceGraph eServiceGraph;

	// the id of the peer which hosts the service table
	private final PeerID peerID;

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
	public SDGTaxonomy(final PeerID peerID, final Taxonomy taxonomy) {
		this.peerID = peerID;
		this.taxonomy = taxonomy;
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
	public void connectRemoteServices(final Service localService, final Set<ServiceDistance> remoteSuccessors, final Set<ServiceDistance> remoteAncestors, final PeerID collisionNode) throws NonLocalServiceException {
		if (isLocal(localService)) {
			for (final ServiceDistance remoteAncestor : remoteAncestors) {

				// save which peer detected the collision for the remote service
				if (!collisionInfoTable.containsKey(collisionNode))
					collisionInfoTable.put(collisionNode, new CollisionInfo(peerID));
				collisionInfoTable.get(collisionNode).add(remoteAncestor);

				// add the remote ancestor to the graph
				eServiceGraph.merge(remoteAncestor.getService());

				// save the added service as a connected ancestor for the local
				// service
				collisionInfoTable.get(collisionNode).addAncestor(localService, remoteAncestor);
			}

			for (final ServiceDistance remoteSuccessor : remoteSuccessors) {
				// save which collision node detected this connections
				if (!collisionInfoTable.containsKey(collisionNode))
					collisionInfoTable.put(collisionNode, new CollisionInfo(peerID));
				collisionInfoTable.get(collisionNode).add(remoteSuccessor);

				// add the remote ancestor to the graph
				eServiceGraph.merge(remoteSuccessor.getService());

				// save the added service as a connected successor for the local
				// service
				collisionInfoTable.get(collisionNode).addSuccessor(localService, remoteSuccessor);
			}
		} else
			throw new NonLocalServiceException();
	}

	@Override
	public Set<ServiceDistance> getSuccessors(final Service service) {
		final Set<ServiceDistance> successors = new HashSet<ServiceDistance>();
		for (final CollisionInfo collisionInfo : collisionInfoTable.values())
			successors.addAll(collisionInfo.getSuccessors(service));
		return successors;
	}

	@Override
	public Set<ServiceDistance> getAncestors(final Service service) {
		final Set<ServiceDistance> ancestors = new HashSet<ServiceDistance>();
		for (final CollisionInfo collisionInfo : collisionInfoTable.values())
			ancestors.addAll(collisionInfo.getAncestors(service));
		return ancestors;
	}

	@Override
	public Set<ServiceDistance> getSuccessors(final Service service, final PeerID collisionPeerID) {
		if (collisionInfoTable.containsKey(collisionPeerID))
			return collisionInfoTable.get(collisionPeerID).getSuccessors(service);
		return Collections.emptySet();
	}

	@Override
	public Set<ServiceDistance> getAncestors(final Service service, final PeerID collisionPeerID) {
		if (collisionInfoTable.containsKey(collisionPeerID))
			return collisionInfoTable.get(collisionPeerID).getAncestors(service);
		return Collections.emptySet();
	}

	@Override
	public void removeLocalService(final Service service) {
		if (eServiceGraph.removeService(service))
			removeNonLocalDisconnectedServices();

		for (final CollisionInfo collisionInfo : collisionInfoTable.values())
			collisionInfo.removeService(service);
	}

	@Override
	public void removeServiceConnectedBy(final Service service, final PeerID collisionPeerID) {
		if (collisionInfoTable.containsKey(collisionPeerID))
			collisionInfoTable.get(collisionPeerID).removeService(service);

		final Set<Service> connectedServices = extractServices(getRemoteServices());
		if (!connectedServices.contains(service))
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

	@Override
	public void removeServicesFromRoute(final MessageID routeID) {
		if (collisionInfoTable.containsKey(routeID.getPeer())) {
			final Set<ServiceDistance> lostServices = new HashSet<ServiceDistance>(collisionInfoTable.get(routeID.getPeer()).getServices());
			
				logger.trace("Peer " + peerID + " removing remote services " + lostServices);

			collisionInfoTable.remove(routeID.getPeer());

			// get those services which are already used in other connections
			final Set<ServiceDistance> connectedServices = getRemoteServices();

			// obtain those lost services which were not present in other
			// connections
			lostServices.removeAll(connectedServices);

			for (final ServiceDistance lostService : lostServices)
				eServiceGraph.removeService(lostService.getService());

			removeNonLocalDisconnectedServices();
		}
	}

	@Override
	public Set<ServiceDistance> servicesConnectedThrough(final MessageID routeID) {
		if (collisionInfoTable.containsKey(routeID.getPeer()))
			return new HashSet<ServiceDistance>(collisionInfoTable.get(routeID.getPeer()).getServices());

		return new HashSet<ServiceDistance>();
	}

	private Set<ServiceDistance> getRemoteServices() {
		final Set<ServiceDistance> remoteServices = new HashSet<ServiceDistance>();
		for (final CollisionInfo collisionInfo : collisionInfoTable.values())
			remoteServices.addAll(collisionInfo.getServices());

		return remoteServices;
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

	private Set<Service> extractServices(final Set<ServiceDistance> serviceDistances) {
		final Set<Service> services = new HashSet<Service>();
		for (final ServiceDistance sDistance : serviceDistances)
			services.add(sDistance.getService());
		return services;
	}

	@Override
	public Set<PeerID> getThroughCollisionNodes(final Service service) {
		final Set<PeerID> collisionNodes = new HashSet<PeerID>();
		for (final PeerID collisionNode : collisionInfoTable.keySet()) {
			final Set<ServiceDistance> serviceDistances = collisionInfoTable.get(collisionNode).getServices();
			if (extractServices(serviceDistances).contains((service)))
				collisionNodes.add(collisionNode);
		}
		return collisionNodes;
	}

	@Override
	public Set<InputParameter> getConnectedInputs(final Service service, final Service ancestor) {
		final ServiceNode serviceNode = eServiceGraph.getServiceNode(service);
		final ServiceNode ancestorNode = eServiceGraph.getServiceNode(ancestor);
		final Set<ConnectionNode> connections = eServiceGraph.getAncestorORNodes(serviceNode, false);
		final Set<ConnectionNode> ancestorConnections = eServiceGraph.getSucessorORNodes(ancestorNode, false);

		connections.retainAll(ancestorConnections);

		final Set<InputParameter> connectedInputs = new HashSet<InputParameter>();
		for (final ConnectionNode connection : connections)
			connectedInputs.add(connection.getInput());

		return connectedInputs;
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

		for (final CollisionInfo collisionInfo : collisionInfoTable.values())
			connectedServices.addAll(collisionInfo.getRemoteConnectedServices(service));

		return connectedServices;
	}

	// removes all remote services which are not connected to local services
	private void removeNonLocalDisconnectedServices() {
		for (final Service s : eServiceGraph.getServices())
			if (!isLocal(s) && eServiceGraph.isDisconnected(s)) {
				eServiceGraph.removeService(s);

				// remove services from their collision node
				final Set<PeerID> collisionNodes = getThroughCollisionNodes(s);
				for (final PeerID collisionNode : collisionNodes) {
					collisionInfoTable.get(collisionNode).removeService(s);
					if (collisionInfoTable.get(collisionNode).isEmpty())
						collisionInfoTable.remove(collisionNode);
				}
			}
	}

	@Override
	public boolean hasService(final Service service) {
		return getService(service.getID()) != null;
	}

	@Override
	public Set<ServiceDistance> getLocalAncestors(final Service remoteService, final PeerID collisionPeerID) {
		if (collisionInfoTable.containsKey(collisionPeerID))
			return collisionInfoTable.get(collisionPeerID).getLocalAncestors(remoteService);

		return Collections.emptySet();
	}

	@Override
	public Set<ServiceDistance> getLocalSuccessors(final Service remoteService, final PeerID collisionPeerID) {
		if (collisionInfoTable.containsKey(collisionPeerID))
			return collisionInfoTable.get(collisionPeerID).getLocalSuccessors(remoteService);

		return Collections.emptySet();
	}

	@Override
	public Set<ServiceDistance> getLocalAncestors(final Service remoteService) {
		final Set<ServiceDistance> localAncestors = new HashSet<ServiceDistance>();
		for (final CollisionInfo collisionInfo : collisionInfoTable.values())
			localAncestors.addAll(collisionInfo.getLocalAncestors(remoteService));
		return localAncestors;
	}

	@Override
	public Set<ServiceDistance> getLocalSuccessors(final Service remoteService) {
		final Set<ServiceDistance> successors = new HashSet<ServiceDistance>();
		for (final CollisionInfo collisionInfo : collisionInfoTable.values())
			successors.addAll(collisionInfo.getLocalSuccessors(remoteService));
		return successors;
	}
}
