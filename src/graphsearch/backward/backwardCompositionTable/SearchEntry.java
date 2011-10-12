package graphsearch.backward.backwardCompositionTable;

import graphcreation.services.Service;
import graphsearch.backward.MessageTree;
import graphsearch.backward.message.BCompositionMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SearchEntry {

	// a map which holds those messages which were received for the
	// specified service
	private final Map<Service, List<BCompositionMessage>> receivedMessagesPerService = new HashMap<Service, List<BCompositionMessage>>();

	private final Map<Service, Set<MessageTree>> messageTrees = new HashMap<Service, Set<MessageTree>>();

	private final long timestamp;
	private final long firstReceivedMessageRemainingTime;

	public SearchEntry(final long createdTime) {
		this.firstReceivedMessageRemainingTime = createdTime;
		this.timestamp = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getRemainingTime() {
		final long elapsedTime = System.currentTimeMillis() - this.timestamp;
		return firstReceivedMessageRemainingTime - elapsedTime;
	}

	public MessageTree addReceivedMessage(final Service service, final BCompositionMessage bCompositionMessage) {
		if (!receivedMessagesPerService.containsKey(service))
			receivedMessagesPerService.put(service, new ArrayList<BCompositionMessage>());

		receivedMessagesPerService.get(service).add(bCompositionMessage);

		if (!messageTrees.containsKey(service))
			messageTrees.put(service, new HashSet<MessageTree>());

		MessageTree messageTree = new MessageTree(bCompositionMessage.getRootID());
		// find tree
		if (!messageTrees.get(service).contains(messageTree))
			messageTrees.get(service).add(messageTree);
		else
			for (final MessageTree tree : messageTrees.get(service))
				if (messageTree.equals(tree))
					messageTree = tree;

		// add the received message;
		messageTree.addMessage(bCompositionMessage);

		// return the tree if it is completed after addition
		return messageTree;
	}

	public List<BCompositionMessage> getMessages(final Service service) {
		if (!receivedMessagesPerService.containsKey(service))
			return new ArrayList<BCompositionMessage>();

		return receivedMessagesPerService.get(service);
	}

	public void removeService(final Service service) {
		receivedMessagesPerService.remove(service);
	}

	@Override
	public String toString() {
		return "receivedMessages: " + receivedMessagesPerService;
	}

	public Set<MessageTree> getCompleteTrees(final Service service) {
		final Set<MessageTree> completeTrees = new HashSet<MessageTree>();
		if (messageTrees.containsKey(service))
			for (final MessageTree tree : messageTrees.get(service))
				if (tree.isComplete())
					completeTrees.add(tree);
		return completeTrees;
	}
}