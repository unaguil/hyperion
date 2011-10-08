package graphcreation.collisionbased.connectionmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.collisionbased.collisiondetector.Collision;
import graphcreation.collisionbased.connectionManager.Connection;
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

public class ConnectionTest {

	private Connection connection1, connection2, connection3;

	private SearchResponseMessage searchResponseMessage, searchResponseMessage2, searchResponseMessage3, searchResponseMessage4, searchResponseMessage5;

	@Before
	public void setUp() throws Exception {
		connection1 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-A")));
		connection2 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-B"), (OutputParameter) ParameterFactory.createParameter("O-B")));
		connection3 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-A")));

		Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B"));
		Set<Service> services = new HashSet<Service>();
		final Service s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-B"));
		services.add(s1);
		searchResponseMessage = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("1")), new PeerID("1"), new MessageID(new PeerID("1"), MessageIDGenerator.getNewID()));
		connection2.addSearchResponse(searchResponseMessage);

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-B"));
		foundParameters.add(ParameterFactory.createParameter("O-C"));
		services = new HashSet<Service>();
		final Service s2 = new Service("S2", new PeerID("2"));
		s2.addParameter(ParameterFactory.createParameter("O-B"));
		s2.addParameter(ParameterFactory.createParameter("O-C"));
		services.add(s2);
		searchResponseMessage2 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("2"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection2.addSearchResponse(searchResponseMessage2);

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-B"));
		services = new HashSet<Service>();
		final Service s3 = new Service("S3", new PeerID("3"));
		s3.addParameter(ParameterFactory.createParameter("O-B"));
		services.add(s3);
		searchResponseMessage3 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("3")), new PeerID("3"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection2.addSearchResponse(searchResponseMessage3);

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-A"));
		services = new HashSet<Service>();
		final Service s4 = new Service("S4", new PeerID("2"));
		s4.addParameter(ParameterFactory.createParameter("O-A"));
		services.add(s4);
		searchResponseMessage4 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("2"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection3.addSearchResponse(searchResponseMessage4);

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-D"));
		services = new HashSet<Service>();
		final Service s5 = new Service("S5", new PeerID("5"));
		s5.addParameter(ParameterFactory.createParameter("O-D"));
		services.add(s5);
		searchResponseMessage5 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("2"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection3.addSearchResponse(searchResponseMessage5);
	}

	private CollisionResponseMessage createCollisionResponseMessage(final Set<Service> services, final PeerID source) {
		final Map<Service, Integer> serviceTable = new HashMap<Service, Integer>();
		for (final Service service : services)
			serviceTable.put(service, Integer.valueOf(0));

		return new CollisionResponseMessage(serviceTable, source);
	}

	@Test
	public void testEquals() {
		assertEquals(connection1, connection1);
		assertFalse(connection1.equals(connection2));
		assertTrue(connection1.equals(connection3));
	}

	@Test
	public void testAddSearchResponse() throws InvalidParameterIDException {
		final Connection connection = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-B"), (OutputParameter) ParameterFactory.createParameter("O-B")));

		assertTrue(connection.addSearchResponse(searchResponseMessage).isEmpty());

		PeerIDSet notifiedPeers = connection.addSearchResponse(searchResponseMessage2);
		assertEquals(2, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("1")));
		assertTrue(notifiedPeers.contains(new PeerID("2")));

		notifiedPeers = connection.addSearchResponse(searchResponseMessage3);
		assertEquals(2, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("1")));
		assertTrue(notifiedPeers.contains(new PeerID("3")));

		assertTrue(connection.addSearchResponse(searchResponseMessage2).isEmpty());
		assertTrue(connection.addSearchResponse(searchResponseMessage3).isEmpty());

		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B"));
		final Set<Service> services = new HashSet<Service>();
		services.add(new Service("S6", new PeerID("6")));
		final SearchResponseMessage searchResponseMessage6 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("6"), new MessageID(new PeerID("6"), MessageIDGenerator.getNewID()));
		notifiedPeers = connection2.addSearchResponse(searchResponseMessage6);
		assertEquals(3, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("2")));
		assertTrue(notifiedPeers.contains(new PeerID("3")));
		assertTrue(notifiedPeers.contains(new PeerID("6")));
	}

	@Test
	public void testGetCollision() throws InvalidParameterIDException {
		assertEquals(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-A")), connection1.getCollision());
	}

	@Test
	public void testIsConnected() throws InvalidParameterIDException {
		assertFalse(connection1.isConnected());

		assertTrue(connection2.isConnected());

		assertFalse(connection3.isConnected());

		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-A"));
		final Set<Service> services = new HashSet<Service>();
		services.add(new Service("S3", new PeerID("2")));
		final SearchResponseMessage newSearchResponseMessage = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("2"), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection3.addSearchResponse(newSearchResponseMessage);

		assertTrue(connection3.isConnected());
	}

	@Test
	public void testGetInputServicesTable() {
		assertTrue(connection1.getInputServicesTable().isEmpty());
		assertTrue(connection3.getInputServicesTable().isEmpty());

		assertFalse(connection2.getInputServicesTable().isEmpty());

		assertEquals(1, connection2.getInputServicesTable().size());
		assertTrue(connection2.getInputServicesTable().contains(new ServiceDistance(new Service("S1", new PeerID("1")), Integer.valueOf(0))));
	}

	@Test
	public void testGetOutputServicesTable() {
		assertTrue(connection1.getOutputServicesTable().isEmpty());

		assertFalse(connection2.getOutputServicesTable().isEmpty());
		assertEquals(2, connection2.getOutputServicesTable().size());
		assertTrue(connection2.getOutputServicesTable().contains(new ServiceDistance(new Service("S2", new PeerID("2")), Integer.valueOf(0))));
		assertTrue(connection2.getOutputServicesTable().contains(new ServiceDistance(new Service("S3", new PeerID("3")), Integer.valueOf(0))));

		assertFalse(connection3.getOutputServicesTable().isEmpty());
		assertEquals(1, connection3.getOutputServicesTable().size());
		assertTrue(connection3.getOutputServicesTable().contains(new ServiceDistance(new Service("S4", new PeerID("2")), Integer.valueOf(0))));
	}

	@Test
	public void testGetInputPeers() {
		assertTrue(connection1.getInputPeers().isEmpty());

		assertEquals(1, connection2.getInputPeers().size());
		assertTrue(connection2.getInputPeers().contains(new PeerID("1")));

		assertTrue(connection3.getInputPeers().isEmpty());
	}

	@Test
	public void testGetOutputPeers() {
		assertTrue(connection1.getOutputPeers().isEmpty());

		assertEquals(2, connection2.getOutputPeers().size());
		assertTrue(connection2.getOutputPeers().contains(new PeerID("2")));
		assertTrue(connection2.getOutputPeers().contains(new PeerID("3")));

		assertTrue(connection3.getOutputPeers().contains(new PeerID("2")));
	}

	@Test
	public void testRemoveParameters() throws InvalidParameterIDException {
		Map<PeerIDSet, Set<Service>> notifiedPeers = connection2.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-B")), new PeerID("1"));
		assertTrue(connection2.getInputPeers().isEmpty());
		assertEquals(1, notifiedPeers.size());

		assertTrue(connection2.getInputPeers().isEmpty());

		final PeerIDSet peers = new PeerIDSet();
		peers.addPeer(new PeerID("2"));
		peers.addPeer(new PeerID("3"));
		assertTrue(notifiedPeers.containsKey(peers));
		assertEquals(notifiedPeers.get(peers), Collections.singleton(new Service("S1", new PeerID("1"))));

		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("O-B"));
		parameters.add(ParameterFactory.createParameter("O-C"));
		notifiedPeers = connection2.removeParameters(parameters, new PeerID("2"));
		assertTrue(notifiedPeers.isEmpty());

		assertEquals(1, connection2.getOutputPeers().size());
	}

	@Test
	public void testRemoveParameters2() throws InvalidParameterIDException {
		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("O-B"));
		parameters.add(ParameterFactory.createParameter("O-C"));
		final Map<PeerIDSet, Set<Service>> notifiedPeers = connection2.removeParameters(parameters, new PeerID("2"));

		assertEquals(1, notifiedPeers.size());
		final PeerIDSet peers = new PeerIDSet();
		peers.addPeer(new PeerID("1"));
		assertTrue(notifiedPeers.containsKey(peers));

		assertTrue(notifiedPeers.get(peers).contains(new Service("S2", new PeerID("2"))));

		assertEquals(1, connection2.getInputPeers().size());

		assertEquals(1, connection2.getOutputPeers().size());
	}

	@Test
	public void testRemoveResponses() {
		final Set<MessageID> lostRoutes = new HashSet<MessageID>();
		lostRoutes.add(searchResponseMessage.getRemoteMessageID());

		final Map<PeerIDSet, Set<Service>> notifications = connection2.removeResponses(lostRoutes);

		assertEquals(1, notifications.size());

		final PeerIDSet notifiedPeers = new PeerIDSet();
		notifiedPeers.addPeer(new PeerID("2"));
		notifiedPeers.addPeer(new PeerID("3"));

		assertTrue(notifications.containsKey(notifiedPeers));
		assertEquals(1, notifications.get(notifiedPeers).size());
		assertTrue(notifications.get(notifiedPeers).contains(new Service("S1", new PeerID("1"))));
	}

	@Test
	public void testRemoveResponses2() {
		final Set<MessageID> lostRoutes = new HashSet<MessageID>();
		lostRoutes.add(searchResponseMessage.getRemoteMessageID());
		lostRoutes.add(searchResponseMessage2.getRemoteMessageID());

		final Map<PeerIDSet, Set<Service>> notifications = connection2.removeResponses(lostRoutes);

		assertEquals(1, notifications.size());

		final PeerIDSet notifiedPeers = new PeerIDSet();
		notifiedPeers.addPeer(new PeerID("3"));

		assertTrue(notifications.containsKey(notifiedPeers));
		assertEquals(1, notifications.get(notifiedPeers).size());
		assertTrue(notifications.get(notifiedPeers).contains(new Service("S1", new PeerID("1"))));
	}

	@Test
	public void testRemoveResponses3() {
		final Set<MessageID> lostRoutes = new HashSet<MessageID>();
		lostRoutes.add(searchResponseMessage.getRemoteMessageID());
		lostRoutes.add(searchResponseMessage2.getRemoteMessageID());
		lostRoutes.add(searchResponseMessage3.getRemoteMessageID());

		final Map<PeerIDSet, Set<Service>> notifications = connection2.removeResponses(lostRoutes);

		assertTrue(notifications.isEmpty());
	}

	@Test
	public void testRemoveServices() {
		final Set<Service> removedServices = new HashSet<Service>();
		removedServices.add(new Service("S1", new PeerID("1")));

		final Map<PeerIDSet, Set<Service>> notifications = connection2.removeServices(removedServices, new PeerID("1"));
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
		final Set<Service> removedServices = new HashSet<Service>();
		removedServices.add(new Service("S2", new PeerID("2")));

		final Map<PeerIDSet, Set<Service>> notifications = connection2.removeServices(removedServices, new PeerID("2"));
		assertEquals(1, notifications.size());

		final PeerIDSet notifiedPeers = new PeerIDSet();
		notifiedPeers.addPeer(new PeerID("1"));

		assertTrue(notifications.containsKey(notifiedPeers));
		assertEquals(1, notifications.get(notifiedPeers).size());
		assertTrue(notifications.get(notifiedPeers).contains(new Service("S2", new PeerID("2"))));
	}

	@Test
	public void testAddSearchResponseWithTaxonomy() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final Connection connection = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-B")), taxonomy);

		assertTrue(connection.addSearchResponse(searchResponseMessage).isEmpty());

		PeerIDSet notifiedPeers = connection.addSearchResponse(searchResponseMessage2);
		assertEquals(2, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("1")));
		assertTrue(notifiedPeers.contains(new PeerID("2")));

		notifiedPeers = connection.addSearchResponse(searchResponseMessage3);
		assertEquals(2, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("1")));
		assertTrue(notifiedPeers.contains(new PeerID("3")));

		assertTrue(connection.addSearchResponse(searchResponseMessage2).isEmpty());
		assertTrue(connection.addSearchResponse(searchResponseMessage3).isEmpty());

		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B"));
		final Set<Service> services = new HashSet<Service>();
		services.add(new Service("S6", new PeerID("6")));
		final SearchResponseMessage searchResponseMessage6 = new SearchResponseMessage(new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new PeerID("6"), new MessageID(new PeerID("6"), MessageIDGenerator.getNewID()));
		notifiedPeers = connection2.addSearchResponse(searchResponseMessage6);
		assertEquals(3, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("2")));
		assertTrue(notifiedPeers.contains(new PeerID("3")));
		assertTrue(notifiedPeers.contains(new PeerID("6")));
	}
}
