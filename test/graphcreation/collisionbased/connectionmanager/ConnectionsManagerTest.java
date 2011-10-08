package graphcreation.collisionbased.connectionmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import graphcreation.collisionbased.collisiondetector.Collision;
import graphcreation.collisionbased.connectionManager.Connection;
import graphcreation.collisionbased.connectionManager.ConnectionsManager;
import graphcreation.collisionbased.message.CollisionResponseMessage;
import graphcreation.services.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multicast.search.message.SearchResponseMessage;

import org.junit.Before;
import org.junit.Test;

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.TaxonomyException;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class ConnectionsManagerTest {

	private final static Taxonomy emptyTaxonomy = new BasicTaxonomy();

	private ConnectionsManager cManager;

	private SearchResponseMessage searchResponseMessage, searchResponseMessage2, searchResponseMessage3, searchResponseMessage4, searchResponseMessage5;

	@Before
	public void setUp() throws Exception {
		cManager = new ConnectionsManager(new BasicTaxonomy());

		cManager.addConnection(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-A")));
		cManager.addConnection(new Collision((InputParameter) ParameterFactory.createParameter("I-B"), (OutputParameter) ParameterFactory.createParameter("O-B")));
		cManager.addConnection(new Collision((InputParameter) ParameterFactory.createParameter("I-C"), (OutputParameter) ParameterFactory.createParameter("O-C")));

		Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B"));
		Set<Service> services = new HashSet<Service>();
		final Service s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-B"));
		services.add(s1);
		searchResponseMessage = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("1")), new PeerID("1"), new MessageID(new PeerID("1"), MessageIDGenerator.getNewID()));

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-B"));
		foundParameters.add(ParameterFactory.createParameter("O-C"));
		services = new HashSet<Service>();
		final Service s2 = new Service("S2", new PeerID("2"));
		s2.addParameter(ParameterFactory.createParameter("O-B"));
		s2.addParameter(ParameterFactory.createParameter("O-C"));
		services.add(s2);
		searchResponseMessage2 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("2"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-B"));
		services = new HashSet<Service>();
		final Service s3 = new Service("S3", new PeerID("3"));
		s3.addParameter(ParameterFactory.createParameter("O-B"));
		services.add(s3);
		searchResponseMessage3 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("3")), new PeerID("3"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-A"));
		services = new HashSet<Service>();
		final Service s4 = new Service("S4", new PeerID("2"));
		s4.addParameter(ParameterFactory.createParameter("O-A"));
		services.add(s4);
		searchResponseMessage4 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("2"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-D"));
		services = new HashSet<Service>();
		final Service s5 = new Service("S5", new PeerID("5"));
		s5.addParameter(ParameterFactory.createParameter("O-D"));
		services.add(s5);
		searchResponseMessage5 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("2"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
	}

	private CollisionResponseMessage createCollisionResponseMessage(final Set<Service> services, final PeerID source) {
		final Map<Service, Integer> serviceTable = new HashMap<Service, Integer>();
		for (final Service service : services)
			serviceTable.put(service, Integer.valueOf(0));

		return new CollisionResponseMessage(serviceTable, source);
	}

	@Test
	public void testUpdateConnections() throws InvalidParameterIDException {
		assertTrue(cManager.updateConnections(searchResponseMessage).isEmpty());

		Map<Connection, PeerIDSet> updatedConnections = cManager.updateConnections(searchResponseMessage2);
		assertEquals(1, updatedConnections.size());
		final Connection connection = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-B"), (OutputParameter) ParameterFactory.createParameter("O-B")), emptyTaxonomy);
		assertTrue(updatedConnections.containsKey(connection));
		assertEquals(2, updatedConnections.get(connection).size());
		assertTrue(updatedConnections.get(connection).contains(new PeerID("1")));
		assertTrue(updatedConnections.get(connection).contains(new PeerID("2")));

		updatedConnections = cManager.updateConnections(searchResponseMessage3);
		assertEquals(1, updatedConnections.size());
		final Connection connection2 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-B"), (OutputParameter) ParameterFactory.createParameter("O-B")), emptyTaxonomy);
		assertTrue(updatedConnections.containsKey(connection2));
		assertEquals(2, updatedConnections.get(connection2).size());
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("1")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("3")));

		assertTrue(cManager.updateConnections(searchResponseMessage3).isEmpty());

		assertTrue(cManager.updateConnections(searchResponseMessage4).isEmpty());

		assertTrue(cManager.updateConnections(searchResponseMessage5).isEmpty());

		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B"));
		final Set<Service> services = new HashSet<Service>();
		services.add(new Service("S6", new PeerID("6")));
		final SearchResponseMessage searchResponseMessage6 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("6"), new MessageID(new PeerID("6"), MessageIDGenerator.getNewID()));
		updatedConnections = cManager.updateConnections(searchResponseMessage6);

		assertTrue(updatedConnections.containsKey(connection2));
		assertEquals(3, updatedConnections.get(connection2).size());
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("2")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("3")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("6")));
	}

	@Test
	public void testRemoveParameters() throws InvalidParameterIDException {
		cManager.updateConnections(searchResponseMessage);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		final Map<PeerIDSet, Set<Service>> notifiedPeers = cManager.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-B")), new PeerID("1"));
		assertEquals(1, notifiedPeers.size());
	}

	@Test
	public void testRemoveResponse() {
		cManager.updateConnections(searchResponseMessage);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		final Set<MessageID> lostRoutes = new HashSet<MessageID>();
		lostRoutes.add(searchResponseMessage.getRemoteMessageID());
		lostRoutes.add(searchResponseMessage2.getRemoteMessageID());

		final Map<PeerIDSet, Set<Service>> notifications = cManager.removeResponses(lostRoutes);

		assertEquals(1, notifications.size());

		final PeerIDSet notifiedPeers = new PeerIDSet();
		notifiedPeers.addPeer(new PeerID("3"));

		assertTrue(notifications.containsKey(notifiedPeers));
		assertEquals(1, notifications.get(notifiedPeers).size());
		assertTrue(notifications.get(notifiedPeers).contains(new Service("S1", new PeerID("1"))));
	}

	@Test
	public void testRemoveResponses2() {
		cManager.updateConnections(searchResponseMessage);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		final Set<MessageID> lostRoutes = new HashSet<MessageID>();
		lostRoutes.add(searchResponseMessage.getRemoteMessageID());
		lostRoutes.add(searchResponseMessage2.getRemoteMessageID());
		lostRoutes.add(searchResponseMessage3.getRemoteMessageID());

		final Map<PeerIDSet, Set<Service>> notifications = cManager.removeResponses(lostRoutes);

		assertTrue(notifications.isEmpty());
	}

	@Test
	public void testRemoveServices() {
		cManager.updateConnections(searchResponseMessage);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		final Set<Service> removedServices = new HashSet<Service>();
		removedServices.add(new Service("S1", new PeerID("1")));

		final Map<PeerIDSet, Set<Service>> notifications = cManager.removeServices(removedServices, new PeerID("1"));
		assertEquals(1, notifications.size());

		final PeerIDSet notifiedPeers = new PeerIDSet();
		notifiedPeers.addPeer(new PeerID("2"));
		notifiedPeers.addPeer(new PeerID("3"));

		assertTrue(notifications.containsKey(notifiedPeers));
		assertEquals(1, notifications.get(notifiedPeers).size());
		assertTrue(notifications.get(notifiedPeers).contains(new Service("S1", new PeerID("1"))));
	}

	@Test
	public void testRemoveServices2() {
		cManager.updateConnections(searchResponseMessage);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		final Set<Service> removedServices = new HashSet<Service>();
		removedServices.add(new Service("S2", new PeerID("2")));

		final Map<PeerIDSet, Set<Service>> notifications = cManager.removeServices(removedServices, new PeerID("2"));
		assertEquals(1, notifications.size());

		final PeerIDSet notifiedPeers = new PeerIDSet();
		notifiedPeers.addPeer(new PeerID("1"));

		assertTrue(notifications.containsKey(notifiedPeers));
		assertEquals(1, notifications.get(notifiedPeers).size());
		assertTrue(notifications.get(notifiedPeers).contains(new Service("S2", new PeerID("2"))));
	}

	@Test
	public void testCheckConnections() throws InvalidParameterIDException {
		cManager.updateConnections(searchResponseMessage);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		Set<Parameter> removedParameters = new HashSet<Parameter>();
		removedParameters.add(ParameterFactory.createParameter("I-A"));

		Set<Connection> connections = cManager.checkCollisions(removedParameters);

		assertEquals(1, connections.size());

		final Set<Collision> collisions = new HashSet<Collision>();
		for (final Connection connection : connections)
			collisions.add(connection.getCollision());

		assertTrue(collisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-A"))));

		removedParameters = new HashSet<Parameter>();
		removedParameters.add(ParameterFactory.createParameter("I-D"));
		connections = cManager.checkCollisions(removedParameters);

		assertTrue(connections.isEmpty());
	}

	@Test
	public void testUpdateConnectionsWithTaxonomy() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final ConnectionsManager cManagerWithTaxonomy = new ConnectionsManager(taxonomy);

		cManagerWithTaxonomy.addConnection(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-B")));
		cManagerWithTaxonomy.addConnection(new Collision((InputParameter) ParameterFactory.createParameter("I-C"), (OutputParameter) ParameterFactory.createParameter("O-C")));

		assertTrue(cManagerWithTaxonomy.updateConnections(searchResponseMessage).isEmpty());

		Map<Connection, PeerIDSet> updatedConnections = cManagerWithTaxonomy.updateConnections(searchResponseMessage2);
		assertEquals(1, updatedConnections.size());
		final Connection connection = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-B")));
		assertTrue(updatedConnections.containsKey(connection));
		assertEquals(2, updatedConnections.get(connection).size());
		assertTrue(updatedConnections.get(connection).contains(new PeerID("1")));
		assertTrue(updatedConnections.get(connection).contains(new PeerID("2")));

		updatedConnections = cManagerWithTaxonomy.updateConnections(searchResponseMessage3);
		assertEquals(1, updatedConnections.size());
		final Connection connection2 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-B")));
		assertTrue(updatedConnections.containsKey(connection2));
		assertEquals(2, updatedConnections.get(connection2).size());
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("1")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("3")));

		assertTrue(cManagerWithTaxonomy.updateConnections(searchResponseMessage3).isEmpty());

		assertTrue(cManagerWithTaxonomy.updateConnections(searchResponseMessage4).isEmpty());

		assertTrue(cManagerWithTaxonomy.updateConnections(searchResponseMessage5).isEmpty());

		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B"));
		final Set<Service> services = new HashSet<Service>();
		services.add(new Service("S6", new PeerID("6")));
		final SearchResponseMessage searchResponseMessage6 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("6"), new MessageID(new PeerID("6"), MessageIDGenerator.getNewID()));
		updatedConnections = cManagerWithTaxonomy.updateConnections(searchResponseMessage6);

		assertTrue(updatedConnections.containsKey(connection2));
		assertEquals(3, updatedConnections.get(connection2).size());
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("2")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("3")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("6")));
	}
}
