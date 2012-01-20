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
import java.util.List;
import java.util.Set;

import multicast.SearchedParameter;
import multicast.search.message.SearchMessage;
import multicast.search.message.SearchMessage.SearchType;
import multicast.search.message.SearchResponseMessage;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.TaxonomyException;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;
import detection.NeighborDetector;
import detection.NeighborEventsListener;

public class UnicastTableTest {

	private UnicastTable table1, table2, table3;

	private SearchResponseMessage searchResponseMessage1, searchResponseMessage2, searchResponseMessage3, searchResponseMessage4;

	private SearchMessage searchMessage1, searchMessage2, searchMessage3, searchMessage4, localSearchMessage1, localSearchMessage2;
	
	private static class DummyNeighborDetector implements NeighborDetector {

		@Override
		public void init() {}

		@Override
		public void stop() {}

		@Override
		public boolean merge(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
			return false;
		}

		@Override
		public void messageReceived(BroadcastMessage message, long receptionTime) {}

		@Override
		public PeerIDSet getCurrentNeighbors() {
			return new PeerIDSet();
		}

		@Override
		public void addNeighborListener(NeighborEventsListener listener) {}
		
	}
	
	private static final NeighborDetector nDetector = new DummyNeighborDetector();
	
	private final Taxonomy emptyTaxonomy = new BasicTaxonomy();
	
	private static final Taxonomy taxonomy = new BasicTaxonomy();
	
	@BeforeClass
	public static void onlyOnce() throws TaxonomyException {
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("A", "B");
		taxonomy.addChild("B", "E");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("Z", "D");
	}

