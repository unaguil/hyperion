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

package graphcreation.graph.servicegraph;

import graphcreation.graph.andorgraph.ANDORGraph;
import graphcreation.graph.andorgraph.edge.EqualsEdge;
import graphcreation.graph.andorgraph.node.GraphNode;
import graphcreation.graph.servicegraph.node.ParameterNode;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.IntegerEdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import serialization.xml.XMLSerializable;
import taxonomy.Taxonomy;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class ServiceGraph extends ANDORGraph<ServiceNode, ParameterNode> implements XMLSerializable {

	private final static String NODE_TAG = "node";
	private final static String EDGE_TAG = "edge";
	private final static String ID_ATTRIB = "id";
	private final static String SOURCE_ATTRIB = "source";
	private final static String TARGET_ATTRIB = "target";

	private final static String PARAMETER_NODE_MARK = "Parameter:";
	private final static String SERVICE_NODE_MARK = "Service:";

	private final Map<Service, ServiceNode> serviceNodeMap = new HashMap<Service, ServiceNode>();
	
	private final Taxonomy taxonomy;
	
	public ServiceGraph(final Taxonomy taxonomy) {
		this.taxonomy = taxonomy;
	}

	protected static ServiceGraph createServiceGraphFromService(final Service s, final Taxonomy taxonomy) {
		final ServiceGraph g = new ServiceGraph(taxonomy);

		final ServiceNode sNode = new ServiceNode(s);
		g.addNode(sNode);

		for (final Parameter i : s.getInputParams()) {
			final ParameterNode attribNode = new ParameterNode(i);
			g.addNode(attribNode);
			g.addEdge(attribNode, sNode);
		}

		for (final Parameter o : s.getOutputParams()) {
			final ParameterNode attribNode = new ParameterNode(o);
			g.addNode(attribNode);
			g.addEdge(sNode, attribNode);
		}
		return g;
	}

	public List<ServiceNode> merge(final Collection<Service> services) {
		final List<ServiceNode> serviceNodes = new ArrayList<ServiceNode>();
		for (final Service s : services)
			serviceNodes.add(merge(s));
		return serviceNodes;
	}

	public ServiceNode merge(final Service s) {
		final ServiceGraph serviceGraph = createServiceGraphFromService(s, taxonomy);
		this.merge(serviceGraph);

		final ServiceNode sNode = serviceGraph.andNodeSet().iterator().next();

		serviceNodeMap.put(s, sNode);

		return sNode;
	}

	public boolean removeService(final Service service, final RemoveType removeType) {
		final ServiceNode sNode = serviceNodeMap.remove(service);
		if (sNode != null) {
			this.remove(sNode, removeType);
			return true;
		}
		return false;
	}

	@Override
	public ServiceGraph copy() {
		final ServiceGraph copy = new ServiceGraph(taxonomy);

		copy.merge(this);

		return copy;
	}

	public ServiceNode getServiceNode(final Service service) {
		return serviceNodeMap.get(service);
	}

	public Service getService(final String serviceID) {
		for (final Service service : serviceNodeMap.keySet())
			if (service.getID().equals(serviceID))
				return service;

		return null;
	}

	public boolean isDisconnected(final Service s) {
		final ServiceNode sNode = getServiceNode(s);
		return getAncestors(sNode, false).isEmpty() && getSuccessors(sNode, false).isEmpty();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ServiceGraph))
			return false;
		final ServiceGraph serviceGraph = (ServiceGraph) o;
		return super.equals(serviceGraph);
	}

	public List<Service> getServices() {
		final List<Service> services = new ArrayList<Service>();
		for (final ServiceNode serviceNode : this.andNodeSet())
			services.add(serviceNode.getService());
		return services;
	}

	private static class GraphNodeNameProvider implements VertexNameProvider<GraphNode> {
		
		private final Taxonomy taxonomy;
		
		public GraphNodeNameProvider(final Taxonomy taxonomy) {
			this.taxonomy = taxonomy;
		}

		@Override
		public String getVertexName(final GraphNode node) {
			if (node instanceof ServiceNode)
				return "Service:" + node.getNodeID();
			if (node instanceof ParameterNode) {
				ParameterNode pNode = (ParameterNode)node;
				return "Parameter:" + pNode.pretty(taxonomy);
			}
			return "INVALID_NODE";
		}
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		final GraphMLExporter<GraphNode, EqualsEdge> exporter = new GraphMLExporter<GraphNode, EqualsEdge>(new GraphNodeNameProvider(taxonomy), new GraphNodeNameProvider(taxonomy), new IntegerEdgeNameProvider<EqualsEdge>(), new IntegerEdgeNameProvider<EqualsEdge>());
		try {
			exporter.export(new OutputStreamWriter(os), this.getGraph());
		} catch (final SAXException saxe) {
			throw new IOException(saxe);
		} catch (final TransformerConfigurationException tce) {
			throw new IOException(tce);
		}
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
		final Map<String, GraphNode> idNodeMap = new HashMap<String, GraphNode>();

		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		Document document = null;
		try {
			document = docBuilder.parse(is);
		} catch (final SAXException e) {
			throw new IOException(e);
		}

		final NodeList nodeEntries = document.getElementsByTagName(NODE_TAG);
		for (int i = 0; i < nodeEntries.getLength(); i++) {
			final Element nodeElement = (Element) nodeEntries.item(i);
			final String id = nodeElement.getAttribute(ID_ATTRIB);

			if (!idNodeMap.containsKey(id))
				if (id.contains(PARAMETER_NODE_MARK)) {
					try {
						final String nodeID = id.substring(PARAMETER_NODE_MARK.length(), id.length());
						final ParameterNode node = new ParameterNode(ParameterFactory.createParameter(nodeID, taxonomy));
						addNode(node);
						idNodeMap.put(id, node);
					} catch (InvalidParameterIDException ipide) {
						throw new IOException(ipide);
					}
				} else if (id.contains(SERVICE_NODE_MARK)) {
					final ServiceNode node = new ServiceNode(id.substring(SERVICE_NODE_MARK.length(), id.length()));
					addNode(node);
					idNodeMap.put(id, node);
				}
		}

		final NodeList edgeEntries = document.getElementsByTagName(EDGE_TAG);
		for (int i = 0; i < edgeEntries.getLength(); i++) {
			final Element edgeElement = (Element) edgeEntries.item(i);
			final String sourceID = edgeElement.getAttribute(SOURCE_ATTRIB);
			final String targetID = edgeElement.getAttribute(TARGET_ATTRIB);

			final GraphNode sourceNode = idNodeMap.get(sourceID);
			final GraphNode targetNode = idNodeMap.get(targetID);

			if (sourceNode instanceof ServiceNode && targetNode instanceof ParameterNode)
				addEdge((ServiceNode) sourceNode, (ParameterNode) targetNode);
			else if (sourceNode instanceof ParameterNode && targetNode instanceof ServiceNode)
				addEdge((ParameterNode) sourceNode, (ServiceNode) targetNode);
		}
	}
}
