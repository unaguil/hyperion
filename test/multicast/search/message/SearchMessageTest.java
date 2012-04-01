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
	
	private final Taxonomy emptyTaxonomy = new BasicTaxonomy();

	@Test
	public void testDecTTL() throws InvalidParameterIDException {
		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-1", emptyTaxonomy), 3));
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-2", emptyTaxonomy), 3));
		final SearchMessage searchMessage = new SearchMessage(new PeerID("0"), emptySet, searchedParameters, null, 3, SearchType.Exact);

		assertTrue(searchMessage.hasTTL());

		searchMessage.decTTL(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		assertEquals(2, searchMessage.getTTL(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		searchMessage.decTTL(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		assertEquals(1, searchMessage.getTTL(ParameterFactory.createParameter("I-1", emptyTaxonomy)));
		searchMessage.decTTL(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		assertEquals(0, searchMessage.getTTL(ParameterFactory.createParameter("I-1", emptyTaxonomy)));

		assertTrue(searchMessage.hasTTL());

		searchMessage.decTTL(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		assertEquals(2, searchMessage.getTTL(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
		searchMessage.decTTL(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		assertEquals(1, searchMessage.getTTL(ParameterFactory.createParameter("I-2", emptyTaxonomy)));
		searchMessage.decTTL(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		assertEquals(0, searchMessage.getTTL(ParameterFactory.createParameter("I-2", emptyTaxonomy)));

		assertFalse(searchMessage.hasTTL());
	}

	@Test
	public void testRemoveParameters() throws InvalidParameterIDException {
		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-1", emptyTaxonomy), 3));
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-2", emptyTaxonomy), 3));
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
		taxonomy.addChild("Z", "1");
		taxonomy.addChild("Z", "3");
		taxonomy.addChild("1", "2");

		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-2", taxonomy), 3));
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-3", taxonomy), 3));
		final SearchMessage searchMessage = new SearchMessage(new PeerID("0"), emptySet, searchedParameters, null, 3, SearchType.Generic);

		assertEquals(2, searchMessage.getSearchedParameters().size());
		assertTrue(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-2", taxonomy)));
		assertTrue(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-3", taxonomy)));

		Set<Parameter> generalizations = new HashSet<Parameter>();
		generalizations.add(ParameterFactory.createParameter("I-1", taxonomy));
		Map<Parameter, Parameter> results = searchMessage.generalizeParameters(generalizations, taxonomy);

		assertEquals(2, searchMessage.getSearchedParameters().size());
		assertTrue(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-1", taxonomy)));
		assertTrue(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-3", taxonomy)));
		assertFalse(searchMessage.getSearchedParameters().contains(ParameterFactory.createParameter("I-2", taxonomy)));

		generalizations = new HashSet<Parameter>();
		generalizations.add(ParameterFactory.createParameter("I-1", taxonomy));
		results = searchMessage.generalizeParameters(generalizations, taxonomy);

		assertTrue(results.isEmpty());
	}
}
