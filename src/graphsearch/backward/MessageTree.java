package graphsearch.backward;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.backward.message.BCompositionMessage;
import graphsearch.backward.message.MessagePart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import peer.message.MessageID;
import peer.peerid.PeerID;

public class MessageTree {

	class Node {

		private final MessageID nodeID;

		private final Set<Node> childs = new HashSet<Node>();

		private final Set<BCompositionMessage> messages = new HashSet<BCompositionMessage>();

		public Node(final MessageID nodeID) {
			this.nodeID = nodeID;
		}

		public void addChild(final MessageID newNodeID) {
			childs.add(new Node(newNodeID));
		}

		public Set<Node> getChilds() {
			return childs;
		}

		public MessageID getNodeID() {
			return nodeID;
		}

		public boolean isComplete() {
			if (childs.isEmpty())
				return areMessagesComplete();

			boolean complete = true;
			for (final Node child : childs)
				complete = complete && child.isComplete();
			return complete;
		}

		private boolean areMessagesComplete() {
			final Set<Integer> totalValues = new HashSet<Integer>();
			final Set<Integer> pNumberValues = new HashSet<Integer>();

			for (final BCompositionMessage message : messages) {
				final LinkedList<MessagePart.Part> parts = new LinkedList<MessagePart.Part>(message.getMessagePart().getParts());
				final MessagePart.Part part = parts.getLast();
				totalValues.add(Integer.valueOf(part.getTotal()));
				pNumberValues.add(Integer.valueOf(part.getPNumber()));
			}

			// All parts must have the same total number
			if (totalValues.size() != 1)
				return false;

			final int expectedTotal = totalValues.iterator().next().intValue();

			// check that pNumberValues contains all the numbers
			if (pNumberValues.size() != expectedTotal)
				return false;

			for (int i = 0; i < expectedTotal; i++)
				if (!pNumberValues.contains(Integer.valueOf(i)))
					return false;

			return true;
		}

		@Override
		public String toString() {
			return "Node: " + nodeID + " childs: " + childs;
		}
	}

	private final Node root;

	public static final MessageTree EMPTY_TREE = new MessageTree(new MessageID(PeerID.VOID_PEERID));

	public MessageTree(final MessageID rootID) {
		this.root = new Node(rootID);
	}

	public boolean addMessage(final BCompositionMessage message) {
		if (!message.getRootID().equals(root.getNodeID()))
			return false;

		final Iterator<MessagePart.Part> it = message.getMessagePart().getParts().iterator();
		if (it.hasNext())
			findNode(message, it, root.getChilds());
		root.messages.add(message);
		return true;
	}

	public boolean isComplete() {
		return root.isComplete();
	}

	private void findNode(final BCompositionMessage message, final Iterator<MessagePart.Part> it, final Set<Node> childs) {
		final MessagePart.Part part = it.next();
		// find the child which has this split id
		Node node = null;
		for (final Node child : childs)
			if (child.getNodeID().equals(part.getPartitionID())) {
				node = child;
				break;
			}

		// if node not was found create it
		if (node == null) {
			node = new Node(part.getPartitionID());
			childs.add(node);
		}

		if (it.hasNext())
			findNode(message, it, node.getChilds());
		else
			// Add message to current node
			node.messages.add(message);
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof MessageTree))
			return false;

		final MessageTree messageTree = (MessageTree) o;
		return this.root.equals(messageTree.root);
	}

	@Override
	public int hashCode() {
		return root.hashCode();
	}

	@Override
	public String toString() {
		return root.toString();
	}

	public Set<Service> getServices() {
		final Set<Service> services = new HashSet<Service>();
		addServices(services, root);
		return services;
	}

	private void addSuccessors(final Set<Service> successors, final Node node) {
		// add current successors
		for (final BCompositionMessage message : node.messages)
			successors.add(message.getSourceService());

		// add all child messages
		for (final Node child : node.childs)
			addSuccessors(successors, child);
	}

	private void addServices(final Set<Service> services, final Node node) {
		// add current received services
		for (final BCompositionMessage message : node.messages)
			services.addAll(message.getComposition());

		// add all child messages
		for (final Node child : node.childs)
			addServices(services, child);
	}

	public Set<Service> getSuccessors() {
		final Set<Service> successors = new HashSet<Service>();
		addSuccessors(successors, root);
		return successors;
	}

	private void addServiceDistances(final Map<Service, Set<ServiceDistance>> ancestorDistances, final Node node) {
		// add current received services
		for (final BCompositionMessage message : node.messages)
			for (final Service service : message.getAncestorDistances().keySet()) {
				if (!ancestorDistances.containsKey(service))
					ancestorDistances.put(service, new HashSet<ServiceDistance>());
				ancestorDistances.get(service).addAll(message.getAncestorDistances().get(service));
			}

		// add all child messages
		for (final Node child : node.childs)
			addServiceDistances(ancestorDistances, child);
	}

	public Map<Service, Set<ServiceDistance>> getAncestorDistances() {
		final Map<Service, Set<ServiceDistance>> ancestorDistances = new HashMap<Service, Set<ServiceDistance>>();
		addServiceDistances(ancestorDistances, root);
		return ancestorDistances;
	}
}
