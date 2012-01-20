package multicast.search.unicastTable;

import java.util.HashSet;
import java.util.Set;

import multicast.search.message.SearchMessage;
import taxonomy.parameter.Parameter;

public class ParametersRemovalResult {
	
	private final Set<Parameter> removedParameters = new HashSet<Parameter>();
	private final Set<SearchMessage> newActiveSearches = new HashSet<SearchMessage>();

	public ParametersRemovalResult(Set<Parameter> removedParameters, Set<SearchMessage> newActiveSearches) {
		this.removedParameters.addAll(removedParameters);
		this.newActiveSearches.addAll(newActiveSearches);
	}

	public Set<SearchMessage> getNewActiveSearches() {
		return newActiveSearches;
	}

	public Set<Parameter> getRemovedParameters() {
		return removedParameters;
	}
}
