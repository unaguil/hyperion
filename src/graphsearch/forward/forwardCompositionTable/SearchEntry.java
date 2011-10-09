package graphsearch.forward.forwardCompositionTable;

import graphcreation.GraphCreator;
import graphcreation.services.Service;
import graphsearch.forward.message.FCompositionMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import taxonomy.parameter.InputParameter;

class SearchEntry {

	// a map which holds those messages which were received for the
	// specified service
	private final Map<Service, List<FCompositionMessage>> receivedMessagesPerService = new HashMap<Service, List<FCompositionMessage>>();
	private final Map<Service, Map<InputParameter, Boolean>> inputsTablePerService = new HashMap<Service, Map<InputParameter, Boolean>>();

	private final long timestamp;
	private final long firstReceivedMessageRemainingTime;
	
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
			receivedMessagesPerService.put(service, new ArrayList<FCompositionMessage>());
			inputsTablePerService.put(service, createInputsTable(service));
		}

		receivedMessagesPerService.get(service).add(fCompositionMessage);

		calculateNewCovers(service, fCompositionMessage);
	}

	private void calculateNewCovers(final Service service, final FCompositionMessage fCompositionMessage) {
		// Obtain which parameters of the local service are covered by the
		// ancestor service
		final Service ancestor = fCompositionMessage.getSourceService();
		final Set<InputParameter> connectedInputs = gCreator.getConnectedInputs(service, ancestor);

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

	public List<FCompositionMessage> getMessages(final Service service) {
		if (!receivedMessagesPerService.containsKey(service))
			return new ArrayList<FCompositionMessage>();

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
}