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

package graphcreation.graph.andorgraph;

import graphcreation.graph.andorgraph.edge.EqualsEdge;
import graphcreation.graph.andorgraph.exception.SolutionFindingException;
import graphcreation.graph.andorgraph.node.ANDNode;
import graphcreation.graph.andorgraph.node.ANDNodeFactory;
import graphcreation.graph.andorgraph.node.ANDNodeSet;
import graphcreation.graph.andorgraph.node.GraphNode;
import graphcreation.graph.andorgraph.node.ORNode;
import graphcreation.util.PowerSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.AsWeightedGraph;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.MaskFunctor;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.jgrapht.traverse.DepthFirstIterator;

public class ANDORGraph<A extends ANDNode, O extends ORNode> {

	static class MaskByEnable implements MaskFunctor<GraphNode, EqualsEdge> {

		@Override
		public boolean isEdgeMasked(final EqualsEdge e) {
			return false;
		}

		@Override
		public boolean isVertexMasked(final GraphNode node) {
			if (node instanceof ANDNode) {
				final ANDNode andNode = (ANDNode) node;
				return !andNode.isEnabled();
			}
			return false;
		}

	}

	static class VirtualGraphIterator<A extends ANDNode, O extends ORNode, S extends ANDNodeSet<A>> extends BreadthFirstIterator<A, EqualsEdge> {

		private final DirectedGraph<A, EqualsEdge> virtualGraph;
		private final ANDORGraph<A, O> g;
		private final ANDNodeFactory<A, S> factory;

		public VirtualGraphIterator(final DirectedGraph<A, EqualsEdge> virtualGraph, final ANDORGraph<A, O> g, final A startNode, final ANDNodeFactory<A, S> factory) {
			super(new AsUndirectedGraph<A, EqualsEdge>(virtualGraph), startNode);
			this.virtualGraph = virtualGraph;
			this.g = g;
			this.factory = factory;
		}

		@Override
		protected void encounterVertex(final A s, final EqualsEdge e) {
			super.encounterVertex(s, e);
			final Set<A> ancestors = getFullCoverSets(s);
			for (final A a : ancestors) {
				virtualGraph.addVertex(a);
				virtualGraph.addEdge(a, s);
			}
		}

		@SuppressWarnings("unchecked")
		protected Set<A> getFullCoverSets(final A covered) {
			final Set<A> fullyCoversSet = new HashSet<A>();
			final Set<A> ancestors = new HashSet<A>(g.getAncestors(covered, true));
			final Set<Set<A>> powerSet = PowerSet.powersetAsc(ancestors);
			for (final Set<A> set : powerSet) {
				A andNode;
				if (set.size() == 1)
					andNode = set.iterator().next();
				else
					andNode = (A) factory.create(set);
				if (g.fullCover(andNode, covered))
					fullyCoversSet.add(andNode);
			}
			return fullyCoversSet;
		}
	}

	private final DirectedGraph<GraphNode, EqualsEdge> g;

	private final AsWeightedGraph<GraphNode, EqualsEdge> weightedGraph;

	transient private final DirectedMaskSubgraph<GraphNode, EqualsEdge> maskedGraph;

	private final Set<A> andNodes = new HashSet<A>();
	private final Set<O> orNodes = new HashSet<O>();

	private final Set<A> notifySuccessors = new HashSet<A>();
	private final Set<A> notifyAncestors = new HashSet<A>();

	private boolean initialized = false;

	public ANDORGraph() {
		g = new SimpleDirectedGraph<GraphNode, EqualsEdge>(EqualsEdge.class);
		maskedGraph = new DirectedMaskSubgraph<GraphNode, EqualsEdge>(g, new MaskByEnable());
		weightedGraph = new AsWeightedGraph<GraphNode, EqualsEdge>(g, new HashMap<EqualsEdge, Double>());
	}

	public void init() {
		initialized = true;
		calculateCoveredGraph();
	}

	public boolean isInitialized() {
		return initialized;
	}

	public Set<A> andNodeSet() {
		return andNodes;
	}

	public Set<O> orNodeSet() {
		return orNodes;
	}

	protected boolean addNode(final A andNode) {
		if (g.addVertex(andNode)) {
			andNodes.add(andNode);
			return true;
		}
		return false;
	}

	protected boolean addNode(final O orNode) {
		if (g.addVertex(orNode)) {
			orNodes.add(orNode);
			return true;
		}
		return false;
	}

	public EqualsEdge addEdge(final A andNode, final O orNode) {
		final EqualsEdge e = g.addEdge(andNode, orNode);
		weightedGraph.setEdgeWeight(e, 1);
		return e;
	}

	public EqualsEdge addEdge(final O orNode, final A andNode) {
		final EqualsEdge e = g.addEdge(orNode, andNode);
		weightedGraph.setEdgeWeight(e, 0);
		return e;
	}

