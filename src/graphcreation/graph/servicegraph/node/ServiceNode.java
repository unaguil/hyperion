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

import graphcreation.graph.andorgraph.node.ANDNode;
import graphcreation.graph.andorgraph.node.GraphNode;
import graphcreation.services.Service;

public class ServiceNode implements ANDNode {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Service s;
	private final String id;
	private boolean enabled = false;

	public ServiceNode(final String id) {
		s = null;
		this.id = id;
	}

	private ServiceNode(final Service s, final String id, final boolean enabled) {
		this.s = s;
		this.id = id;
		this.enabled = enabled;
	}

	public ServiceNode(final Service s) {
		this.s = s;
		this.id = s.getID();
	}

	public Service getService() {
		return s;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(final boolean enable) {
		this.enabled = enable;
	}

	@Override
	public String getNodeID() {
		return id;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ServiceNode))
			return false;
		final ServiceNode sNode = (ServiceNode) o;
		return sNode.getNodeID().equals(this.getNodeID());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		if (this.isEnabled())
			return "*<" + this.getNodeID() + ">";

		return "<" + this.getNodeID() + ">";
	}

	@Override
	public GraphNode copy() {
		return new ServiceNode(this.s, this.id, this.enabled);
	}
}
