package graphcreation.graph.extendedServiceGraph.node;

import graphcreation.graph.andorgraph.node.GraphNode;
import graphcreation.graph.andorgraph.node.ORNode;
import graphcreation.graph.servicegraph.node.ParameterNode;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;

public class ConnectionNode implements ORNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final ParameterNode output;
	private final ParameterNode input;

	private final String parameterID;

	public ConnectionNode(final OutputParameter output, final InputParameter input) {
		this.output = new ParameterNode(output);
		this.input = new ParameterNode(input);
		this.parameterID = createID(output.toString(), input.toString());
	}

	private String createID(final String outputID, final String inputID) {
		return outputID + "*" + inputID;
	}

	public InputParameter getInput() {
		return (InputParameter) input.getParam();
	}

	public OutputParameter getOutput() {
		return (OutputParameter) output.getParam();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ConnectionNode))
			return false;
		final ConnectionNode attrib = (ConnectionNode) o;
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
		return new ConnectionNode(this.getOutput(), this.getInput());
	}
}
