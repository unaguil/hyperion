package graphsearch.backward;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.backward.message.BCompositionMessage;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import peer.peerid.PeerID;

public class MessageTreeTest {

	private static final int TTL = 5;
	private static final long TIME = 10000;

	@Test
	public void testAddMessage() {
		final BCompositionMessage message = new BCompositionMessage(new SearchID(new PeerID("0")), new Service("1", new PeerID("")), new HashSet<ServiceDistance>(), TTL, TIME, new PeerID("0"));

		final MessageTree messageTree = new MessageTree(message.getRootID());

		assertTrue(messageTree.addMessage(message));

		final BCompositionMessage message2 = new BCompositionMessage(new SearchID(new PeerID("0")), new Service("1", new PeerID("")), new HashSet<ServiceDistance>(), TTL, TIME, new PeerID("0"));

		assertFalse(messageTree.addMessage(message2));

		// Split the first message
		final Set<ServiceDistance> serviceDistances = new HashSet<ServiceDistance>();
		serviceDistances.add(new ServiceDistance(new Service("A", new PeerID("5")), Integer.valueOf(0)));
		serviceDistances.add(new ServiceDistance(new Service("B", new PeerID("6")), Integer.valueOf(0)));
		serviceDistances.add(new ServiceDistance(new Service("C", new PeerID("7")), Integer.valueOf(0)));
		final Service sourceService = new Service("SourceService", new PeerID("0"));
		final Map<Service, BCompositionMessage> messages = message.split(sourceService, serviceDistances, new PeerID("3"), 0);

		assertEquals(3, messages.size());

		for (final BCompositionMessage messagePart : messages.values())
			assertTrue(messageTree.addMessage(messagePart));
	}

	@Test
	public void testIsComplete() {
		final BCompositionMessage message = new BCompositionMessage(new SearchID(new PeerID("0")), new Service("1", new PeerID("")), new HashSet<ServiceDistance>(), TTL, TIME, new PeerID("0"));

		final MessageTree messageTree = new MessageTree(message.getRootID());

		messageTree.addMessage(message);

		// Split the first message
		final Set<ServiceDistance> servicesDistances = new HashSet<ServiceDistance>();
		servicesDistances.add(new ServiceDistance(new Service("A", new PeerID("5")), Integer.valueOf(0)));
		servicesDistances.add(new ServiceDistance(new Service("B", new PeerID("6")), Integer.valueOf(0)));
		servicesDistances.add(new ServiceDistance(new Service("C", new PeerID("7")), Integer.valueOf(0)));
		final Service sourceService = new Service("SourceService", new PeerID("0"));
		final Map<Service, BCompositionMessage> messages = message.split(sourceService, servicesDistances, new PeerID("3"), 0);

		for (final BCompositionMessage messagePart : messages.values())
			messageTree.addMessage(messagePart);

		assertTrue(messageTree.isComplete());

		final MessageTree messageTree2 = new MessageTree(message.getRootID());

		// add a part of the messages
		final LinkedList<BCompositionMessage> list = new LinkedList<BCompositionMessage>(messages.values());
		final BCompositionMessage missingMessage = list.removeLast();

		final Set<BCompositionMessage> partialMessages = new HashSet<BCompositionMessage>(list);

		for (final BCompositionMessage messagePart : partialMessages)
			messageTree2.addMessage(messagePart);

		assertFalse(messageTree2.isComplete());

		messageTree2.addMessage(missingMessage);

		assertTrue(messageTree2.isComplete());
	}
}
