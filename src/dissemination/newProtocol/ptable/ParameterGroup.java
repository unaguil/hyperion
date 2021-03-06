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

package dissemination.newProtocol.ptable;

import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

public class ParameterGroup {

	// the taxonomy used for parameter relationship calculation
	private final Taxonomy taxonomy;

	// the current representative parameter of this group. It is the more
	// general type according to the taxonomy
	private Parameter currentParameter;

	public ParameterGroup(final Parameter p, final Taxonomy taxonomy) {
		this.currentParameter = p;
		this.taxonomy = taxonomy;
	}

	public Parameter getCurrentParameter() {
		return currentParameter;
	}

	public boolean belongs(final Parameter p) {
		// check that the parameter has the same type that the current one
		// (input/output)
		if (!((isInput(currentParameter) && isInput(p)) || (isOutput(currentParameter) && isOutput(p))))
			return false;

		// check that parameters are related according to the taxonomy
		return taxonomy.areRelated(currentParameter.getID(), p.getID());
	}

	public boolean add(final Parameter p) {
		if (!belongs(p))
			return false;

		// decide if the new parameter is more general, according to the
		// taxonomy, than the previous one
		if (taxonomy.subsumes(p.getID(), currentParameter.getID()))
			currentParameter = p;

		return true;
	}

	private boolean isInput(final Parameter p) {
		return p instanceof InputParameter;
	}

	private boolean isOutput(final Parameter p) {
		return p instanceof OutputParameter;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ParameterGroup))
			return false;

		final ParameterGroup pGroup = (ParameterGroup) o;
		return this.currentParameter.equals(pGroup.currentParameter);
	}

	@Override
	public int hashCode() {
		return currentParameter.hashCode();
	}
	
	@Override
	public String toString() {
		return currentParameter.pretty(taxonomy);
	}
}
