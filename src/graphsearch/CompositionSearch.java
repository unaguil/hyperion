package graphsearch;

import graphcreation.services.Service;
import graphcreation.services.ServiceList;
import taxonomy.Taxonomy;

public interface CompositionSearch {

	public SearchID startComposition(Service searchedService);

	public void manageLocalServices(ServiceList addedServices, ServiceList removedServices);

	public Service getService(String serviceID);

	public boolean isRunningSearch(SearchID searchID);
	
	public Taxonomy getTaxonomy();
}