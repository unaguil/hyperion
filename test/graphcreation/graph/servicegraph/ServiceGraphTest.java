package graphcreation.graph.servicegraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

public class ServiceGraphTest {

	private static final String GRAPH_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<graphml>" + "<graph edgedefault=\"directed\">" + "<node id=\"Service:S1:1\"/>" + "<node id=\"Parameter:A\"/>" + "<node id=\"Parameter:B\"/>"
			+ "<edge id=\"1\" source=\"Parameter:A\" target=\"Service:S1:1\"/>" + "<edge id=\"2\" source=\"Service:S1:1\" target=\"Parameter:B\"/>" + "<node id=\"Service:S2:2\"/>" + "<node id=\"Parameter:B\"/>" + "<node id=\"Parameter:C\"/>"
			+ "<edge id=\"3\" source=\"Parameter:B\" target=\"Service:S2:2\"/>" + "<edge id=\"4\" source=\"Service:S2:2\" target=\"Parameter:C\"/>" + "<node id=\"Service:S1:4\"/>" + "<node id=\"Parameter:A\"/>" + "<node id=\"Parameter:B\"/>"
			+ "<edge id=\"5\" source=\"Parameter:A\" target=\"Service:S1:4\"/>" + "<edge id=\"6\" source=\"Service:S1:4\" target=\"Parameter:B\"/>" + "</graph>" + "</graphml>";

	private static final String GRAPH_XML2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<graphml>" + "<graph edgedefault=\"directed\">" + "<node id=\"Service:S1:1\"/>" + "<node id=\"Parameter:A\"/>" + "<node id=\"Parameter:B\"/>"
			+ "<edge id=\"1\" source=\"Parameter:A\" target=\"Service:S1:1\"/>" + "<edge id=\"2\" source=\"Service:S1:1\" target=\"Parameter:B\"/>" + "<node id=\"Service:S2:2\"/>" + "<node id=\"Parameter:B\"/>" + "<node id=\"Parameter:C\"/>"
			+ "<edge id=\"3\" source=\"Service:S2:2\" target=\"Parameter:C\"/>" + "<node id=\"Service:S1:4\"/>" + "<node id=\"Parameter:A\"/>" + "<node id=\"Parameter:B\"/>" + "<edge id=\"5\" source=\"Parameter:A\" target=\"Service:S1:4\"/>"
			+ "<edge id=\"6\" source=\"Service:S1:4\" target=\"Parameter:B\"/>" + "</graph>" + "</graphml>";

	@Test
	public void testXML() throws Exception {
		final ServiceGraph sGraph1 = new ServiceGraph();
		sGraph1.readFromXML(new ByteArrayInputStream(GRAPH_XML.getBytes()));

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		sGraph1.saveToXML(baos);
		baos.close();

		final ServiceGraph sGraph2 = new ServiceGraph();
		final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		sGraph2.readFromXML(bais);
		bais.close();

		assertEquals(sGraph1, sGraph2);
	}

	@Test
	public void testEquals() throws IOException {
		final ServiceGraph sGraph1 = new ServiceGraph();
		sGraph1.readFromXML(new ByteArrayInputStream(GRAPH_XML.getBytes()));

		final ServiceGraph sGraph2 = new ServiceGraph();
		sGraph2.readFromXML(new ByteArrayInputStream(GRAPH_XML.getBytes()));

		final ServiceGraph sGraph3 = new ServiceGraph();
		sGraph3.readFromXML(new ByteArrayInputStream(GRAPH_XML2.getBytes()));

		assertEquals(sGraph1, sGraph1);
		assertEquals(sGraph1, sGraph2);

		assertFalse(sGraph1.equals(sGraph3));
	}
}
