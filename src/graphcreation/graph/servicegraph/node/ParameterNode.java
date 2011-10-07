package graphcreation.graph.servicegraph.node;

import graphcreation.graph.andorgraph.node.GraphNode;
import graphcreation.graph.andorgraph.node.ORNode;
import taxonomy.parameter.Parameter;

public class ParameterNode implements ORNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5260804846383993076L;
	private final Parameter p;

	private final String parameterID;

	public ParameterNode(final Parameter p) {
		this.parameterID = p.toString();
		this.p = p;
	}

	public ParameterNode(final String parameterID) {
		this.parameterID = parameterID;
		this.p = null;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ParameterNode))
			return false;
		final ParameterNode attrib = (ParameterNode) o;
		return attrib.getNodeID().equals(this.getNodeID());
	}

	@Override
	public int hashCode() {
		return parameterID.hashCode();
	}

	@Override
	public String getNodeID() {
		return parameterID;
	}

	@Override
	public String toString() {
		return "¿" + this.getNodeID() + "?";
	}

	@Override
	public GraphNode copy() {
		return new ParameterNode(this.p);
	}

	public Parameter getParam() {
		return p;
	}
}
