package layers;

import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;

import java.io.FileInputStream;

import taxonomy.BasicTaxonomy;
import testing.BasicTest;
import testing.MultipleTests;

public class BidirectionalSearchTest extends MultipleTests {

	public BidirectionalSearchTest() throws Exception {
		super("files/BidirectionalSearch.xml", new GraphFileFilter());
	}

	@Override
	public Object readObject(final FileInputStream fileInputStream) throws Exception {
		final ExtendedServiceGraph graph = new ExtendedServiceGraph(new BasicTaxonomy());
		graph.readFromXML(fileInputStream);
		return graph;
	}

	@Override
	public void check(final BasicTest test) throws Exception {
	}
}
