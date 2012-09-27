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

package graphsearch.backward;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.extendedServiceGraph.node.ConnectionNode;
import graphcreation.services.Service;
import graphcreation.util.PowerSet;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;

public class CoveringSets {

	public static Set<Set<ServiceDistance>> calculateCoveringSets(final Service service, final Set<Set<ServiceDistance>> currentCoveringSets, final Set<ServiceDistance> ancestors, final Taxonomy taxonomy) {
		final Set<Set<ServiceDistance>> powerSet = PowerSet.powersetAsc(ancestors);
		// Obtain the new covering sets
		powerSet.removeAll(currentCoveringSets);

		// Check each set for covering
		final Set<Set<ServiceDistance>> newCoveringSets = new LinkedHashSet<Set<ServiceDistance>>();
		for (final Set<ServiceDistance> set : powerSet)
			// only sets which do not contain previous covering sets and which
			// fully cover the current service are added
			if (!contains(set, newCoveringSets) && !contains(set, currentCoveringSets) && covers(set, service, taxonomy))
				newCoveringSets.add(set);
		return newCoveringSets;
	}

	private static boolean contains(final Set<ServiceDistance> set, final Set<Set<ServiceDistance>> coveringSets) {
		for (final Set<ServiceDistance> coveringSet : coveringSets)
			if (set.containsAll(coveringSet))
				return true;
		return false;
	}

	private static boolean covers(final Set<ServiceDistance> set, final Service service, final Taxonomy taxonomy) {
		final ExtendedServiceGraph eServiceGraph = new ExtendedServiceGraph(taxonomy);

		for (final ServiceDistance antecessor : set)
			eServiceGraph.merge(antecessor.getService());

		eServiceGraph.merge(service);

		// check if service is covered
		final Set<InputParameter> coveredInputs = new HashSet<InputParameter>();
		for (final ConnectionNode connectionNode : eServiceGraph.getAncestorORNodes(eServiceGraph.getServiceNode(service), false))
			coveredInputs.add(connectionNode.getInput());

		// Check if all service inputs are contained in the covered inputs
		return coveredInputs.containsAll(service.getInputParams());
	}
}
