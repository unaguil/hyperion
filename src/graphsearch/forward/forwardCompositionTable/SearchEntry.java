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

package graphsearch.forward.forwardCompositionTable;

import graphcreation.GraphCreator;
import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.graph.extendedServiceGraph.node.ConnectionNode;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;
import graphsearch.forward.message.FCompositionMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import taxonomy.parameter.InputParameter;

class SearchEntry {

	// a map which holds those messages which were received for the
	// specified service
	private final Map<Service, Set<FCompositionMessage>> receivedMessagesPerService = new HashMap<Service, Set<FCompositionMessage>>();
	private final Map<Service, Map<InputParameter, Boolean>> inputsTablePerService = new HashMap<Service, Map<InputParameter, Boolean>>();
	private final Set<Service> forwardedSuccesors = new HashSet<Service>();

	private final long timestamp;
	private final long firstReceivedMessageRemainingTime;
	
	private boolean hasChanged = false;
	
	private final GraphCreator gCreator;

	public SearchEntry(final long createdTime, final GraphCreator gCreator) {
		this.firstReceivedMessageRemainingTime = createdTime;
		this.timestamp = System.currentTimeMillis();
		this.gCreator = gCreator;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getRemainingTime() {
		final long elapsedTime = System.currentTimeMillis() - this.timestamp;
		return firstReceivedMessageRemainingTime - elapsedTime;
	}

	public boolean areAllInputsCovered(final Service service) {
		for (final Boolean b : inputsTablePerService.get(service).values())
			if (!b.booleanValue())
				return false;
		return true;
	}

	public Map<InputParameter, Boolean> createInputsTable(final Service service) {
		final Map<InputParameter, Boolean> inputsTable = new HashMap<InputParameter, Boolean>();
		for (final InputParameter input : service.getInputParams())
			inputsTable.put(input, Boolean.FALSE);
		return inputsTable;
	}

	public void addReceivedMessage(final Service service, final FCompositionMessage fCompositionMessage) { 
		if (!receivedMessagesPerService.containsKey(service)) {
			receivedMessagesPerService.put(service, new HashSet<FCompositionMessage>());
			inputsTablePerService.put(service, createInputsTable(service));
		}
		
		final int priorSize = receivedMessagesPerService.get(service).size();
		receivedMessagesPerService.get(service).add(fCompositionMessage);
		
		if (priorSize < receivedMessagesPerService.get(service).size())
			hasChanged = true;

		calculateNewCovers(service, fCompositionMessage);
	}
	
	public boolean hasChanged() {
		final boolean value = hasChanged;
		hasChanged = false;
		return value;
	}
	
	private Set<InputParameter> getConnectedInputs(final Service service, final Service ancestor) {
		final ExtendedServiceGraph eServiceGraph = new ExtendedServiceGraph(gCreator.getPSearch().getDisseminationLayer().getTaxonomy());
		eServiceGraph.merge(service);
		eServiceGraph.merge(ancestor);
		
		final ServiceNode serviceNode = eServiceGraph.getServiceNode(service);
		final ServiceNode ancestorNode = eServiceGraph.getServiceNode(ancestor);
		final Set<ConnectionNode> connections = eServiceGraph.getAncestorORNodes(serviceNode, false);
		final Set<ConnectionNode> ancestorConnections = eServiceGraph.getSucessorORNodes(ancestorNode, false);

		connections.retainAll(ancestorConnections);

		final Set<InputParameter> connectedInputs = new HashSet<InputParameter>();
		for (final ConnectionNode connection : connections)
			connectedInputs.add(connection.getInput());

		return connectedInputs;
	}

	private void calculateNewCovers(final Service service, final FCompositionMessage fCompositionMessage) {
		// Obtain which parameters of the local service are covered by the
		// ancestor service
		final Service ancestor = fCompositionMessage.getSourceService();
		final Set<InputParameter> connectedInputs = getConnectedInputs(service, ancestor);

		coverInputs(service, connectedInputs);
	}

	private void calculateCovers(final Service service) {
		inputsTablePerService.put(service, createInputsTable(service));
		for (final FCompositionMessage fCompositionMessage : receivedMessagesPerService.get(service))
			calculateNewCovers(service, fCompositionMessage);
	}

	private void coverInputs(final Service service, final Set<InputParameter> inputs) {
		for (final InputParameter input : inputs)
			inputsTablePerService.get(service).put(input, Boolean.TRUE);
	}

	public Set<FCompositionMessage> getMessages(final Service service) {
		if (!receivedMessagesPerService.containsKey(service))
			return new HashSet<FCompositionMessage>();

		return receivedMessagesPerService.get(service);
	}

	public Set<Service> getServicesReceivingMessagesFrom(final Service ancestor) {
		final Set<Service> services = new HashSet<Service>();
		for (final Service service : receivedMessagesPerService.keySet())
			for (final FCompositionMessage fCompositionMessage : receivedMessagesPerService.get(service))
				if (fCompositionMessage.getSourceService().equals(ancestor)) {
					services.add(service);
					break;
				}
		return services;
	}

	public void removeService(final Service service) {
		receivedMessagesPerService.remove(service);
		inputsTablePerService.remove(service);
	}

	@Override
	public String toString() {
		return "receivedMessages: " + receivedMessagesPerService + " inputsTablePerService: " + inputsTablePerService;
	}

	public boolean removeMessagesFrom(final Service service, final Service ancestor) {
		boolean removed = false;
		if (receivedMessagesPerService.containsKey(service))
			for (final Iterator<FCompositionMessage> it = receivedMessagesPerService.get(service).iterator(); it.hasNext();) {
				final FCompositionMessage fCompositionMessage = it.next();
				if (fCompositionMessage.getSourceService().equals(ancestor)) {
					removed = true;
					it.remove();
				}
			}

		if (removed)
			calculateCovers(service);

		return removed;
	}
	
	public void addForwardedSuccessor(final Service successor) {
		forwardedSuccesors.add(successor);
	}
	
	public boolean wasAlreadyForwarded(final Service successor) {
		return forwardedSuccesors.contains(successor);
	}
}