	@Before
	public void setUp() throws Exception, InvalidParameterIDException {
		table1 = new UnicastTable(PeerID.VOID_PEERID, nDetector);
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

		table1.updateUnicastTable(searchMessage1, emptyTaxonomy);
		table1.updateUnicastTable(searchMessage2, emptyTaxonomy);
		table1.updateUnicastTable(searchMessage3, emptyTaxonomy);
		table1.updateUnicastTable(searchMessage4, emptyTaxonomy);
		table1.updateUnicastTable(searchMessage5, emptyTaxonomy);
		table1.updateUnicastTable(searchResponseMessage1);
		table1.updateUnicastTable(searchResponseMessage2);
		table1.updateUnicastTable(searchResponseMessage3);
		table1.updateUnicastTable(searchResponseMessage4);
		table1.updateUnicastTable(localSearchResponseMessage1);
		table1.updateUnicastTable(localSearchResponseMessage2);
		table1.updateUnicastTable(localSearchMessage1, emptyTaxonomy);
		table1.updateUnicastTable(localSearchMessage2, emptyTaxonomy);

		table2 = new UnicastTable(PeerID.VOID_PEERID, nDetector);
		table2.updateUnicastTable(createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-D")), new PeerID("1"), new PeerID("2")), emptyTaxonomy);
		table2.updateUnicastTable(createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-B")), new PeerID("3"), new PeerID("3")), emptyTaxonomy);
		table2.updateUnicastTable(createSearchResponseMessage(Collections.singleton(ParameterFactory.createParameter("I-C")), new PeerID("5"), new PeerID("6"), searchMessage4));

		table3 = new UnicastTable(PeerID.VOID_PEERID, nDetector);
		table3.updateUnicastTable(searchMessage1, emptyTaxonomy);
		table3.updateUnicastTable(searchMessage2, emptyTaxonomy);
		table3.updateUnicastTable(searchMessage3, emptyTaxonomy);
		table3.updateUnicastTable(searchMessage4, emptyTaxonomy);
		table3.updateUnicastTable(searchMessage5, emptyTaxonomy);
		table3.updateUnicastTable(searchResponseMessage1);
		table3.updateUnicastTable(searchResponseMessage2);
		table3.updateUnicastTable(searchResponseMessage3);
		table3.updateUnicastTable(searchResponseMessage4);
		table3.updateUnicastTable(localSearchResponseMessage1);
		table3.updateUnicastTable(localSearchResponseMessage2);
		table3.updateUnicastTable(localSearchMessage1, emptyTaxonomy);
		table3.updateUnicastTable(localSearchMessage2, emptyTaxonomy);
	}

	private SearchMessage createSearchMessage(final Set<Parameter> parameters, final PeerID source, final PeerID sender) {
		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		for (final Parameter p : parameters) 
			searchedParameters.add(new SearchedParameter(p, 0));
		
		final SearchMessage searchMessage = new SearchMessage(source, Collections.<PeerID> emptySet(), searchedParameters, null, 0, SearchType.Exact);

		return new SearchMessage(searchMessage, sender, Collections.<PeerID> emptySet(), 0);
	}

	private SearchResponseMessage createSearchResponseMessage(final Set<Parameter> parameters, final PeerID source, final PeerID sender, final SearchMessage searchMessage) {
		final SearchResponseMessage searchResponseMessage = new SearchResponseMessage(source, PeerID.VOID_PEERID, parameters, null, searchMessage.getRemoteMessageID());

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
	public void testGetActiveSearches() {
		assertEquals(7, table1.getActiveSearches().size());

		Set<MessageID> routeIDs = table1.getRouteIDs(new PeerID("1"));
		assertTrue(table1.cancelSearch(routeIDs.iterator().next(), new PeerID("2"), emptyTaxonomy).wasRemoved());

		routeIDs = table1.getRouteIDs(new PeerID("5"));
		table1.removeRoute(routeIDs.iterator().next());

		routeIDs = table1.getRouteIDs(new PeerID("7"));
		table1.removeRoute(routeIDs.iterator().next());

		assertEquals(6, table1.getActiveSearches().size());

		routeIDs = table1.getRouteIDs(new PeerID("3"));
		assertTrue(table1.cancelSearch(routeIDs.iterator().next(), new PeerID("4"), emptyTaxonomy).wasRemoved());

		assertEquals(5, table1.getActiveSearches().size());

		routeIDs = table1.getRouteIDs(new PeerID("13"));
		assertTrue(table1.cancelSearch(routeIDs.iterator().next(), new PeerID("14"), emptyTaxonomy).wasRemoved());

		assertEquals(4, table1.getActiveSearches().size());

		routeIDs = table1.getRouteIDs(new PeerID("17"));
		assertTrue(table1.cancelSearch(routeIDs.iterator().next(), new PeerID("4"), emptyTaxonomy).wasRemoved());

		assertEquals(3, table1.getActiveSearches().size());

		routeIDs = table1.getRouteIDs(new PeerID("18"));
		assertTrue(table1.cancelSearch(routeIDs.iterator().next(), new PeerID("4"), emptyTaxonomy).wasRemoved());

		assertEquals(2, table1.getActiveSearches().size());
	}

	@Test
	public void testEquals() {
		assertEquals(table1, table1);
		assertEquals(table1, table3);

		assertFalse(table1.equals(table2));

		assertEquals(new UnicastTable(PeerID.VOID_PEERID, nDetector), new UnicastTable(PeerID.VOID_PEERID, nDetector));
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
	public void testGetPeersSearching() throws InvalidParameterIDException {
		assertEquals(1, table1.getSearches(ParameterFactory.createParameter("I-A"), taxonomy).size());
		assertTrue(table1.getSearches(ParameterFactory.createParameter("I-A"), taxonomy).contains(searchMessage1));
		assertEquals(1, table1.getSearches(ParameterFactory.createParameter("I-H"), taxonomy).size());
		assertEquals(1, table1.getSearches(ParameterFactory.createParameter("I-I"), taxonomy).size());
		assertTrue(table1.getSearches(ParameterFactory.createParameter("I-H"), taxonomy).contains(searchMessage3));
		assertTrue(table1.getSearches(ParameterFactory.createParameter("I-I"), taxonomy).contains(searchMessage3));
		assertTrue(table1.getSearches(ParameterFactory.createParameter("I-W"), taxonomy).isEmpty());
	}

	@Test
	public void testGetNeighbor() {
		assertEquals(new PeerID("2"), table1.getRoute(new PeerID("1")).getThrough());
		assertEquals(new PeerID("4"), table1.getRoute(new PeerID("3")).getThrough());
		assertEquals(new PeerID("6"), table1.getRoute(new PeerID("5")).getThrough());
		assertEquals(new PeerID("6"), table1.getRoute(new PeerID("5")).getThrough());

		assertEquals(new PeerID("9"), table1.getRoute(new PeerID("8")).getThrough());

		assertNull(table1.getRoute(new PeerID("10")));
	}

	@Test
	public void testRemoveRoute() {
		Set<MessageID> routeIDs = table1.getRouteIDs(new PeerID("13"));

		assertTrue(table1.knowsRouteTo(new PeerID("13")));
		routeIDs = table1.getRouteIDs(new PeerID("13"));
		table1.cancelSearch(routeIDs.iterator().next(), new PeerID("8"), emptyTaxonomy);
		assertTrue(table1.knowsRouteTo(new PeerID("13")));

		routeIDs = table1.getRouteIDs(new PeerID("5"));
		table1.removeRoute(routeIDs.iterator().next());
		assertFalse(table1.knowsRouteTo(new PeerID("5")));

		routeIDs = table1.getRouteIDs(new PeerID("7"));

		for (final MessageID routeID : routeIDs) {
			if (table1.isSearchRoute(routeID))
				table1.cancelSearch(routeID, new PeerID("6"), emptyTaxonomy);
			else 
				table1.removeRoute(routeID);
		}

		assertFalse(table1.knowsRouteTo(new PeerID("7")));

		routeIDs = table1.getRouteIDs(new PeerID("13"));
		table1.removeRoute(routeIDs.iterator().next());
		assertFalse(table1.knowsRouteTo(new PeerID("13")));

		assertTrue(table1.knowsRouteTo(new PeerID("3")));
		routeIDs = table1.getRouteIDs(new PeerID("3"));
		table1.removeRoute(routeIDs.iterator().next());
		assertFalse(table1.knowsRouteTo(new PeerID("3")));
	}

	@Test
	public void testRemoveSearchRouteResponseNoteRemoved() {
		table1.removeRoute(searchMessage4.getRemoteMessageID());

		assertFalse(table1.knowsRouteTo(new PeerID("17")));
		assertTrue(table1.knowsRouteTo(new PeerID("5")));
		assertTrue(table1.knowsRouteTo(new PeerID("7")));
	}

	@Test
	public void testRemoveParameters() throws InvalidParameterIDException {
		assertEquals(7, table1.getActiveSearches().size());

		assertTrue(table1.getSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-H")));
		assertTrue(table1.getSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-I")));

		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-H")), searchMessage3.getRemoteMessageID(), emptyTaxonomy);

		assertTrue(table1.knowsRouteTo(new PeerID("13")));

		assertEquals(7, table1.getActiveSearches().size());

		assertFalse(table1.getSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-H")));
		assertTrue(table1.getSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-I")));

		table1.removeParameters(Collections.singleton(ParameterFactory.createParameter("I-I")), searchMessage3.getRemoteMessageID(), emptyTaxonomy);

		assertTrue(table1.knowsRouteTo(new PeerID("13")));

		assertEquals(6, table1.getActiveSearches().size());

		assertNull(table1.getSearch(searchMessage3.getRemoteMessageID()));
		assertNull(table1.getSearch(searchMessage3.getRemoteMessageID()));
	}

	@Test
	public void testRemoveParametersAtOnce() throws InvalidParameterIDException {
		Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-E"));
		parameters.add(ParameterFactory.createParameter("I-F"));

		assertTrue(table1.knowsRouteTo(new PeerID("8")));

		parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-H"));
		parameters.add(ParameterFactory.createParameter("I-I"));

		assertEquals(7, table1.getActiveSearches().size());

		assertTrue(table1.getSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-H")));
		assertTrue(table1.getSearch(searchMessage3.getRemoteMessageID()).getSearchedParameters().contains(ParameterFactory.createParameter("I-I")));

		table1.removeParameters(parameters, searchMessage3.getRemoteMessageID(), emptyTaxonomy);

		assertNull(table1.getSearch(searchMessage3.getRemoteMessageID()));
		assertNull(table1.getSearch(searchMessage3.getRemoteMessageID()));
	}

	@Test
	public void testXMLSerialization() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		table1.saveToXML(baos);
		baos.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		final UnicastTable otherTable = new UnicastTable(PeerID.VOID_PEERID, nDetector);
		otherTable.readFromXML(bais);

		baos = new ByteArrayOutputStream();
		otherTable.saveToXML(baos);
		baos.close();

		bais = new ByteArrayInputStream(baos.toByteArray());
		final UnicastTable otherTable2 = new UnicastTable(PeerID.VOID_PEERID, nDetector);
		otherTable2.readFromXML(bais);

		assertEquals(otherTable, otherTable2);
	}
	
	@Test
	public void testAddAssociatedSearchEquals() throws InvalidParameterIDException {		
		final SearchMessage testSearchMessage1 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("1"), new PeerID("2"));
		final SearchMessage testSearchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		
		assertEquals(1, uTable.getActiveSearches().size());
		assertEquals(2, uTable.getSearches().size());
		
		assertEquals(1, uTable.getAssociatedSearches(testSearchMessage1).size());
		assertTrue(uTable.getAssociatedSearches(testSearchMessage2).isEmpty());
	}
	
	@Test
	public void testAddAssociatedSearchNotEquals() throws InvalidParameterIDException {		
		final SearchMessage testSearchMessage1 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("1"), new PeerID("2"));
		final SearchMessage testSearchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-C")), new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		
		assertEquals(2, uTable.getActiveSearches().size());
		assertEquals(2, uTable.getSearches().size());
		
		assertTrue(uTable.getAssociatedSearches(testSearchMessage1).isEmpty());
		assertTrue(uTable.getAssociatedSearches(testSearchMessage2).isEmpty());
	}
	
	@Test
	public void testAddAssociatedSearchSubsumedByFirstOne() throws InvalidParameterIDException {		
		final SearchMessage testSearchMessage1 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("1"), new PeerID("2"));
		final SearchMessage testSearchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-B")), new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		
		assertEquals(1, uTable.getActiveSearches().size());
		assertEquals(2, uTable.getSearches().size());
		
		assertEquals(1, uTable.getAssociatedSearches(testSearchMessage1).size());
		assertTrue(uTable.getAssociatedSearches(testSearchMessage2).isEmpty());
	}
	
	@Test
	public void testAddAssociatedSearchSubsumesFirstOne() throws InvalidParameterIDException {		
		final SearchMessage testSearchMessage1 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-B")), new PeerID("1"), new PeerID("2"));
		final SearchMessage testSearchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		
		assertEquals(2, uTable.getActiveSearches().size());
		assertEquals(2, uTable.getSearches().size());
		
		assertTrue(uTable.getAssociatedSearches(testSearchMessage1).isEmpty());
		assertTrue(uTable.getAssociatedSearches(testSearchMessage2).isEmpty());
	}
	
	@Test
	public void testAddAssociatedSearchShorter() throws InvalidParameterIDException {		
		final Set<Parameter> firstSearchParameters = new HashSet<Parameter>();
		firstSearchParameters.add(ParameterFactory.createParameter("I-A"));
		firstSearchParameters.add(ParameterFactory.createParameter("I-C"));
		final SearchMessage testSearchMessage1 = createSearchMessage(firstSearchParameters, new PeerID("1"), new PeerID("2"));
		
		final SearchMessage testSearchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-B")), new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		
		assertEquals(1, uTable.getActiveSearches().size());
		assertEquals(2, uTable.getSearches().size());
		
		assertEquals(1, uTable.getAssociatedSearches(testSearchMessage1).size());
		assertTrue(uTable.getAssociatedSearches(testSearchMessage2).isEmpty());
	}
	
	@Test
	public void testAddAssociatedSearchLarger() throws InvalidParameterIDException {		
		final Set<Parameter> firstSearchParameters = new HashSet<Parameter>();
		firstSearchParameters.add(ParameterFactory.createParameter("I-A"));
		firstSearchParameters.add(ParameterFactory.createParameter("I-C"));
		final SearchMessage testSearchMessage1 = createSearchMessage(firstSearchParameters, new PeerID("1"), new PeerID("2"));
		
		final Set<Parameter> secondSearchParameters = new HashSet<Parameter>();
		secondSearchParameters.add(ParameterFactory.createParameter("I-B"));
		secondSearchParameters.add(ParameterFactory.createParameter("I-C"));
		secondSearchParameters.add(ParameterFactory.createParameter("I-D"));
		final SearchMessage testSearchMessage2 = createSearchMessage(secondSearchParameters, new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		
		assertEquals(2, uTable.getActiveSearches().size());
		assertEquals(2, uTable.getSearches().size());
		
		assertTrue(uTable.getAssociatedSearches(testSearchMessage1).isEmpty());
		assertTrue(uTable.getAssociatedSearches(testSearchMessage2).isEmpty());
	}
	
	@Test
	public void testAddAssociatedSearchMultiple() throws InvalidParameterIDException {		
		final Set<Parameter> firstSearchParameters = new HashSet<Parameter>();
		firstSearchParameters.add(ParameterFactory.createParameter("I-B"));
		final SearchMessage testSearchMessage1 = createSearchMessage(firstSearchParameters, new PeerID("1"), new PeerID("2"));
		
		final Set<Parameter> secondSearchParameters = new HashSet<Parameter>();
		secondSearchParameters.add(ParameterFactory.createParameter("I-A"));
		final SearchMessage testSearchMessage2 = createSearchMessage(secondSearchParameters, new PeerID("1"), new PeerID("2"));
		
		final Set<Parameter> thirdSearchParameters = new HashSet<Parameter>();
		thirdSearchParameters.add(ParameterFactory.createParameter("I-E"));
		final SearchMessage testSearchMessage3 = createSearchMessage(thirdSearchParameters, new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		uTable.updateUnicastTable(testSearchMessage3, taxonomy);
		
		assertEquals(2, uTable.getActiveSearches().size());
		assertEquals(3, uTable.getSearches().size());
		
		assertEquals(1, uTable.getAssociatedSearches(testSearchMessage1).size());
		assertEquals(1, uTable.getAssociatedSearches(testSearchMessage2).size());
		assertTrue(uTable.getAssociatedSearches(testSearchMessage3).isEmpty());
	}
	
	@Test
	public void testCancelSearchNewActiveSearches() throws InvalidParameterIDException {
		final SearchMessage testSearchMessage1 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("1"), new PeerID("2"));
		final SearchMessage testSearchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		
		SearchRemovalResult searchRemovalResult = uTable.cancelSearch(uTable.getRouteIDs(new PeerID("1")).iterator().next(), new PeerID("2"), taxonomy);
		assertTrue(searchRemovalResult.wasRemoved());
		assertEquals(Collections.singleton(testSearchMessage2), searchRemovalResult.getNewActiveSearches());
		
		assertEquals(1, uTable.getActiveSearches().size());
		assertEquals(1, uTable.getSearches().size());
		
		assertTrue(uTable.getAssociatedSearches(testSearchMessage2).isEmpty());
	}
	
	@Test
	public void testCancelAssociatedSearch() throws InvalidParameterIDException {
		final SearchMessage testSearchMessage1 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("1"), new PeerID("2"));
		final SearchMessage testSearchMessage2 = createSearchMessage(Collections.singleton(ParameterFactory.createParameter("I-A")), new PeerID("3"), new PeerID("4"));
		
		final UnicastTable uTable = new UnicastTable(new PeerID("0"), nDetector);
		
		uTable.updateUnicastTable(testSearchMessage1, taxonomy);
		uTable.updateUnicastTable(testSearchMessage2, taxonomy);
		
		assertEquals(1, uTable.getActiveSearches().size());
		assertEquals(2, uTable.getSearches().size());
		
		assertEquals(1, uTable.getAssociatedSearches(testSearchMessage1).size());
		assertTrue(uTable.getAssociatedSearches(testSearchMessage2).isEmpty());
		
		SearchRemovalResult searchRemovalResult = uTable.cancelSearch(uTable.getRouteIDs(new PeerID("3")).iterator().next(), new PeerID("4"), taxonomy);
		assertTrue(searchRemovalResult.wasRemoved());
		assertTrue(searchRemovalResult.getNewActiveSearches().isEmpty());
		
		assertEquals(1, uTable.getActiveSearches().size());
		assertEquals(1, uTable.getSearches().size());
		
		assertTrue(uTable.getAssociatedSearches(testSearchMessage1).isEmpty());
	}

}
