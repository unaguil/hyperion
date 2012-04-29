package floodsearch;

import graphcreation.services.Service;
import graphsearch.SearchID;

public interface InitCompositionListener {

	public abstract void initFComposition(final Service initService, final SearchID searchID);

}