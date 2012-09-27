/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package graphcreation.graph.servicegraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;

public class ServiceGraphTest {

	private static final String GRAPH_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<graphml>" + "<graph edgedefault=\"directed\">" + "<node id=\"Service:S1:1\"/>" + "<node id=\"Parameter:I-1\"/>" + "<node id=\"Parameter:O-2\"/>" + "<edge id=\"1\" source=\"Parameter:I-1\" target=\"Service:S1:1\"/>" + "<edge id=\"2\" source=\"Service:S1:1\" target=\"Parameter:O-2\"/>" + "<node id=\"Service:S2:2\"/>" + "<node id=\"Parameter:O-2\"/>" + "<node id=\"Parameter:O-3\"/>"
			+ "<edge id=\"3\" source=\"Parameter:O-2\" target=\"Service:S2:2\"/>" + "<edge id=\"4\" source=\"Service:S2:2\" target=\"Parameter:O-3\"/>" + "<node id=\"Service:S1:4\"/>" + "<node id=\"Parameter:I-1\"/>" + "<node id=\"Parameter:O-2\"/>" + "<edge id=\"5\" source=\"Parameter:I-1\" target=\"Service:S1:4\"/>" + "<edge id=\"6\" source=\"Service:S1:4\" target=\"Parameter:O-2\"/>" + "</graph>" + "</graphml>";

	private static final String GRAPH_XML2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<graphml>" + "<graph edgedefault=\"directed\">" + "<node id=\"Service:S1:1\"/>" + "<node id=\"Parameter:I-1\"/>" + "<node id=\"Parameter:O-2\"/>" + "<edge id=\"1\" source=\"Parameter:I-1\" target=\"Service:S1:1\"/>" + "<edge id=\"2\" source=\"Service:S1:1\" target=\"Parameter:O-2\"/>" + "<node id=\"Service:S2:2\"/>" + "<node id=\"Parameter:O-2\"/>" + "<node id=\"Parameter:O-3\"/>"
			+ "<edge id=\"3\" source=\"Service:S2:2\" target=\"Parameter:O-3\"/>" + "<node id=\"Service:S1:4\"/>" + "<node id=\"Parameter:I-1\"/>" + "<node id=\"Parameter:O-2\"/>" + "<edge id=\"5\" source=\"Parameter:I-1\" target=\"Service:S1:4\"/>" + "<edge id=\"6\" source=\"Service:S1:4\" target=\"Parameter:O-2\"/>" + "</graph>" + "</graphml>";

	private static final Taxonomy emptyTaxonomy = new BasicTaxonomy();
	
	@Test
	public void testXML() throws Exception {
		final ServiceGraph sGraph1 = new ServiceGraph(emptyTaxonomy);
		sGraph1.readFromXML(new ByteArrayInputStream(GRAPH_XML.getBytes()));

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		sGraph1.saveToXML(baos);
		baos.close();

		final ServiceGraph sGraph2 = new ServiceGraph(emptyTaxonomy);
		final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		sGraph2.readFromXML(bais);
		bais.close();

		assertEquals(sGraph1, sGraph2);
	}

	@Test
	public void testEquals() throws IOException {
		final ServiceGraph sGraph1 = new ServiceGraph(emptyTaxonomy);
		sGraph1.readFromXML(new ByteArrayInputStream(GRAPH_XML.getBytes()));

		final ServiceGraph sGraph2 = new ServiceGraph(emptyTaxonomy);
		sGraph2.readFromXML(new ByteArrayInputStream(GRAPH_XML.getBytes()));

		final ServiceGraph sGraph3 = new ServiceGraph(emptyTaxonomy);
		sGraph3.readFromXML(new ByteArrayInputStream(GRAPH_XML2.getBytes()));

		assertEquals(sGraph1, sGraph1);
		assertEquals(sGraph1, sGraph2);

		assertFalse(sGraph1.equals(sGraph3));
	}
}
