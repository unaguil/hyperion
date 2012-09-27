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
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package graphcreation.graph.andorgraph.edge;

import org.jgrapht.graph.DefaultWeightedEdge;

public class EqualsEdge extends DefaultWeightedEdge {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof EqualsEdge))
			return false;

		final EqualsEdge equalsEdge = (EqualsEdge) o;
		return this.getSource().equals(equalsEdge.getSource()) && this.getTarget().equals(equalsEdge.getTarget());
	}

	@Override
	public Object getTarget() {
		return super.getTarget();
	}

	@Override
	public Object getSource() {
		return super.getSource();
	}

	@Override
	public int hashCode() {
		int result = 17;

		if (this.getSource() != null && this.getTarget() != null) {
			result = 31 * result + this.getSource().hashCode();
			result = 31 * result + this.getTarget().hashCode();
		}

		return result;
	}
}