	protected DirectedGraph<GraphNode, EqualsEdge> getGraph() {
		return g;
	}

	protected DirectedGraph<GraphNode, EqualsEdge> getMaskedGraph() {
		return maskedGraph;
	}

	@SuppressWarnings("unchecked")
	protected void merge(final ANDORGraph<A, O> graphB) {
		final Set<EqualsEdge> edgesB = graphB.getGraph().edgeSet();

		final Set<A> addedAndNodes = new HashSet<A>();

		for (final A a : graphB.andNodeSet()) {
			final A andNode = (A) a.copy();
			this.addNode(andNode);
			addedAndNodes.add(andNode);
		}

		for (final O o : graphB.orNodeSet())
			this.addNode((O) o.copy());

		for (final EqualsEdge e : edgesB) {
			final GraphNode source = g.getEdgeSource(e);
			final GraphNode target = g.getEdgeTarget(e);

			if (source instanceof ANDNode && target instanceof ORNode)
				this.addEdge((A) source, (O) target);
			else
				this.addEdge((O) source, (A) target);
		}
	}

	public enum RemoveType {
		Normal, SharedORNodes, DisconnectedOrNodes
	}

	public void remove(final A andNode, final RemoveType removeType) {
		final Set<O> relatedOrNodes = getAllORNodes(andNode, false);

		g.removeAllEdges(g.edgesOf(andNode));

		for (final O orNode : relatedOrNodes)
			if ((removeType.equals(RemoveType.Normal) && g.edgesOf(orNode).isEmpty()) || removeType.equals(RemoveType.SharedORNodes) || (removeType.equals(RemoveType.DisconnectedOrNodes) && isDisconnected(orNode))) {
				g.removeAllEdges(g.edgesOf(orNode));
				g.removeVertex(orNode);
				orNodes.remove(orNode);
			}

		g.removeVertex(andNode);
		andNodes.remove(andNode);

		// remove notifications
		removeNotifications(andNode);
	}

	private boolean isDisconnected(final O orNode) {
		return g.incomingEdgesOf(orNode).isEmpty() || g.outgoingEdgesOf(orNode).isEmpty();
	}

	public boolean isDisconnected(final A andNode) {
		return getAncestors(andNode, false).isEmpty() && getSuccessors(andNode, false).isEmpty();
	}

	public long getCoveredNodes() {
		long i = 0;
		for (final A andNode : andNodes)
			if (andNode.isEnabled())
				i++;
		return i;
	}

	private void printGraphStatistics() {
		final Set<GraphNode> vertices = g.vertexSet();
		final Set<EqualsEdge> edges = g.edgeSet();
		System.out.println("Vertices: " + vertices.size());
		System.out.println("Edges: " + edges.size());
	}

	public void printANDORGraphStatistics() {
		System.out.println("ANDOR Graph Statistics");
		System.out.println("*************************");
		printGraphStatistics();
		System.out.println("ANDNodes: " + andNodeSet().size());
		System.out.println("ORNodes: " + orNodeSet().size());
		System.out.println("Covered ANDNodes: " + getCoveredNodes());
		System.out.println("*************************");
		System.out.println();
	}

	@SuppressWarnings("unchecked")
	public Set<O> getAncestorORNodes(final A andNode, final boolean mask) {
		if (andNode instanceof ANDNodeSet<?>)
			return getSetAncestorORNodes((ANDNodeSet<A>) andNode, mask);

		final DirectedGraph<GraphNode, EqualsEdge> graph = (mask ? this.maskedGraph : this.g);
		final Set<O> ancestorORNodes = new HashSet<O>();
		for (final EqualsEdge e : graph.incomingEdgesOf(andNode))
			ancestorORNodes.add((O) graph.getEdgeSource(e));
		return ancestorORNodes;
	}

	@SuppressWarnings("unchecked")
	public Set<O> getSucessorORNodes(final A andNode, final boolean mask) {
		if (andNode instanceof ANDNodeSet<?>)
			return getSetSucessorORNodes((ANDNodeSet<A>) andNode, mask);

		final DirectedGraph<GraphNode, EqualsEdge> graph = (mask ? this.maskedGraph : this.g);
		final Set<O> succesorORNodes = new HashSet<O>();
		for (final EqualsEdge e : graph.outgoingEdgesOf(andNode))
			succesorORNodes.add((O) graph.getEdgeTarget(e));
		return succesorORNodes;
	}

