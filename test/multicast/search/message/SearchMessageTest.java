package multicast.search.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multicast.SearchedParameter;
import multicast.search.message.SearchMessage.SearchType;

import org.junit.Test;

import peer.peerid.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.TaxonomyException;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class SearchMessageTest {
	
	private final static Set<PeerID> emptySet = Collections.<PeerID> emptySet();

	@Test
	public void testDecTTL() throws InvalidParameterIDException {
		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-A"), 3));
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-B"), 3));
		final SearchMessage searchMessage = new SearchMessage(new PeerID("0"), emptySet, searchedParameters, null, 3, SearchType.Exact);

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
	public void testRemoveParameters() throws InvalidParameterIDException {
		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-A"), 3));
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-B"), 3));
		final SearchMessage searchMessage = new SearchMessage(new PeerID("0"), emptySet, searchedParameters, null, 3, SearchType.Exact);

		assertTrue(searchMessage.hasTTL());
		
		final Set<Parameter> parameters = new HashSet<Parameter>();
		for (final SearchedParameter searchedParameter : searchedParameters)
			parameters.add(searchedParameter.getParameter());

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

		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-B"), 3));
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-C"), 3));
		final SearchMessage searchMessage = new SearchMessage(new PeerID("0"), emptySet, searchedParameters, null, 3, SearchType.Generic);

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
