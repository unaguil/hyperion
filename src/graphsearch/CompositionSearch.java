package graphsearch;

import graphcreation.services.Service;
import graphcreation.services.ServiceList;

public interface CompositionSearch {

	public SearchID startComposition(Service searchedService);

	public void manageLocalServices(ServiceList addedServices, ServiceList removedServices);

	public Service getService(String serviceID);

	public boolean isRunningSearch(SearchID searchID);

	public void disableMulticastLayer();
}