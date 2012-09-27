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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package graphcreation.collisionbased.connectionmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.collisiondetector.Collision;
import graphcreation.collisionbased.connectionManager.Connection;
import graphcreation.collisionbased.connectionManager.ConnectionsManager;
import graphcreation.collisionbased.message.CollisionResponseMessage;
import graphcreation.services.Service;

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

	private SearchResponseMessage searchResponseMessage1, searchResponseMessage2, searchResponseMessage3, searchResponseMessage4, searchResponseMessage5;

	@Before
	public void setUp() throws Exception {
		cManager = new ConnectionsManager(emptyTaxonomy, GraphType.BIDIRECTIONAL);

		cManager.addCollision(new Collision((InputParameter) ParameterFactory.createParameter("I-1", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-1", emptyTaxonomy)));
		cManager.addCollision(new Collision((InputParameter) ParameterFactory.createParameter("I-2", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-2", emptyTaxonomy)));
		cManager.addCollision(new Collision((InputParameter) ParameterFactory.createParameter("I-3", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-3", emptyTaxonomy)));

		Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		Set<Service> services = new HashSet<Service>();
		final Service s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		services.add(s1);
		searchResponseMessage1 = new SearchResponseMessage(new PeerID("1"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("1")), new MessageID(new PeerID("1"), MessageIDGenerator.getNewID()));

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		foundParameters.add(ParameterFactory.createParameter("O-3", emptyTaxonomy));
		services = new HashSet<Service>();
		final Service s2 = new Service("S2", new PeerID("2"));
		s2.addParameter(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		s2.addParameter(ParameterFactory.createParameter("O-3", emptyTaxonomy));
		services.add(s2);
		searchResponseMessage2 = new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		services = new HashSet<Service>();
		final Service s3 = new Service("S3", new PeerID("3"));
		s3.addParameter(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		services.add(s3);
		searchResponseMessage3 = new SearchResponseMessage(new PeerID("3"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("3")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-1", emptyTaxonomy));
		services = new HashSet<Service>();
		final Service s4 = new Service("S4", new PeerID("2"));
		s4.addParameter(ParameterFactory.createParameter("O-1", emptyTaxonomy));
		services.add(s4);
		searchResponseMessage4 = new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-4", emptyTaxonomy));
		services = new HashSet<Service>();
		final Service s5 = new Service("S5", new PeerID("5"));
		s5.addParameter(ParameterFactory.createParameter("O-4", emptyTaxonomy));
		services.add(s5);
		searchResponseMessage5 = new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
	}

	private CollisionResponseMessage createCollisionResponseMessage(final Set<Service> services, final PeerID source) {
		final Map<Service, Byte> serviceTable = new HashMap<Service, Byte>();
		for (final Service service : services)
			serviceTable.put(service, Byte.valueOf((byte)0));

		return new CollisionResponseMessage(source, serviceTable);
	}

	@Test
	public void testUpdateConnections() throws InvalidParameterIDException {
		assertTrue(cManager.updateConnections(searchResponseMessage1).isEmpty());

		Map<Connection, Set<PeerID>> updatedConnections = cManager.updateConnections(searchResponseMessage2);
		assertEquals(1, updatedConnections.size());
		final Connection connection = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-2", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-2", emptyTaxonomy)), emptyTaxonomy,  GraphType.BIDIRECTIONAL);
		assertTrue(updatedConnections.containsKey(connection));
		assertEquals(2, updatedConnections.get(connection).size());
		assertTrue(updatedConnections.get(connection).contains(new PeerID("1")));
		assertTrue(updatedConnections.get(connection).contains(new PeerID("2")));

		updatedConnections = cManager.updateConnections(searchResponseMessage3);
		assertEquals(1, updatedConnections.size());
		final Connection connection2 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-2", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-2", emptyTaxonomy)), emptyTaxonomy, GraphType.BIDIRECTIONAL);
		assertTrue(updatedConnections.containsKey(connection2));
		assertEquals(2, updatedConnections.get(connection2).size());
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("1")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("3")));

		assertTrue(cManager.updateConnections(searchResponseMessage3).isEmpty());

		assertTrue(cManager.updateConnections(searchResponseMessage4).isEmpty());

		assertTrue(cManager.updateConnections(searchResponseMessage5).isEmpty());

		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service s6 = new Service("S6", new PeerID("6"));
		s6.addParameter(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		services.add(s6);
		final SearchResponseMessage searchResponseMessage6 = new SearchResponseMessage(new PeerID("6"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("6"), MessageIDGenerator.getNewID()));
		updatedConnections = cManager.updateConnections(searchResponseMessage6);

		assertTrue(updatedConnections.containsKey(connection2));
		assertEquals(3, updatedConnections.get(connection2).size());
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("2")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("3")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("6")));
	}
	
	@Test
	public void testRemoveResponse() {
		cManager.updateConnections(searchResponseMessage1);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		final Set<PeerID> lostDestinations = new HashSet<PeerID>();
		lostDestinations.add(searchResponseMessage1.getSource());
		lostDestinations.add(searchResponseMessage2.getSource());

		final Map<PeerIDSet, Set<Service>> notifications = cManager.removeResponses(lostDestinations);

		assertEquals(1, notifications.size());

		final PeerIDSet notifiedPeers = new PeerIDSet();
		notifiedPeers.addPeer(new PeerID("3"));

		assertTrue(notifications.containsKey(notifiedPeers));
		assertEquals(1, notifications.get(notifiedPeers).size());
		assertTrue(notifications.get(notifiedPeers).contains(new Service("S1", new PeerID("1"))));
	}

	@Test
	public void testRemoveResponses2() {
		cManager.updateConnections(searchResponseMessage1);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		final Set<PeerID> lostDestinations = new HashSet<PeerID>();
		lostDestinations.add(searchResponseMessage1.getSource());
		lostDestinations.add(searchResponseMessage2.getSource());
		lostDestinations.add(searchResponseMessage3.getSource());

		final Map<PeerIDSet, Set<Service>> notifications = cManager.removeResponses(lostDestinations);

		assertTrue(notifications.isEmpty());
	}

	@Test
	public void testRemoveServices() {
		cManager.updateConnections(searchResponseMessage1);
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
		cManager.updateConnections(searchResponseMessage1);
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
		cManager.updateConnections(searchResponseMessage1);
		cManager.updateConnections(searchResponseMessage2);
		cManager.updateConnections(searchResponseMessage3);
		cManager.updateConnections(searchResponseMessage4);
		cManager.updateConnections(searchResponseMessage5);

		Set<Parameter> removedParameters = new HashSet<Parameter>();
		removedParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));

		Set<Connection> connections = cManager.checkCollisions(removedParameters);

		assertEquals(1, connections.size());

		final Set<Collision> collisions = new HashSet<Collision>();
		for (final Connection connection : connections)
			collisions.add(connection.getCollision());

		assertTrue(collisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-1", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-1", emptyTaxonomy))));

		removedParameters = new HashSet<Parameter>();
		removedParameters.add(ParameterFactory.createParameter("I-4", emptyTaxonomy));
		connections = cManager.checkCollisions(removedParameters);

		assertTrue(connections.isEmpty());
	}
	
	private SearchResponseMessage createSearchResponseMessageA(final Taxonomy taxonomy) throws InvalidParameterIDException {
		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B", taxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service service = new Service("S1", new PeerID("1"));
		service.addParameter(ParameterFactory.createParameter("I-B", taxonomy));
		services.add(service);
		return new SearchResponseMessage(new PeerID("1"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("1")), new MessageID(new PeerID("1"), MessageIDGenerator.getNewID()));
	}
	
	private SearchResponseMessage createSearchResponseMessageB(final Taxonomy taxonomy) throws InvalidParameterIDException {
		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-B", taxonomy));
		foundParameters.add(ParameterFactory.createParameter("O-C", taxonomy));
		final Set<Service>services = new HashSet<Service>();
		final Service service = new Service("S2", new PeerID("2"));
		service.addParameter(ParameterFactory.createParameter("O-B", taxonomy));
		service.addParameter(ParameterFactory.createParameter("O-C", taxonomy));
		services.add(service);
		return new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
	}
	
	private SearchResponseMessage createSearchResponseMessageC(final Taxonomy taxonomy) throws InvalidParameterIDException {
		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-B", taxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service service = new Service("S3", new PeerID("3"));
		service.addParameter(ParameterFactory.createParameter("O-B", taxonomy));
		services.add(service);
		return new SearchResponseMessage(new PeerID("3"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("3")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
	}
	
	private SearchResponseMessage createSearchResponseMessageD(final Taxonomy taxonomy) throws InvalidParameterIDException {
		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-A", taxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service service = new Service("S4", new PeerID("2"));
		service.addParameter(ParameterFactory.createParameter("O-A", taxonomy));
		services.add(service);
		return new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
	}
	
	private SearchResponseMessage createSearchResponseMessageE(final Taxonomy taxonomy) throws InvalidParameterIDException {
		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-1", taxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service service = new Service("S5", new PeerID("5"));
		service.addParameter(ParameterFactory.createParameter("O-1", taxonomy));
		services.add(service);
		return new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
	}
	
	private ConnectionsManager prepareConnectionManager(final Taxonomy taxonomy) throws InvalidParameterIDException {
		ConnectionsManager connectionsManager = new ConnectionsManager(taxonomy, GraphType.BIDIRECTIONAL);
		connectionsManager.addCollision(new Collision((InputParameter) ParameterFactory.createParameter("I-A", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-A", taxonomy)));
		connectionsManager.addCollision(new Collision((InputParameter) ParameterFactory.createParameter("I-B", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-B", taxonomy)));
		connectionsManager.addCollision(new Collision((InputParameter) ParameterFactory.createParameter("I-C", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-C", taxonomy)));
		
		connectionsManager.addCollision(new Collision((InputParameter) ParameterFactory.createParameter("I-A", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-B", taxonomy)));
		connectionsManager.addCollision(new Collision((InputParameter) ParameterFactory.createParameter("I-C", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-C", taxonomy)));
		
		return connectionsManager;
	}

	@Test
	public void testUpdateConnectionsWithTaxonomy() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final ConnectionsManager cManagerWithTaxonomy = prepareConnectionManager(taxonomy);
		
		assertTrue(cManagerWithTaxonomy.updateConnections(createSearchResponseMessageA(taxonomy)).isEmpty());

		Map<Connection, Set<PeerID>> updatedConnections = cManagerWithTaxonomy.updateConnections(createSearchResponseMessageB(taxonomy));
		assertEquals(3, updatedConnections.size());
		final Connection connection = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-A", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-B", taxonomy)), taxonomy, GraphType.BIDIRECTIONAL);
		assertTrue(updatedConnections.containsKey(connection));
		assertEquals(2, updatedConnections.get(connection).size());
		assertTrue(updatedConnections.get(connection).contains(new PeerID("1")));
		assertTrue(updatedConnections.get(connection).contains(new PeerID("2")));

		final SearchResponseMessage searchResponseMessageC = createSearchResponseMessageC(taxonomy);
		updatedConnections = cManagerWithTaxonomy.updateConnections(searchResponseMessageC);
		assertEquals(3, updatedConnections.size());
		final Connection connection2 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-A", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-B", taxonomy)), taxonomy, GraphType.BIDIRECTIONAL);
		assertTrue(updatedConnections.containsKey(connection2));
		assertEquals(2, updatedConnections.get(connection2).size());
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("1")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("3")));

		assertTrue(cManagerWithTaxonomy.updateConnections(searchResponseMessageC).isEmpty());

		assertEquals(1, cManagerWithTaxonomy.updateConnections(createSearchResponseMessageD(taxonomy)).size());

		assertTrue(cManagerWithTaxonomy.updateConnections(createSearchResponseMessageE(taxonomy)).isEmpty());

		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B", taxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service s6 = new Service("S6", new PeerID("6"));
		s6.addParameter(ParameterFactory.createParameter("I-B", taxonomy));
		services.add(s6);
		final SearchResponseMessage searchResponseMessage6 = new SearchResponseMessage(new PeerID("6"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("6"), MessageIDGenerator.getNewID()));
		updatedConnections = cManagerWithTaxonomy.updateConnections(searchResponseMessage6);

		assertTrue(updatedConnections.containsKey(connection2));
		assertEquals(3, updatedConnections.get(connection2).size());
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("2")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("3")));
		assertTrue(updatedConnections.get(connection2).contains(new PeerID("6")));
	}
}
