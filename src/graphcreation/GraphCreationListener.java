package graphcreation;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.util.Map;
import java.util.Set;

public interface GraphCreationListener {

	public void newSuccessors(Map<Service, Set<ServiceDistance>> newSuccessors);

	public void lostSuccessors(Map<Service, Set<Service>> lostSuccessors);

	public void newAncestors(Map<Service, Set<ServiceDistance>> newAncestors);

	public void lostAncestors(Map<Service, Set<Service>> lostAncestors);

	public void filterConnections(Map<Service, Set<ServiceDistance>> foundRemoteSuccessors, Map<Service, Set<ServiceDistance>> foundRemoteAncestors);
}
