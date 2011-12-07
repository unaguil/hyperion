package graphcreation.graph.extendedServiceGraph;

import graphcreation.graph.andorgraph.ANDORGraph;
import graphcreation.graph.andorgraph.edge.EqualsEdge;
import graphcreation.graph.andorgraph.node.GraphNode;
import graphcreation.graph.extendedServiceGraph.node.ConnectionNode;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.IntegerEdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import serialization.xml.XMLSerializable;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.ParameterFactory;

public class ExtendedServiceGraph extends ANDORGraph<ServiceNode, ConnectionNode> implements XMLSerializable {

	private final static String NODE_TAG = "node";
	private final static String EDGE_TAG = "edge";
	private final static String ID_ATTRIB = "id";
	private final static String SOURCE_ATTRIB = "source";
	private final static String TARGET_ATTRIB = "target";

	private final static String CONNECTION_NODE_MARK = "Connection:";
	private final static String SERVICE_NODE_MARK = "Service:";
	private final static String INVALID_NODE_MARK = "INVALID_NODE";

	private final Map<Service, ServiceNode> serviceNodeMap = new HashMap<Service, ServiceNode>();

	// taxonomy used for input/output connection
	private final Taxonomy taxonomy;

	public ExtendedServiceGraph(final Taxonomy taxonomy) {
		this.taxonomy = taxonomy;
	}

	public Taxonomy getTaxonomy() {
		return taxonomy;
	}

	public void merge(final ExtendedServiceGraph eServiceGraph) {
		for (final Service service : eServiceGraph.serviceNodeMap.keySet())
			this.merge(service);
	}

	public ServiceNode merge(final Service s) {
		final ServiceNode sNode = new ServiceNode(s);
		this.addNode(sNode);

		for (final InputParameter input : s.getInputParams()) {
			// create all compatible connections
			final Map<ConnectionNode, Set<Service>> connections = getCompatibleConnections(input);
			for (final Entry<ConnectionNode, Set<Service>> entry : connections.entrySet()) {
				final ConnectionNode connectionNode = entry.getKey();
				// only new connections are added
				if (!orNodeSet().contains(connectionNode)) {
					addNode(connectionNode);
					// connect other services to node
					for (final Service otherService : entry.getValue())
						addEdge(getServiceNode(otherService), connectionNode);
				}

				// connect node to service
				addEdge(connectionNode, sNode);
			}
		}

		for (final OutputParameter output : s.getOutputParams()) {
			// create all compatible connections
			final Map<ConnectionNode, Set<Service>> connections = getCompatibleConnections(output);
			for (final Entry<ConnectionNode, Set<Service>> entry : connections.entrySet()) {
				final ConnectionNode connectionNode = entry.getKey();
				if (!orNodeSet().contains(connectionNode)) {
					addNode(connectionNode);
					// connect node to other services
					for (final Service otherService : entry.getValue())
						addEdge(connectionNode, getServiceNode(otherService));
				}

				// connect service to node
				addEdge(sNode, connectionNode);
			}
		}

		serviceNodeMap.put(s, sNode);

		return sNode;
	}

	// the method searches those connections which are compatible and are
	// disconnected. If a compatible connection which is connected is found a
	// new object is created
	private Map<ConnectionNode, Set<Service>> getCompatibleConnections(final InputParameter input) {
		final Map<ConnectionNode, Set<Service>> connections = new HashMap<ConnectionNode, Set<Service>>();
		for (final Service service : serviceNodeMap.keySet())
			for (final OutputParameter output : service.getOutputParams())
				if (taxonomy.subsumes(input.getID(), output.getID()))
					put(connections, new ConnectionNode(output, input), service);
		return connections;
	}

	private void put(final Map<ConnectionNode, Set<Service>> connections, final ConnectionNode connectionNode, final Service service) {
		if (!connections.containsKey(connectionNode))
			connections.put(connectionNode, new HashSet<Service>());
		connections.get(connectionNode).add(service);
	}

	// the method searches those connections which are compatible and are
	// disconnected. If a compatible connection which is connected is found a
	// new object is created
	private Map<ConnectionNode, Set<Service>> getCompatibleConnections(final OutputParameter output) {
		final Map<ConnectionNode, Set<Service>> connections = new HashMap<ConnectionNode, Set<Service>>();
		for (final Service service : serviceNodeMap.keySet())
			for (final InputParameter input : service.getInputParams())
				if (taxonomy.subsumes(input.getID(), output.getID()))
					put(connections, new ConnectionNode(output, input), service);
		return connections;
	}

