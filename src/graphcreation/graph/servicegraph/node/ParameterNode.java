package graphcreation.graph.servicegraph.node;

import graphcreation.graph.andorgraph.node.GraphNode;
import graphcreation.graph.andorgraph.node.ORNode;
import taxonomy.Taxonomy;
import taxonomy.parameter.Parameter;

public class ParameterNode implements ORNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5260804846383993076L;
	private final Parameter p;

	public ParameterNode(final Parameter p) {
		this.p = p;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ParameterNode))
			return false;
		final ParameterNode pNode = (ParameterNode) o;
		return pNode.getNodeID().equals(this.getNodeID());
	}

	@Override
	public int hashCode() {
		return p.hashCode();
	}

	public String pretty(final Taxonomy taxonomy) {
		return p.pretty(taxonomy);
	}

	@Override
	public GraphNode copy() {
		return new ParameterNode(this.p);
	}

	public Parameter getParam() {
		return p;
	}

	@Override
	public String getNodeID() {
		return "" + p.getID();
	}
}
