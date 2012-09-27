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

package graphsearch.shortestpathnotificator;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;
import graphsearch.bidirectionalsearch.Util;
import graphsearch.util.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import peer.peerid.PeerID;
import taxonomy.Taxonomy;

public class ShortestPathCalculator {

	public static List<Service> findShortestPath(final Map<Service, Set<ServiceDistance>> distanceBetweenServices, final PeerID currentPeer, final Taxonomy taxonomy) {
		// get all services located in the current peer
		final Set<Service> localServices = new HashSet<Service>();
		final Set<Service> services = Util.getAllServices(distanceBetweenServices);
		for (final Service service : services)
			if (service.getPeerID().equals(currentPeer))
				localServices.add(service);

		// for each service located in the current node, find its shortest path
		// to composition starting node
		final List<Path> possiblePaths = new ArrayList<Path>();
		for (final Service service : localServices) {
			final Path path = findShortestPath(distanceBetweenServices, service, taxonomy);
			possiblePaths.add(path);
		}

		if (!possiblePaths.isEmpty()) {
			// find the shortest path
			Collections.sort(possiblePaths);
			return possiblePaths.get(0).serviceList;
		}

		return Collections.emptyList();
	}

	static class Path implements Comparable<Path> {

		private final List<Service> serviceList = new ArrayList<Service>();
		private final double cost;

		public Path(final List<Service> path, final double cost) {
			this.serviceList.addAll(path);
			this.cost = cost;
		}

		@Override
		public boolean equals(final Object o) {
			if (!(o instanceof Path))
				return false;

			final Path path = (Path) o;
			return this.serviceList.equals(path.serviceList) && this.cost == path.cost;
		}

		@Override
		public int hashCode() {
			int result = 17;

			result = 31 * result + serviceList.hashCode();
			result = (int) (31 * result + cost);

			return result;
		}

		@Override
		public int compareTo(final Path path) {
			return (int) (this.cost - path.cost);
		}
	}

	private static Path findShortestPath(final Map<Service, Set<ServiceDistance>> distanceBetweenServices, final Service service, final Taxonomy taxonomy) {
		final ExtendedServiceGraph eServiceGraph = new ExtendedServiceGraph(taxonomy);

		ServiceNode init = null;
		ServiceNode goal = null;

		for (final Service s : Util.getAllServices(distanceBetweenServices)) {
			eServiceGraph.merge(s);

			if (Utility.isINITService(s))
				init = eServiceGraph.getServiceNode(s);

			if (Utility.isGoalService(s))
				goal = eServiceGraph.getServiceNode(s);
		}

		// set distance weight
		for (final Entry<Service, Set<ServiceDistance>> entry : distanceBetweenServices.entrySet()) {
			final Service s = entry.getKey();
			for (final ServiceDistance sDistance : entry.getValue()) {
				final ServiceNode serviceNode = eServiceGraph.getServiceNode(s);
				final ServiceNode successorNode = eServiceGraph.getServiceNode(sDistance.getService());
				eServiceGraph.setWeight(serviceNode, successorNode, sDistance.getDistance().intValue());
			}
		}

		// Find direct path. INIT -> Service
		final List<ServiceNode> directPath = eServiceGraph.findShortestPath(init, eServiceGraph.getServiceNode(service));
		final double directPathCost = eServiceGraph.getShortestPathCost(init, eServiceGraph.getServiceNode(service));

		// Find inverse path. GOAL -> Service
		final List<ServiceNode> inversePath = eServiceGraph.findShortestPath(eServiceGraph.getServiceNode(service), goal);
		final double inversePathCost = eServiceGraph.getShortestPathCost(eServiceGraph.getServiceNode(service), goal);

		// select the shortest path among the different options
		List<ServiceNode> shortestPath;
		double cost;
		if (directPathCost < inversePathCost) {
			Collections.reverse(directPath);
			shortestPath = directPath;
			cost = directPathCost;
		} else {
			shortestPath = inversePath;
			cost = inversePathCost;
		}

		// return solution
		final List<Service> services = new ArrayList<Service>();
		for (final ServiceNode sNode : shortestPath)
			services.add(sNode.getService());

		return new Path(services, cost);
	}
}
