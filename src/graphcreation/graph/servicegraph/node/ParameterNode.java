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