	@SuppressWarnings("unchecked")
	public Set<A> getAncestors(final A andNode, final boolean mask) {
		if (andNode instanceof ANDNodeSet<?>)
			return getSetAncestors((ANDNodeSet<A>) andNode, mask);

		final DirectedGraph<GraphNode, EqualsEdge> graph = (mask ? this.maskedGraph : this.g);
		final Set<O> ancestorORNodes = getAncestorORNodes(andNode, mask);
		final Set<A> ancestors = new HashSet<A>();
		for (final O orNode : ancestorORNodes) {
			final Set<EqualsEdge> ancestorEdges = graph.incomingEdgesOf(orNode);
			for (final EqualsEdge e : ancestorEdges) {
				final A ancestor = (A) graph.getEdgeSource(e);
				ancestors.add(ancestor);
			}
		}
		return ancestors;
	}

	@SuppressWarnings("unchecked")
	public Set<A> getSuccessors(final A andNode, final boolean mask) {
		if (andNode instanceof ANDNodeSet<?>)
			return getSetSucessors((ANDNodeSet<A>) andNode, mask);

		final DirectedGraph<GraphNode, EqualsEdge> graph = (mask ? this.maskedGraph : this.g);
		final Set<O> sucessorORNodes = getSucessorORNodes(andNode, mask);
		final Set<A> sucessors = new HashSet<A>();
		for (final ORNode orNode : sucessorORNodes) {
			final Set<EqualsEdge> sucessorEdges = graph.outgoingEdgesOf(orNode);
			for (final EqualsEdge e : sucessorEdges) {
				final A succesor = (A) graph.getEdgeTarget(e);
				sucessors.add(succesor);
			}
		}
		return sucessors;
	}

	public boolean fullCover(final A cover, final A covered) {
		final Set<O> ancestorORNodes = getAncestorORNodes(covered, false);
		final Set<O> sucessorORNodes = getSucessorORNodes(cover, false);
		return sucessorORNodes.containsAll(ancestorORNodes);
	}

	public boolean isCovered(final A andNode) {
		for (final O ancestorORNode : getAncestorORNodes(andNode, false))
			if (g.incomingEdgesOf(ancestorORNode).size() == 0)
				return false;
		return true;
	}

	protected void enableSucessors(final A andNode) {
		final Set<A> successors = getSuccessors(andNode, false);
		for (final A s : successors)
			s.setEnabled(true);
	}

	public Set<O> getAllORNodes(final A andNode, final boolean mask) {
		final Set<O> allORNodes = new HashSet<O>();
		allORNodes.addAll(getAncestorORNodes(andNode, mask));
		allORNodes.addAll(getSucessorORNodes(andNode, mask));
		return allORNodes;
	}

	public Set<O> getSetAncestorORNodes(final ANDNodeSet<A> andNodeSet, final boolean mask) {
		final Set<O> ancestorORNodes = new HashSet<O>();
		for (final A s : andNodeSet.getInnerSet())
			ancestorORNodes.addAll(getAncestorORNodes(s, mask));
		return ancestorORNodes;
	}

	public Set<A> getSetAncestors(final ANDNodeSet<A> sNodeSet, final boolean mask) {
		final Set<A> ancestors = new HashSet<A>();
		for (final A s : sNodeSet.getInnerSet())
			ancestors.addAll(getAncestors(s, mask));
		return ancestors;
	}

	public Set<O> getSetSucessorORNodes(final ANDNodeSet<A> andNodeSet, final boolean mask) {
		final Set<O> succesorORNodes = new HashSet<O>();
		for (final A s : andNodeSet.getInnerSet())
			succesorORNodes.addAll(getSucessorORNodes(s, mask));
		return succesorORNodes;
	}

	public Set<A> getSetSucessors(final ANDNodeSet<A> andNodeSet, final boolean mask) {
		final Set<A> sucessors = new HashSet<A>();
		for (final A s : andNodeSet.getInnerSet())
			sucessors.addAll(getSuccessors(s, mask));
		return sucessors;
	}

	public void notifyNewSuccessors(final A andNode) {
		notifySuccessors.add(andNode);
	}

	public void notifyNewAncestors(final A andNode) {
		notifyAncestors.add(andNode);
	}

	public void removeNotifications(final A andNode) {
		notifySuccessors.remove(andNode);
		notifyAncestors.remove(andNode);
	}

	protected void calculateCoveredGraph() {
		for (final A andNode : andNodeSet())
			if (isCovered(andNode))
				andNode.setEnabled(true);
	}

	public void printPath(final A start, final A end) {
		final List<EqualsEdge> shortestPath = DijkstraShortestPath.findPathBetween(new AsUndirectedGraph<GraphNode, EqualsEdge>(g), start, end);
		if (shortestPath != null)
			for (final EqualsEdge e : shortestPath)
				System.out.println(e);
		else
			System.out.println("Path not found!");
	}

	@Override
	public String toString() {
		return g.toString();
	}

