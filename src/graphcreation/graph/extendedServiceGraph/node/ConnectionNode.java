/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package graphcreation.graph.extendedServiceGraph.node;

import graphcreation.graph.andorgraph.node.GraphNode;
import graphcreation.graph.andorgraph.node.ORNode;
import graphcreation.graph.servicegraph.node.ParameterNode;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;

public class ConnectionNode implements ORNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final ParameterNode output;
	private final ParameterNode input;

	private final String nodeID;
	
	private final Taxonomy taxonomy;

	public ConnectionNode(final OutputParameter output, final InputParameter input, final Taxonomy taxonomy) {
		this.output = new ParameterNode(output);
		this.input = new ParameterNode(input);
		this.nodeID = createID(output.pretty(taxonomy), input.pretty(taxonomy));
		this.taxonomy = taxonomy;
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
		return nodeID.hashCode();
	}

	@Override
	public String getNodeID() {
		return nodeID;
	}

	@Override
	public String toString() {
		return "¿" + this.getNodeID() + "?";
	}

	@Override
	public GraphNode copy() {
		return new ConnectionNode(this.getOutput(), this.getInput(), taxonomy);
	}
}