	public boolean removeService(final Service service) {
		final ServiceNode sNode = serviceNodeMap.remove(service);
		if (sNode != null) {
			this.remove(sNode, RemoveType.DisconnectedOrNodes);
			return true;
		}
		return false;
	}

	@Override
	public ExtendedServiceGraph copy() {
		final ExtendedServiceGraph copy = new ExtendedServiceGraph(this.taxonomy);

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
		if (!(o instanceof ExtendedServiceGraph))
			return false;
		final ExtendedServiceGraph extendedServiceGraph = (ExtendedServiceGraph) o;
		return super.equals(extendedServiceGraph);
	}

	public List<Service> getServices() {
		final List<Service> services = new ArrayList<Service>();
		for (final ServiceNode serviceNode : this.andNodeSet())
			services.add(serviceNode.getService());
		return services;
	}

	private static class GraphNodeNameProvider implements VertexNameProvider<GraphNode> {

		@Override
		public String getVertexName(final GraphNode node) {
			if (node instanceof ServiceNode)
				return SERVICE_NODE_MARK + node.getNodeID();
			if (node instanceof ConnectionNode)
				return CONNECTION_NODE_MARK + node.getNodeID();
			return INVALID_NODE_MARK;
		}
	}
	
	private static class DOTNodeNameProvider extends GraphNodeNameProvider {

		@Override
		public String getVertexName(final GraphNode node) {
			String str = super.getVertexName(node).replace(':', '_');
			str = str.replace('-', '_');
			return str.replace('*', '_');
		}
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		final GraphMLExporter<GraphNode, EqualsEdge> exporter = new GraphMLExporter<GraphNode, EqualsEdge>(new GraphNodeNameProvider(), new GraphNodeNameProvider(), new IntegerEdgeNameProvider<EqualsEdge>(), new IntegerEdgeNameProvider<EqualsEdge>());

		final ExtendedServiceGraph copy = new ExtendedServiceGraph(taxonomy);
		for (final Service service : serviceNodeMap.keySet())
			// Only add non disconnected services
			if (!isDisconnected(service))
				copy.merge(service);

		try {
			exporter.export(new OutputStreamWriter(os), copy.getGraph());
		} catch (final SAXException saxe) {
			throw new IOException(saxe);
		} catch (final TransformerConfigurationException tce) {
			throw new IOException(tce);
		}
	}
	
	public void saveToDOT(final OutputStream os) {
		final DOTExporter<GraphNode, EqualsEdge> exporter = new DOTExporter<GraphNode, EqualsEdge>(new DOTNodeNameProvider(), new DOTNodeNameProvider(), new IntegerEdgeNameProvider<EqualsEdge>());

		final ExtendedServiceGraph copy = new ExtendedServiceGraph(taxonomy);
		for (final Service service : serviceNodeMap.keySet())
			// Only add non disconnected services
			if (!isDisconnected(service))
				copy.merge(service);
		
		exporter.export(new OutputStreamWriter(os), copy.getGraph());
	}

	private ConnectionNode createConnectionNode(final String fullID) throws InvalidParameterIDException {
		final String connectionID = fullID.substring(CONNECTION_NODE_MARK.length(), fullID.length());

		final int index = connectionID.indexOf("*");
		final String outputID = connectionID.substring(0, index);
		final String inputID = connectionID.substring(index + 1, connectionID.length());

		return new ConnectionNode((OutputParameter) ParameterFactory.createParameter(outputID), (InputParameter) ParameterFactory.createParameter(inputID));
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
				if (id.contains(CONNECTION_NODE_MARK))
					try {
						final ConnectionNode node = createConnectionNode(id);
						addNode(node);
						idNodeMap.put(id, node);
					} catch (final InvalidParameterIDException ipe) {
						throw new IOException(ipe);
					}
				else if (id.contains(SERVICE_NODE_MARK)) {
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

			if (sourceNode instanceof ServiceNode && targetNode instanceof ConnectionNode)
				addEdge((ServiceNode) sourceNode, (ConnectionNode) targetNode);
			else if (sourceNode instanceof ConnectionNode && targetNode instanceof ServiceNode)
				addEdge((ConnectionNode) sourceNode, (ServiceNode) targetNode);
		}

	}
}
