package multicast.search.unicastTable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import multicast.search.message.SearchMessage;
import multicast.search.message.SearchMessage.SearchType;
import multicast.search.message.SearchResponseMessage;

import org.junit.Before;
import org.junit.Test;

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.peerid.PeerID;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class UnicastTableTest {

	private UnicastTable table1, table2, table3;

	private SearchResponseMessage searchResponseMessage1, searchResponseMessage2, searchResponseMessage3, searchResponseMessage4;

	private SearchMessage searchMessage1, searchMessage2, searchMessage3, searchMessage4, localSearchMessage1, localSearchMessage2;

	@Before
	public void setUp() throws Exception, InvalidParameterIDException {
		table1 = new UnicastTable(PeerID.VOID_PEERID);
		searchMessage1 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("1"), new PeerID("2"));
		searchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-B")), new PeerID("3"), new PeerID("4"));
		final Set<Parameter> searchParameters = new HashSet<Parameter>();
		searchParameters.add(ParameterFactory.createParameter("I-H"));
		searchParameters.add(ParameterFactory.createParameter("I-I"));
		searchMessage3 = createSearchMessage(searchParameters, new PeerID("13"), new PeerID("14"));
		searchMessage4 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-C")), new PeerID("17"), new PeerID("4"));
		searchResponseMessage1 = createSearchResponseMessage(Collections.singleton(ParameterFactory.createParameter("I-C")), new PeerID("5"), new PeerID("6"), searchMessage4);
		searchResponseMessage2 = createSearchResponseMessage(Collections.singleton(ParameterFactory.createParameter("I-C")), new PeerID("7"), new PeerID("6"), searchMessage4);
		searchResponseMessage3 = createSearchResponseMessage(Collections.singleton(ParameterFactory.createParameter("I-C")), new PeerID("7"), new PeerID("6"), searchMessage4);

		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-E"));
		parameters.add(ParameterFactory.createParameter("I-F"));
		final SearchMessage searchMessage5 = createSearchMessage(parameters, new PeerID("18"), new PeerID("4"));
		searchResponseMessage4 = createSearchResponseMessage(parameters, new PeerID("8"), new PeerID("9"), searchMessage5);

		final SearchMessage searchMessage6 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-D")), new PeerID("17"), new PeerID("4"));
		final SearchResponseMessage localSearchResponseMessage1 = createSearchResponseMessage(Collections.singleton(ParameterFactory.createParameter("I-C")), PeerID.VOID_PEERID, PeerID.VOID_PEERID, searchMessage6);
		final SearchMessage searchMessage7 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-D")), new PeerID("17"), new PeerID("4"));
		final SearchResponseMessage localSearchResponseMessage2 = createSearchResponseMessage(Collections.singleton(ParameterFactory.createParameter("I-D")), PeerID.VOID_PEERID, PeerID.VOID_PEERID, searchMessage7);

		localSearchMessage1 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-L")), PeerID.VOID_PEERID, PeerID.VOID_PEERID);
		localSearchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-M")), PeerID.VOID_PEERID, PeerID.VOID_PEERID);

		table1.updateUnicastTable(searchMessage1);
		table1.updateUnicastTable(searchMessage2);
		table1.updateUnicastTable(searchMessage3);
		table1.updateUnicastTable(searchMessage4);
		table1.updateUnicastTable(searchMessage5);
		table1.updateUnicastTable(searchResponseMessage1);
		table1.updateUnicastTable(searchResponseMessage2);
		table1.updateUnicastTable(searchResponseMessage3);
		table1.updateUnicastTable(searchResponseMessage4);
		table1.updateUnicastTable(localSearchResponseMessage1);
		table1.updateUnicastTable(localSearchResponseMessage2);
		table1.updateUnicastTable(localSearchMessage1);
		table1.updateUnicastTable(localSearchMessage2);

		table2 = new UnicastTable(PeerID.VOID_PEERID);
		table2.updateUnicastTable(createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-D")), new PeerID("1"), new PeerID("2")));
		table2.updateUnicastTable(createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-B")), new PeerID("3"), new PeerID("3")));
		table2.updateUnicastTable(createSearchResponseMessage(Collections.singleton(ParameterFactory.createParameter("I-C")), new PeerID("5"), new PeerID("6"), searchMessage4));

		table3 = new UnicastTable(PeerID.VOID_PEERID);
		table3.updateUnicastTable(searchMessage1);
		table3.updateUnicastTable(searchMessage2);
		table3.updateUnicastTable(searchMessage3);
		table3.updateUnicastTable(searchMessage4);
		table3.updateUnicastTable(searchMessage5);
		table3.updateUnicastTable(searchResponseMessage1);
		table3.updateUnicastTable(searchResponseMessage2);
		table3.updateUnicastTable(searchResponseMessage3);
		table3.updateUnicastTable(searchResponseMessage4);
		table3.updateUnicastTable(localSearchResponseMessage1);
		table3.updateUnicastTable(localSearchResponseMessage2);
		table3.updateUnicastTable(localSearchMessage1);
		table3.updateUnicastTable(localSearchMessage2);
	}

	private SearchMessage createSearchMessage(final Set<Parameter> parameters, final PeerID source, final PeerID sender) {
		final SearchMessage searchMessage = new SearchMessage(parameters, null, source, 0, 0, SearchType.Exact);

		return new SearchMessage(searchMessage, sender, 0);
	}

	private SearchResponseMessage createSearchResponseMessage(final Set<Parameter> parameters, final PeerID source, final PeerID sender, final SearchMessage searchMessage) {
		final SearchResponseMessage searchResponseMessage = new SearchResponseMessage(PeerID.VOID_PEERID, parameters, null, source, searchMessage.getRemoteMessageID());

		return new SearchResponseMessage(searchResponseMessage, sender, PeerID.VOID_PEERID, 0);
	}

	@Test
	public void testGetSearchedParameters() throws InvalidParameterIDException {
		final Set<Parameter> searchedParameters = table1.getSearchedParameters();
		assertEquals(2, searchedParameters.size());

		assertTrue(searchedParameters.contains(ParameterFactory.createParameter("I-L")));
		assertTrue(searchedParameters.contains(ParameterFactory.createParameter("I-M")));
	}

	@Test
	public void testGetRouteIDs() {
		assertEquals(1, table1.getRouteIDs(new PeerID("1")).size());
		assertEquals(1, table1.getRouteIDs(new PeerID("3")).size());
		assertEquals(1, table1.getRouteIDs(new PeerID("5")).size());
		assertEquals(2, table1.getRouteIDs(new PeerID("7")).size());
	}

	@Test
	public void testGetLocalParameterRoutes() throws InvalidParameterIDException {
		assertTrue(table1.getLocalParameterRoutes(ParameterFactory.createParameter("I-A")).isEmpty());
		assertEquals(1, table1.getLocalParameterRoutes(ParameterFactory.createParameter("I-C")).size());
		assertEquals(1, table1.getLocalParameterRoutes(ParameterFactory.createParameter("I-D")).size());
	}

	@Test
	public void testGetActiveSearches() {
		assertEquals(7, table1.getActiveSearches().size());

		Set<MessageID> routeIDs = table1.getRouteIDs(new PeerID("1"));
		table1.removeRoute(routeIDs.iterator().next(), new PeerID("2"));

		routeIDs = table1.getRouteIDs(new PeerID("5"));
		table1.removeRoute(routeIDs.iterator().next(), new PeerID("6"));

		routeIDs = table1.getRouteIDs(new PeerID("7"));
		table1.removeRoute(routeIDs.iterator().next(), new PeerID("6"));

		assertEquals(6, table1.getActiveSearches().size());

		routeIDs = table1.getRouteIDs(new PeerID("3"));
		table1.removeRoute(routeIDs.iterator().next(), new PeerID("4"));

		assertEquals(5, table1.getActiveSearches().size());

		routeIDs = table1.getRouteIDs(new PeerID("13"));
		table1.removeRoute(routeIDs.iterator().next(), new PeerID("14"));

		assertEquals(4, table1.getActiveSearches().size());

		routeIDs = table1.getRouteIDs(new PeerID("17"));
		table1.removeRoute(routeIDs.iterator().next(), new PeerID("4"));

		assertEquals(3, table1.getActiveSearches().size());

		routeIDs = table1.getRouteIDs(new PeerID("18"));
		table1.removeRoute(routeIDs.iterator().next(), new PeerID("4"));

		assertEquals(2, table1.getActiveSearches().size());
	}

	@Test
	public void testEquals() {
		assertEquals(table1, table1);
		assertEquals(table1, table3);

		assertFalse(table1.equals(table2));

		assertEquals(new UnicastTable(PeerID.VOID_PEERID), new UnicastTable(PeerID.VOID_PEERID));
	}

	@Test
	public void testKnowsRouteTo() {
		assertTrue(table1.knowsRouteTo(new PeerID("1")));
		assertTrue(table1.knowsRouteTo(new PeerID("3")));
		assertTrue(table1.knowsRouteTo(new PeerID("5")));
		assertTrue(table1.knowsRouteTo(new PeerID("7")));
		assertFalse(table1.knowsRouteTo(new PeerID("2")));
	}

	@Test
	public void testKnowsSearchRouteTo() {
		assertTrue(table1.knowsSearchRouteTo(new PeerID("1")));
		assertTrue(table1.knowsSearchRouteTo(new PeerID("3")));
		assertFalse(table1.knowsSearchRouteTo(new PeerID("5")));
		assertFalse(table1.knowsSearchRouteTo(new PeerID("7")));
		assertFalse(table1.knowsSearchRouteTo(new PeerID("2")));
	}

	@Test
	public void testIsRoute() {
		assertTrue(table1.isRoute(searchMessage1.getRemoteMessageID()));
		assertTrue(table1.isRoute(searchMessage2.getRemoteMessageID()));
		assertTrue(table1.isRoute(searchMessage3.getRemoteMessageID()));
		assertTrue(table1.isRoute(searchMessage4.getRemoteMessageID()));

		assertTrue(table1.isRoute(searchResponseMessage1.getRemoteMessageID()));
		assertTrue(table1.isRoute(searchResponseMessage2.getRemoteMessageID()));
		assertTrue(table1.isRoute(searchResponseMessage3.getRemoteMessageID()));
		assertTrue(table1.isRoute(searchResponseMessage4.getRemoteMessageID()));

		assertFalse(table1.isRoute(new MessageID(new PeerID("10"), MessageIDGenerator.getNewID())));
	}

	@Test
	public void testIsLocalRoute() {
		assertFalse(table1.isLocalRoute(searchMessage1.getRemoteMessageID()));
		assertFalse(table1.isLocalRoute(searchMessage2.getRemoteMessageID()));
		assertFalse(table1.isLocalRoute(searchMessage3.getRemoteMessageID()));
		assertFalse(table1.isLocalRoute(searchMessage4.getRemoteMessageID()));

		assertFalse(table1.isLocalRoute(searchResponseMessage1.getRemoteMessageID()));
		assertFalse(table1.isLocalRoute(searchResponseMessage2.getRemoteMessageID()));
		assertFalse(table1.isLocalRoute(searchResponseMessage3.getRemoteMessageID()));
		assertFalse(table1.isLocalRoute(searchResponseMessage4.getRemoteMessageID()));

		assertTrue(table1.isLocalRoute(localSearchMessage1.getRemoteMessageID()));
		assertTrue(table1.isLocalRoute(localSearchMessage2.getRemoteMessageID()));
	}

	@Test
	public void testGetPeersSearching() throws InvalidParameterIDException {
		assertEquals(1, table1.getActiveSearches(ParameterFactory.createParameter("I-A")).size());
		assertTrue(table1.getActiveSearches(ParameterFactory.createParameter("I-A")).contains(searchMessage1));
		assertEquals(1, table1.getActiveSearches(ParameterFactory.createParameter("I-H")).size());
		assertEquals(1, table1.getActiveSearches(ParameterFactory.createParameter("I-I")).size());
		assertTrue(table1.getActiveSearches(ParameterFactory.createParameter("I-H")).contains(searchMessage3));
		assertTrue(table1.getActiveSearches(ParameterFactory.createParameter("I-I")).contains(searchMessage3));
		assertTrue(table1.getActiveSearches(ParameterFactory.createParameter("I-W")).isEmpty());
	}

	@Test
	public void testGetNeighbor() {
		assertEquals(new PeerID("2"), table1.getNeighbor(new PeerID("1")));
		assertEquals(new PeerID("4"), table1.getNeighbor(new PeerID("3")));
		assertEquals(new PeerID("6"), table1.getNeighbor(new PeerID("5")));
		assertEquals(new PeerID("6"), table1.getNeighbor(new PeerID("5")));

		assertEquals(new PeerID("9"), table1.getNeighbor(new PeerID("8")));

		assertEquals(PeerID.VOID_PEERID, table1.getNeighbor(new PeerID("10")));
	}

	@Test
	public void testRemoveRoute() {
		Set<MessageID> routeIDs = table1.getRouteIDs(new PeerID("13"));

		assertTrue(table1.knowsRouteTo(new PeerID("13")));

		routeIDs = table1.getRouteIDs(new PeerID("13"));

		table1.removeRoute(routeIDs.iterator().next(), new PeerID("8"));

		assertTrue(table1.knowsRouteTo(new PeerID("13")));

		routeIDs = table1.getRouteIDs(new PeerID("5"));

		table1.removeRoute(routeIDs.iterator().next(), new PeerID("6"));

		assertFalse(table1.knowsRouteTo(new PeerID("5")));

		routeIDs = table1.getRouteIDs(new PeerID("7"));

		for (final MessageID routeID : routeIDs)
			table1.removeRoute(routeID, new PeerID("6"));

		assertFalse(table1.knowsRouteTo(new PeerID("7")));

		routeIDs = table1.getRouteIDs(new PeerID("13"));

		table1.removeRoute(routeIDs.iterator().next(), new PeerID("14"));

		assertFalse(table1.knowsRouteTo(new PeerID("13")));

		assertTrue(table1.knowsRouteTo(new PeerID("3")));

		routeIDs = table1.getRouteIDs(new PeerID("3"));

		table1.removeRoute(routeIDs.iterator().next(), PeerID.VOID_PEERID);

		assertFalse(table1.knowsRouteTo(new PeerID("3")));
	}

	@Test
	public void testRemoveRouteWithResponseRemoval() {
		table1.removeRoute(searchMessage4.getRemoteMessageID(), new PeerID("4"));

		assertFalse(table1.knowsRouteTo(new PeerID("17")));
		assertFalse(table1.knowsRouteTo(new PeerID("5")));
		assertFalse(table1.knowsRouteTo(new PeerID("7")));
	}

	@Test
	public void testRemoveParameters() throws InvalidParameterIDException {
		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-C")), searchResponseMessage1.getRemoteMessageID());

		assertFalse(table1.knowsRouteTo(new PeerID("5")));
		assertTrue(table1.knowsRouteTo(new PeerID("7")));

		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-C")), searchResponseMessage2.getRemoteMessageID());

		assertTrue(table1.knowsRouteTo(new PeerID("7")));

		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-C")), searchResponseMessage3.getRemoteMessageID());

		assertFalse(table1.knowsRouteTo(new PeerID("7")));

		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-E")), searchResponseMessage4.getRemoteMessageID());

		assertTrue(table1.knowsRouteTo(new PeerID("8")));

		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-F")), searchResponseMessage4.getRemoteMessageID());

		assertFalse(table1.knowsRouteTo(new PeerID("8")));

		assertEquals(7, table1.getActiveSearches().size());

		assertTrue(table1.getActiveSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-H")));
		assertTrue(table1.getActiveSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-I")));

		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-H")), searchMessage3.getRemoteMessageID());

		assertTrue(table1.knowsRouteTo(new PeerID("13")));

		assertEquals(7, table1.getActiveSearches().size());

		assertFalse(table1.getActiveSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-H")));
		assertTrue(table1.getActiveSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-I")));

		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-I")), searchMessage3.getRemoteMessageID());

		assertFalse(table1.knowsRouteTo(new PeerID("13")));

		assertEquals(6, table1.getActiveSearches().size());

		assertNull(table1.getActiveSearch(searchMessage3.getRemoteMessageID()));
		assertNull(table1.getActiveSearch(searchMessage3.getRemoteMessageID()));
	}

	@Test
	public void testRemoveParametersAtOnce() throws InvalidParameterIDException {
		Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-E"));
		parameters.add(ParameterFactory.createParameter("I-F"));

		assertTrue(table1.knowsRouteTo(new PeerID("8")));

		table1.removeParameters(parameters, searchResponseMessage4.getRemoteMessageID());

		assertFalse(table1.knowsRouteTo(new PeerID("8")));

		parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-H"));
		parameters.add(ParameterFactory.createParameter("I-I"));

		assertEquals(7, table1.getActiveSearches().size());

		assertTrue(table1.getActiveSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-H")));
		assertTrue(table1.getActiveSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-I")));

		table1.removeParameters(parameters, searchMessage3.getRemoteMessageID());

		assertNull(table1.getActiveSearch(searchMessage3.getRemoteMessageID()));
		assertNull(table1.getActiveSearch(searchMessage3.getRemoteMessageID()));
	}

	@Test
	public void testXMLSerialization() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		table1.saveToXML(baos);
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		final UnicastTable otherTable = new UnicastTable(PeerID.VOID_PEERID);
		otherTable.readFromXML(bais);

		baos = new ByteArrayOutputStream();
		otherTable.saveToXML(baos);
		baos.close();

		bais = new ByteArrayInputStream(baos.toByteArray());
		final UnicastTable otherTable2 = new UnicastTable(PeerID.VOID_PEERID);
		otherTable2.readFromXML(bais);

		assertEquals(otherTable, otherTable2);
	}
}