	protected <S extends ANDNodeSet<A>> DirectedGraph<A, EqualsEdge> searchSolution(final A goalNode, final A initNode, final ANDNodeFactory<A, S> factory) {
		final DirectedGraph<A, EqualsEdge> virtualGraph = new SimpleDirectedGraph<A, EqualsEdge>(EqualsEdge.class);
		virtualGraph.addVertex(goalNode);
		final VirtualGraphIterator<A, O, S> virtualIterator = new VirtualGraphIterator<A, O, S>(virtualGraph, this, goalNode, factory);
		boolean finished = false;
		while (virtualIterator.hasNext() && !finished) {
			final A node = virtualIterator.next();
			if (node.equals(initNode)) {
				finished = true;
				System.out.println("Init reached!");
				return virtualGraph;
			}
		}
		return new SimpleDirectedGraph<A, EqualsEdge>(EqualsEdge.class);
	}

	public <S extends ANDNodeSet<A>> List<A> findSolution(final A initNode, final A goalNode, final ANDNodeFactory<A, S> factory) throws SolutionFindingException {
		// Enable nodes
		initNode.setEnabled(true);
		if (!isCovered(goalNode))
			throw new SolutionFindingException("Solution cannot be found. GOAL node missing!");

		goalNode.setEnabled(true);
		System.out.println("Solution could be found someday");
		enableSucessors(initNode);
		System.out.println("ReCovered graph with init and goal: ");
		printANDORGraphStatistics();

		final DirectedGraph<A, EqualsEdge> virtualGraph = this.searchSolution(goalNode, initNode, factory);

		if (virtualGraph.vertexSet().size() == 0)
			throw new SolutionFindingException("Path not found");

		final DepthFirstIterator<A, EqualsEdge> it = new DepthFirstIterator<A, EqualsEdge>(virtualGraph, initNode);
		final List<A> solution = new ArrayList<A>();
		while (it.hasNext())
			solution.add(it.next());
		return solution;
	}

	public ANDORGraph<A, O> copy() {
		final ANDORGraph<A, O> copy = new ANDORGraph<A, O>();

		copy.merge(this);

		return copy;
	}

	public void setWeight(final A node, final A successor, final double weight) {
		final Set<O> commonOrNodes = getCommonORNodes(node, successor, false);
		for (final O orNode : commonOrNodes) {
			final EqualsEdge e = weightedGraph.getEdge(node, orNode);
			weightedGraph.setEdgeWeight(e, weight);
		}
	}

	@SuppressWarnings("unchecked")
	public List<A> findShortestPath(final A startAndNode, final A endAndNode) {
		final List<A> shortestPath = new ArrayList<A>();

		if (startAndNode.equals(endAndNode)) {
			shortestPath.add(startAndNode);
			return shortestPath;
		}

		final BellmanFordShortestPath<GraphNode, EqualsEdge> bellmanFordShortestPath = new BellmanFordShortestPath<GraphNode, EqualsEdge>(weightedGraph, startAndNode);

		final List<EqualsEdge> edgeList = bellmanFordShortestPath.getPathEdgeList(endAndNode);

		for (final EqualsEdge e : edgeList) {
			if (e.getSource() instanceof ANDNode && !shortestPath.contains(e.getSource()))
				shortestPath.add((A) e.getSource());
			if (e.getTarget() instanceof ANDNode && !shortestPath.contains(e.getTarget()))
				shortestPath.add((A) e.getTarget());
		}
		return shortestPath;
	}

	public double getShortestPathCost(final A startAndNode, final A endAndNode) {
		if (startAndNode.equals(endAndNode))
			return 0;

		final BellmanFordShortestPath<GraphNode, EqualsEdge> bellmanFordShortestPath = new BellmanFordShortestPath<GraphNode, EqualsEdge>(weightedGraph, startAndNode);

		final double pathCost = bellmanFordShortestPath.getCost(endAndNode);

		return pathCost;
	}

	public Set<O> getCommonORNodes(final A ancestor, final A sucessor, final boolean mask) {
		final Set<O> sucessorORNodes = getSucessorORNodes(ancestor, mask);
		final Set<O> ancestorORNodes = getAncestorORNodes(sucessor, mask);

		// Obtain common nodes
		final Set<O> commonORNodes = new HashSet<O>();
		commonORNodes.addAll(sucessorORNodes);
		commonORNodes.addAll(ancestorORNodes);
		return commonORNodes;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ANDORGraph<?, ?>))
			return false;
		final ANDORGraph<?, ?> andORGraph = (ANDORGraph<?, ?>) o;
		final boolean andNodesEquals = this.andNodeSet().equals(andORGraph.andNodeSet());
		final boolean orNodeEquals = this.orNodeSet().equals(andORGraph.orNodeSet());
		final boolean edgeEquals = this.g.edgeSet().equals(andORGraph.g.edgeSet());
		return andNodesEquals && orNodeEquals && edgeEquals;
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + this.andNodeSet().hashCode();
		result = 37 * result + this.orNodeSet().hashCode();

		return result;
	}
}
