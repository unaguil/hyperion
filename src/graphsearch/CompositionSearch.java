package graphsearch;

import graphcreation.services.Service;
import graphcreation.services.ServiceList;
import graphsearch.compositionData.localSearchesTable.SearchExpiredListener;
import taxonomy.Taxonomy;

public interface CompositionSearch extends SearchExpiredListener {

	public SearchID startComposition(Service searchedService);

	public void manageLocalServices(ServiceList addedServices, ServiceList removedServices);

	public Service getService(String serviceID);
	
	public Taxonomy getTaxonomy();

	public SearchID prepareComposition(Service searchedService);
}