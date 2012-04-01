package graphcreation.graph.extendedServiceGraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import graphcreation.graph.extendedServiceGraph.node.ConnectionNode;
import graphcreation.graph.servicegraph.node.ServiceNode;
import graphcreation.services.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import peer.peerid.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.TaxonomyException;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.ParameterFactory;

public class ExtendedServiceGraphTest {

	private ExtendedServiceGraph eServiceGraph;

	private ServiceNode s1Node, s2Node, s3Node, s4Node;

	private Taxonomy taxonomy;

	@Before
	public void setUp() throws TaxonomyException, InvalidParameterIDException {
		taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		eServiceGraph = new ExtendedServiceGraph(taxonomy);

		final Service s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-C", taxonomy));
		s1.addParameter(ParameterFactory.createParameter("O-B", taxonomy));

		s1Node = eServiceGraph.merge(s1);

		final Service s5 = new Service("S5", new PeerID("1"));
		s5.addParameter(ParameterFactory.createParameter("I-1", taxonomy));
		s5.addParameter(ParameterFactory.createParameter("O-B", taxonomy));

		eServiceGraph.merge(s5);

		final Service s2 = new Service("S2", new PeerID("1"));
		s2.addParameter(ParameterFactory.createParameter("I-A", taxonomy));
		s2.addParameter(ParameterFactory.createParameter("O-2", taxonomy));

		s2Node = eServiceGraph.merge(s2);

		final Service s3 = new Service("S3", new PeerID("1"));
		s3.addParameter(ParameterFactory.createParameter("I-B", taxonomy));
		s3.addParameter(ParameterFactory.createParameter("O-Z", taxonomy));

		s3Node = eServiceGraph.merge(s3);

		final Service s4 = new Service("S4", new PeerID("1"));
		s4.addParameter(ParameterFactory.createParameter("I-2", taxonomy));
		s4.addParameter(ParameterFactory.createParameter("O-3", taxonomy));

		s4Node = eServiceGraph.merge(s4);
	}

	@Test
	public void addService() {
		Set<ServiceNode> successors = eServiceGraph.getSuccessors(s1Node, false);
		assertEquals(2, successors.size());
		assertTrue(successors.contains(s2Node));
		assertTrue(successors.contains(s3Node));

		successors = eServiceGraph.getSuccessors(s2Node, false);
		assertEquals(1, successors.size());
		assertTrue(successors.contains(s4Node));

		assertTrue(eServiceGraph.getSuccessors(s3Node, false).isEmpty());

		assertTrue(eServiceGraph.getSuccessors(s4Node, false).isEmpty());

		assertEquals(5, eServiceGraph.andNodeSet().size());
		assertEquals(3, eServiceGraph.orNodeSet().size());
	}

	@Test
	public void removeService() throws InvalidParameterIDException {
		final Service s1 = new Service("S1", new PeerID("1"));
		eServiceGraph.removeService(s1);

		final Service s5 = new Service("S5", new PeerID("1"));
		eServiceGraph.removeService(s5);

		assertFalse(eServiceGraph.andNodeSet().contains(s1Node));
		assertFalse(eServiceGraph.orNodeSet().contains(new ConnectionNode((OutputParameter)ParameterFactory.createParameter("O-B", taxonomy), (InputParameter)ParameterFactory.createParameter("I-A", taxonomy), taxonomy)));

		final Set<ServiceNode> successors = eServiceGraph.getSuccessors(s2Node, false);
		assertEquals(1, successors.size());
		assertTrue(successors.contains(s4Node));

		assertTrue(eServiceGraph.getSuccessors(s3Node, false).isEmpty());

		assertTrue(eServiceGraph.getSuccessors(s4Node, false).isEmpty());

		assertEquals(3, eServiceGraph.andNodeSet().size());
		assertEquals(1, eServiceGraph.orNodeSet().size());
	}

	@Test
	public void testXML() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		eServiceGraph.saveToXML(baos);
		baos.close();

		final ExtendedServiceGraph eServiceGraph2 = new ExtendedServiceGraph(taxonomy);
		final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		eServiceGraph2.readFromXML(bais);
		bais.close();

		assertEquals(eServiceGraph, eServiceGraph2);
	}

	@Test
	public void testEquals() throws InvalidParameterIDException {
		final ExtendedServiceGraph eServiceGraph2 = new ExtendedServiceGraph(taxonomy);

		Service s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-C", taxonomy));
		s1.addParameter(ParameterFactory.createParameter("O-B", taxonomy));

		eServiceGraph2.merge(s1);

		Service s2 = new Service("S2", new PeerID("1"));
		s2.addParameter(ParameterFactory.createParameter("I-A", taxonomy));
		s2.addParameter(ParameterFactory.createParameter("O-2", taxonomy));

		eServiceGraph2.merge(s2);

		final Service s3 = new Service("S3", new PeerID("1"));
		s3.addParameter(ParameterFactory.createParameter("I-B", taxonomy));
		s3.addParameter(ParameterFactory.createParameter("O-Z", taxonomy));

		eServiceGraph2.merge(s3);

		Service s4 = new Service("S4", new PeerID("1"));
		s4.addParameter(ParameterFactory.createParameter("I-2", taxonomy));
		s4.addParameter(ParameterFactory.createParameter("O-3", taxonomy));

		eServiceGraph2.merge(s4);

		final Service s5 = new Service("S5", new PeerID("1"));
		s5.addParameter(ParameterFactory.createParameter("I-1", taxonomy));
		s5.addParameter(ParameterFactory.createParameter("O-B", taxonomy));

		eServiceGraph2.merge(s5);

		assertEquals(eServiceGraph, eServiceGraph2);
		assertEquals(eServiceGraph, eServiceGraph);

		final ExtendedServiceGraph eServiceGraph3 = new ExtendedServiceGraph(taxonomy);

		s1 = new Service("S1", new PeerID("1"));
		s1.addParameter(ParameterFactory.createParameter("I-C", taxonomy));
		s1.addParameter(ParameterFactory.createParameter("O-B", taxonomy));

		eServiceGraph3.merge(s1);

		s2 = new Service("S2", new PeerID("1"));
		s2.addParameter(ParameterFactory.createParameter("I-A", taxonomy));
		s2.addParameter(ParameterFactory.createParameter("O-2", taxonomy));

		eServiceGraph3.merge(s2);

		s4 = new Service("S4", new PeerID("1"));
		s4.addParameter(ParameterFactory.createParameter("I-2", taxonomy));
		s4.addParameter(ParameterFactory.createParameter("O-3", taxonomy));

		eServiceGraph3.merge(s4);

		assertFalse(eServiceGraph.equals(eServiceGraph3));
	}
}
