package layers;

import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;

import java.io.FileInputStream;

import taxonomy.BasicTaxonomy;
import testing.BasicTest;
import testing.MultipleTests;

public class GraphCreationTest extends MultipleTests {

	public GraphCreationTest() throws Exception {
		super("files/GraphCreation.xml", new GraphFileFilter());
	}

	@Override
	public Object readObject(final FileInputStream fileInputStream) throws Exception {
		final ExtendedServiceGraph eServiceGraph = new ExtendedServiceGraph(new BasicTaxonomy());
		eServiceGraph.readFromXML(fileInputStream);
		return eServiceGraph;
	}

	@Override
	public void check(final BasicTest test) throws Exception {
	}
}
