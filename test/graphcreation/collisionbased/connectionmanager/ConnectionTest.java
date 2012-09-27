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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import graphcreation.GraphCreator.GraphType;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.collisionbased.collisiondetector.Collision;
import graphcreation.collisionbased.connectionManager.Connection;
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

public class ConnectionTest {

	private Connection connection1, connection2, connection3;

	private SearchResponseMessage searchResponseMessage1, searchResponseMessage2, searchResponseMessage3, searchResponseMessage4, searchResponseMessage5;
	
	private Taxonomy emptyTaxonomy = new BasicTaxonomy();

	@Before
	public void setUp() throws Exception {
		connection1 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-1", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-1", emptyTaxonomy)), emptyTaxonomy, GraphType.BIDIRECTIONAL);
		connection2 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-2", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-2", emptyTaxonomy)), emptyTaxonomy, GraphType.BIDIRECTIONAL);
		connection3 = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-1", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-1", emptyTaxonomy)), emptyTaxonomy, GraphType.BIDIRECTIONAL);

		Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		Set<Service> services = new HashSet<Service>();
		final Service s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		services.add(s1);
		searchResponseMessage1 = new SearchResponseMessage(new PeerID("1"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("1")), new MessageID(new PeerID("1"), MessageIDGenerator.getNewID()));
		connection2.addSearchResponse(searchResponseMessage1);

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		foundParameters.add(ParameterFactory.createParameter("O-3", emptyTaxonomy));
		services = new HashSet<Service>();
		final Service s2 = new Service("S2", new PeerID("2"));
		s2.addParameter(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		s2.addParameter(ParameterFactory.createParameter("O-3", emptyTaxonomy));
		services.add(s2);
		searchResponseMessage2 = new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection2.addSearchResponse(searchResponseMessage2);

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		services = new HashSet<Service>();
		final Service s3 = new Service("S3", new PeerID("3"));
		s3.addParameter(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		services.add(s3);
		searchResponseMessage3 = new SearchResponseMessage(new PeerID("3"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("3")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection2.addSearchResponse(searchResponseMessage3);

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-1", emptyTaxonomy));
		services = new HashSet<Service>();
		final Service s4 = new Service("S4", new PeerID("2"));
		s4.addParameter(ParameterFactory.createParameter("O-1", emptyTaxonomy));
		services.add(s4);
		searchResponseMessage4 = new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection3.addSearchResponse(searchResponseMessage4);

		foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-4", emptyTaxonomy));
		services = new HashSet<Service>();
		final Service s5 = new Service("S5", new PeerID("5"));
		s5.addParameter(ParameterFactory.createParameter("O-4", emptyTaxonomy));
		services.add(s5);
		searchResponseMessage5 = new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		connection3.addSearchResponse(searchResponseMessage5);
	}

	private CollisionResponseMessage createCollisionResponseMessage(final Set<Service> services, final PeerID source) {
		final Map<Service, Byte> serviceTable = new HashMap<Service, Byte>();
		for (final Service service : services)
			serviceTable.put(service, Byte.valueOf((byte)0));

		return new CollisionResponseMessage(source, serviceTable);
	}

	@Test
	public void testEquals() {
		assertEquals(connection1, connection1);
		assertFalse(connection1.equals(connection2));
		assertTrue(connection1.equals(connection3));
	}

	@Test
	public void testAddSearchResponse() throws InvalidParameterIDException {
		final Connection connection = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-2", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-2", emptyTaxonomy)), emptyTaxonomy, GraphType.BIDIRECTIONAL);

		assertTrue(connection.addSearchResponse(searchResponseMessage1).isEmpty());

		Set<PeerID> notifiedPeers = connection.addSearchResponse(searchResponseMessage2);
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
		foundParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service s6 = new Service("S6", new PeerID("6"));
		s6.addParameter(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		services.add(s6);
		final SearchResponseMessage searchResponseMessage6 = new SearchResponseMessage(new PeerID("6"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("6"), MessageIDGenerator.getNewID()));
		notifiedPeers = connection2.addSearchResponse(searchResponseMessage6);
		assertEquals(3, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("2")));
		assertTrue(notifiedPeers.contains(new PeerID("3")));
		assertTrue(notifiedPeers.contains(new PeerID("6")));
	}

	@Test
	public void testGetCollision() throws InvalidParameterIDException {
		assertEquals(new Collision((InputParameter) ParameterFactory.createParameter("I-1", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-1", emptyTaxonomy)), connection1.getCollision());
	}

	@Test
	public void testIsConnected() throws InvalidParameterIDException {
		assertFalse(connection1.isConnected());

		assertTrue(connection2.isConnected());

		assertFalse(connection3.isConnected());

		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		final Set<Service> services = new HashSet<Service>();
		services.add(new Service("S3", new PeerID("2")));
		final SearchResponseMessage newSearchResponseMessage = new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
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
	public void testRemoveResponses() {
		final Set<PeerID> lostDestinations = new HashSet<PeerID>();
		lostDestinations.add(searchResponseMessage1.getSource());

		final Map<PeerIDSet, Set<Service>> notifications = connection2.removeResponses(lostDestinations);

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
		final Set<PeerID> lostDestinations = new HashSet<PeerID>();
		lostDestinations.add(searchResponseMessage1.getSource());
		lostDestinations.add(searchResponseMessage2.getSource());

		final Map<PeerIDSet, Set<Service>> notifications = connection2.removeResponses(lostDestinations);

		assertEquals(1, notifications.size());

		final PeerIDSet notifiedPeers = new PeerIDSet();
		notifiedPeers.addPeer(new PeerID("3"));

		assertTrue(notifications.containsKey(notifiedPeers));
		assertEquals(1, notifications.get(notifiedPeers).size());
		assertTrue(notifications.get(notifiedPeers).contains(new Service("S1", new PeerID("1"))));
	}

	@Test
	public void testRemoveResponses3() {
		final Set<PeerID> lostDestinations = new HashSet<PeerID>();
		lostDestinations.add(searchResponseMessage1.getSource());
		lostDestinations.add(searchResponseMessage2.getSource());
		lostDestinations.add(searchResponseMessage3.getSource());

		final Map<PeerIDSet, Set<Service>> notifications = connection2.removeResponses(lostDestinations);

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

		final Connection connectionA = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-A", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-B", taxonomy)), taxonomy, GraphType.BIDIRECTIONAL);

		assertTrue(connectionA.addSearchResponse(createSearchMessage(taxonomy)).isEmpty());

		final SearchResponseMessage searchResponseMessageA = createSearchResponseMessageA(taxonomy);
		Set<PeerID> notifiedPeers = connectionA.addSearchResponse(searchResponseMessageA);
		assertEquals(2, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("1")));
		assertTrue(notifiedPeers.contains(new PeerID("2")));

		final SearchResponseMessage searchResponseMessageB = createSearchResponseMessageB(taxonomy);
		notifiedPeers = connectionA.addSearchResponse(searchResponseMessageB);
		assertEquals(2, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("1")));
		assertTrue(notifiedPeers.contains(new PeerID("3")));

		assertTrue(connectionA.addSearchResponse(searchResponseMessageA).isEmpty());
		assertTrue(connectionA.addSearchResponse(searchResponseMessageB).isEmpty());

		final Connection connectionB = new Connection(new Collision((InputParameter) ParameterFactory.createParameter("I-B", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-B", taxonomy)), taxonomy, GraphType.BIDIRECTIONAL);
		connectionB.addSearchResponse(searchResponseMessageA);
		connectionB.addSearchResponse(searchResponseMessageB);
		notifiedPeers = connectionB.addSearchResponse(createSearchResponseMessageC(taxonomy));
		assertEquals(3, notifiedPeers.size());
		assertTrue(notifiedPeers.contains(new PeerID("2")));
		assertTrue(notifiedPeers.contains(new PeerID("3")));
		assertTrue(notifiedPeers.contains(new PeerID("6")));
	}

	private SearchResponseMessage createSearchResponseMessageC(final Taxonomy taxonomy) throws InvalidParameterIDException {
		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B", taxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service s6 = new Service("S6", new PeerID("6"));
		s6.addParameter(ParameterFactory.createParameter("I-B", taxonomy));
		services.add(s6);
		return new SearchResponseMessage(new PeerID("6"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("6"), MessageIDGenerator.getNewID()));
	}
	
	private SearchResponseMessage createSearchResponseMessageB(final Taxonomy taxonomy) throws InvalidParameterIDException {
		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-B", taxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service service = new Service("S3", new PeerID("3"));
		service.addParameter(ParameterFactory.createParameter("O-B", taxonomy));
		services.add(service);
		return new SearchResponseMessage(new PeerID("3"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("3")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
	}
	
	private SearchResponseMessage createSearchMessage(final Taxonomy taxonomy) throws InvalidParameterIDException {
		Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-B", taxonomy));
		Set<Service> services = new HashSet<Service>();
		final Service s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-B", taxonomy));
		services.add(s1);
		return new SearchResponseMessage(new PeerID("1"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("1")), new MessageID(new PeerID("1"), MessageIDGenerator.getNewID()));
	}
	
	private SearchResponseMessage createSearchResponseMessageA(final Taxonomy taxonomy) throws InvalidParameterIDException {
		final Set<Parameter>  foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("O-B", taxonomy));
		foundParameters.add(ParameterFactory.createParameter("O-C", taxonomy));
		final Set<Service> services = new HashSet<Service>();
		final Service service = new Service("S2", new PeerID("2"));
		service.addParameter(ParameterFactory.createParameter("O-B", taxonomy));
		service.addParameter(ParameterFactory.createParameter("O-C", taxonomy));
		services.add(service);
		return new SearchResponseMessage(new PeerID("2"), new PeerID("0"), foundParameters, createCollisionResponseMessage(services, new PeerID("2")), new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
	}
}
