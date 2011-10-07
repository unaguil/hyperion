package graphsearch;

import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.services.Service;

import java.util.Set;

public interface CompositionListener {

	public void compositionFound(ExtendedServiceGraph composition, SearchID searchID);

	public void compositionTimeExpired(SearchID searchID);

	public void compositionsLost(SearchID searchID, ExtendedServiceGraph invalidComposition);

	public void compositionModified(SearchID searchID, Set<Service> removedServices);
}
