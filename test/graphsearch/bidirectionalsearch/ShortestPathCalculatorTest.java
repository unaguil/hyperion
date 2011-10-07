package graphsearch.bidirectionalsearch;

import static org.junit.Assert.assertEquals;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.shortestpathnotificator.ShortestPathCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import peer.peerid.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.parameter.ParameterFactory;

public class ShortestPathCalculatorTest {

	private Service s1, s2, s3, s4, s5, init, goal;

	private Map<Service, Set<ServiceDistance>> serviceDistances;

	private final Taxonomy taxonomy = new BasicTaxonomy();

	@Before
	public void setUp() throws Exception {
		serviceDistances = new HashMap<Service, Set<ServiceDistance>>();

		init = new Service("Composition-INIT", new PeerID("6"));
		init.addParameter(ParameterFactory.createParameter("O-Z"));

		s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-Z"));
		s1.addParameter(ParameterFactory.createParameter("O-A"));
		s1.addParameter(ParameterFactory.createParameter("O-B"));

		s2 = new Service("S2", new PeerID("2"));
		s2.addParameter(ParameterFactory.createParameter("I-A"));
		s2.addParameter(ParameterFactory.createParameter("O-D"));

		s3 = new Service("S3", new PeerID("3"));
		s3.addParameter(ParameterFactory.createParameter("I-B"));
		s3.addParameter(ParameterFactory.createParameter("O-C"));

		s4 = new Service("S4", new PeerID("4"));
		s4.addParameter(ParameterFactory.createParameter("I-C"));
		s4.addParameter(ParameterFactory.createParameter("O-E"));

		s5 = new Service("S5", new PeerID("2"));
		s5.addParameter(ParameterFactory.createParameter("I-E"));
		s5.addParameter(ParameterFactory.createParameter("O-W"));

		goal = new Service("Composition-GOAL", new PeerID("6"));
		goal.addParameter(ParameterFactory.createParameter("I-W"));

		serviceDistances.put(init, new HashSet<ServiceDistance>());
		serviceDistances.get(init).add(new ServiceDistance(s1, Integer.valueOf(0)));

		serviceDistances.put(s1, new HashSet<ServiceDistance>());
		serviceDistances.get(s1).add(new ServiceDistance(s2, Integer.valueOf(5)));
		serviceDistances.get(s1).add(new ServiceDistance(s3, Integer.valueOf(3)));

		serviceDistances.put(s2, new HashSet<ServiceDistance>());
		serviceDistances.get(s2).add(new ServiceDistance(s5, Integer.valueOf(4)));

		serviceDistances.put(s3, new HashSet<ServiceDistance>());
		serviceDistances.get(s3).add(new ServiceDistance(s4, Integer.valueOf(3)));

		serviceDistances.put(s4, new HashSet<ServiceDistance>());
		serviceDistances.get(s4).add(new ServiceDistance(s5, Integer.valueOf(2)));

		serviceDistances.put(s5, new HashSet<ServiceDistance>());
		serviceDistances.get(s5).add(new ServiceDistance(goal, Integer.valueOf(1)));

		serviceDistances.put(goal, new HashSet<ServiceDistance>());
	}

	@Test
	public void testFindClosestPath() throws Exception {
		List<Service> foundPath = ShortestPathCalculator.findShortestPath(serviceDistances, new PeerID("3"), taxonomy);

		List<Service> expected = new ArrayList<Service>();
		expected.add(s3);
		expected.add(s1);
		expected.add(init);

		assertEquals(expected, foundPath);

		foundPath = ShortestPathCalculator.findShortestPath(serviceDistances, new PeerID("4"), taxonomy);

		expected = new ArrayList<Service>();
		expected.add(s4);
		expected.add(s5);
		expected.add(goal);

		assertEquals(expected, foundPath);

		foundPath = ShortestPathCalculator.findShortestPath(serviceDistances, new PeerID("2"), taxonomy);

		expected = new ArrayList<Service>();
		expected.add(s5);
		expected.add(goal);

		assertEquals(expected, foundPath);

		foundPath = ShortestPathCalculator.findShortestPath(serviceDistances, new PeerID("6"), taxonomy);

		expected = new ArrayList<Service>();
		expected.add(init);

		assertEquals(expected, foundPath);
	}
}
