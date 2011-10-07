package multicast.search.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multicast.search.message.SearchMessage.SearchType;

import org.junit.Test;

import peer.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.TaxonomyException;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class SearchMessageTest {

	@Test
	public void testDecTTL() throws InvalidParameterIDException {
		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-A"));
		parameters.add(ParameterFactory.createParameter("I-B"));
		final SearchMessage searchMessage = new SearchMessage(parameters, null, new PeerID("0"), 3, 3, SearchType.Exact);

		assertTrue(searchMessage.hasTTL());

		searchMessage.decTTL(ParameterFactory.createParameter("I-A"));
		assertEquals(2, searchMessage.getTTL(ParameterFactory.createParameter("I-A")));
		searchMessage.decTTL(ParameterFactory.createParameter("I-A"));
		assertEquals(1, searchMessage.getTTL(ParameterFactory.createParameter("I-A")));
		searchMessage.decTTL(ParameterFactory.createParameter("I-A"));
		assertEquals(0, searchMessage.getTTL(ParameterFactory.createParameter("I-A")));

		assertTrue(searchMessage.hasTTL());

		searchMessage.decTTL(ParameterFactory.createParameter("I-B"));
		assertEquals(2, searchMessage.getTTL(ParameterFactory.createParameter("I-B")));
		searchMessage.decTTL(ParameterFactory.createParameter("I-B"));
		assertEquals(1, searchMessage.getTTL(ParameterFactory.createParameter("I-B")));
		searchMessage.decTTL(ParameterFactory.createParameter("I-B"));
		assertEquals(0, searchMessage.getTTL(ParameterFactory.createParameter("I-B")));

		assertFalse(searchMessage.hasTTL());
	}

	@Test
	public void testRestoreTTL() throws InvalidParameterIDException {
		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-A"));
		parameters.add(ParameterFactory.createParameter("I-B"));
		final SearchMessage searchMessage = new SearchMessage(parameters, null, new PeerID("0"), 3, 3, SearchType.Exact);

		searchMessage.decTTL(ParameterFactory.createParameter("I-A"));
		searchMessage.decTTL(ParameterFactory.createParameter("I-A"));

		searchMessage.decTTL(ParameterFactory.createParameter("I-B"));
		searchMessage.decTTL(ParameterFactory.createParameter("I-B"));
		searchMessage.decTTL(ParameterFactory.createParameter("I-B"));

		searchMessage.restoreTTL(ParameterFactory.createParameter("I-A"), 3);

		assertEquals(3, searchMessage.getTTL(ParameterFactory.createParameter("I-A")));
		assertEquals(0, searchMessage.getTTL(ParameterFactory.createParameter("I-B")));

		assertTrue(searchMessage.hasTTL());
	}

	@Test
	public void testPreviousDistance() throws InvalidParameterIDException {
		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-A"));
		parameters.add(ParameterFactory.createParameter("I-B"));
		final SearchMessage searchMessage = new SearchMessage(parameters, null, new PeerID("0"), 3, 3, SearchType.Exact);

		assertEquals(0, searchMessage.getPreviousDistance(ParameterFactory.createParameter("I-A")));

		searchMessage.setCurrentDistance(ParameterFactory.createParameter("I-A"), 3);

		assertEquals(3, searchMessage.getPreviousDistance(ParameterFactory.createParameter("I-A")));
	}

	@Test
	public void testRemoveParameters() throws InvalidParameterIDException {
		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-A"));
		parameters.add(ParameterFactory.createParameter("I-B"));
		final SearchMessage searchMessage = new SearchMessage(parameters, null, new PeerID("0"), 3, 3, SearchType.Exact);

		assertTrue(searchMessage.hasTTL());

		searchMessage.removeParameters(parameters);

		assertFalse(searchMessage.hasTTL());
	}

	@Test
	public void testGeneralizeParameters() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-B"));
		parameters.add(ParameterFactory.createParameter("I-C"));
		final SearchMessage searchMessage = new SearchMessage(parameters, null, new PeerID("0"), 3, 3, SearchType.Generic);

		assertEquals(2, searchMessage.getSearchedParameters().size());
		assertTrue(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-B")));
		assertTrue(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-C")));

		Set<Parameter> generalizations = new HashSet<Parameter>();
		generalizations.add(ParameterFactory.createParameter("I-A"));
		Map<Parameter, Parameter> results = searchMessage.generalizeParameters(generalizations, taxonomy);

		assertEquals(2, searchMessage.getSearchedParameters().size());
		assertTrue(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-A")));
		assertTrue(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-C")));
		assertFalse(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-B")));

		generalizations = new HashSet<Parameter>();
		generalizations.add(ParameterFactory.createParameter("I-A"));
		results = searchMessage.generalizeParameters(generalizations, taxonomy);

		assertTrue(results.isEmpty());
	}
}
