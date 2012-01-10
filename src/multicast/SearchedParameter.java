package multicast;

import taxonomy.parameter.Parameter;

public class SearchedParameter {

	private final Parameter p;
	private final int maxTTL;
	
	public SearchedParameter(final Parameter p, final int maxTTL) {
		this.p = p;
		this.maxTTL = maxTTL;
	}

	public Parameter getParameter() {
		return p;
	}

	public int getMaxTTL() {
		return maxTTL;
	}
	
	@Override
	public String toString() {
		return p.toString();
	}
}
