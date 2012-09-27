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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package taxonomy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class TaxonomyTest {

	BasicTaxonomy taxonomy, taxonomy2;

	@Before
	public void setUp() throws TaxonomyException {
		taxonomy = new BasicTaxonomy();

		taxonomy.setRoot("A");

		taxonomy.addChild("A", "B");
		taxonomy.addChild("A", "C");
		taxonomy.addChild("B", "E");
		taxonomy.addChild("B", "D");
		taxonomy.addChild("C", "F");
		taxonomy.addChild("C", "G");

		taxonomy2 = new BasicTaxonomy();
		taxonomy2.setRoot("A");

		taxonomy2.addChild("A", "B");
		taxonomy2.addChild("A", "C");
		taxonomy2.addChild("B", "D");
		taxonomy2.addChild("B", "E");
		taxonomy2.addChild("C", "F");
		taxonomy2.addChild("C", "G");
	}

	@Test(expected = TaxonomyException.class)
	public void testAddChild() throws TaxonomyException {
		final Taxonomy otherTaxonomy = new BasicTaxonomy();
		otherTaxonomy.addChild("A", "B");
	}

	@Test
	public void testSubsume() {
		assertTrue(taxonomy.subsumes(taxonomy.encode("B"), taxonomy.encode("B")));
		assertTrue(taxonomy.subsumes(taxonomy.encode("A"), taxonomy.encode("B")));
		assertTrue(taxonomy.subsumes(taxonomy.encode("A"), taxonomy.encode("C")));
		assertTrue(taxonomy.subsumes(taxonomy.encode("B"), taxonomy.encode("D")));
		assertTrue(taxonomy.subsumes(taxonomy.encode("B"), taxonomy.encode("E")));
		assertTrue(taxonomy.subsumes(taxonomy.encode("C"), taxonomy.encode("F")));
		assertTrue(taxonomy.subsumes(taxonomy.encode("C"), taxonomy.encode("G")));

		assertTrue(taxonomy.subsumes(taxonomy.encode("A"), taxonomy.encode("G")));

		assertFalse(taxonomy.subsumes(taxonomy.encode("B"), taxonomy.encode("F")));
		assertFalse(taxonomy.subsumes(taxonomy.encode("B"), taxonomy.encode("A")));

		assertFalse(taxonomy.subsumes(taxonomy.encode("G"), taxonomy.encode("A")));

		final Taxonomy emptyTaxonomy = new BasicTaxonomy();
		assertFalse(emptyTaxonomy.subsumes(taxonomy.encode("A"), taxonomy.encode("B")));
	}

	@Test
	public void testAreRelated() {
		assertTrue(taxonomy.areRelated(taxonomy.encode("A"), taxonomy.encode("A")));
		assertTrue(taxonomy.areRelated(taxonomy.encode("A"), taxonomy.encode("E")));
		assertTrue(taxonomy.areRelated(taxonomy.encode("E"), taxonomy.encode("A")));

		assertFalse(taxonomy.areRelated(taxonomy.encode("B"), taxonomy.encode("F")));
		assertFalse(taxonomy.areRelated(taxonomy.encode("F"), taxonomy.encode("B")));
	}

	@Test(expected=TaxonomyException.class)
	public void testGetParent() throws TaxonomyException {
		assertEquals("NONE", taxonomy.getParent("A"));
		assertEquals("A", taxonomy.getParent("B"));
		assertEquals("A", taxonomy.getParent("C"));
		assertEquals("B", taxonomy.getParent("D"));
		assertEquals("B", taxonomy.getParent("E"));
		assertEquals("C", taxonomy.getParent("F"));
		assertEquals("C", taxonomy.getParent("G"));

		assertNull(taxonomy.getParent("W"));
	}

	@Test
	public void testEquals() {
		assertTrue(taxonomy.equals(taxonomy2));
	}

	@Test
	public void testReadFromXML() throws IOException {
		final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + "<taxonomy root=\"A\">" + "<element id=\"A\">" + "<child id=\"B\"/>" + "<child id=\"C\"/>" + "</element>" + "<element id=\"B\">" + "<child id=\"D\"/>" + "<child id=\"E\"/>" + "</element>" + "<element id=\"E\"/>" + "<element id=\"C\">" + "<child id=\"F\"/>" + "<child id=\"G\"/>" + "</element>" + "<element id=\"F\"/>" + "<element id=\"G\"/>" + "</taxonomy>";

		final BasicTaxonomy otherTaxonomy = new BasicTaxonomy();
		otherTaxonomy.readFromXML(new ByteArrayInputStream(xml.getBytes()));

		assertEquals(taxonomy, otherTaxonomy);
	}

	@Test
	public void testXMLSerialization() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		taxonomy.saveToXML(baos);
		baos.close();

		final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		final BasicTaxonomy otherTaxonomy = new BasicTaxonomy();
		otherTaxonomy.readFromXML(bais);

		assertEquals(taxonomy, otherTaxonomy);
	}
	
	@Test
	public void testEncode() {
		assertEquals(1, taxonomy.encode("A"));
		assertEquals(2, taxonomy.encode("B"));
		assertEquals(3, taxonomy.encode("C"));
		assertEquals(4, taxonomy.encode("D"));
		assertEquals(5, taxonomy.encode("E"));
		assertEquals(6, taxonomy.encode("F"));
		assertEquals(7, taxonomy.encode("G"));
		
		assertEquals(0, taxonomy.encode("0"));
		assertEquals(-1, taxonomy.encode("1"));
		assertEquals(-2, taxonomy.encode("2"));
		assertEquals(-3, taxonomy.encode("3"));
		assertEquals(-4, taxonomy.encode("4"));
		assertEquals(-5, taxonomy.encode("5"));
		assertEquals(-6, taxonomy.encode("6"));
	}
	
	@Test
	public void testDecode() {
		assertEquals("A", taxonomy.decode((short)1));
		assertEquals("B", taxonomy.decode((short)2));
		assertEquals("C", taxonomy.decode((short)3));
		assertEquals("D", taxonomy.decode((short)4));
		assertEquals("E", taxonomy.decode((short)5));
		assertEquals("F", taxonomy.decode((short)6));
		assertEquals("G", taxonomy.decode((short)7));
		
		assertEquals("0", taxonomy.decode((short)-0));
		assertEquals("1", taxonomy.decode((short)-1));
		assertEquals("2", taxonomy.decode((short)-2));
		assertEquals("3", taxonomy.decode((short)-3));
		assertEquals("4", taxonomy.decode((short)-4));
		assertEquals("5", taxonomy.decode((short)-5));
		assertEquals("6", taxonomy.decode((short)-6));
	}
}
