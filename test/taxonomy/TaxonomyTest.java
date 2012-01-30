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
		assertTrue(taxonomy.subsumes("B", "B"));
		assertTrue(taxonomy.subsumes("A", "B"));
		assertTrue(taxonomy.subsumes("A", "C"));
		assertTrue(taxonomy.subsumes("B", "D"));
		assertTrue(taxonomy.subsumes("B", "E"));
		assertTrue(taxonomy.subsumes("C", "F"));
		assertTrue(taxonomy.subsumes("C", "G"));

		assertTrue(taxonomy.subsumes("A", "G"));

		assertFalse(taxonomy.subsumes("B", "F"));
		assertFalse(taxonomy.subsumes("B", "A"));

		assertFalse(taxonomy.subsumes("G", "A"));

		final Taxonomy emptyTaxonomy = new BasicTaxonomy();
		assertFalse(emptyTaxonomy.subsumes("A", "B"));
	}

	@Test
	public void testAreRelated() {
		assertTrue(taxonomy.areRelated("A", "A"));
		assertTrue(taxonomy.areRelated("A", "E"));
		assertTrue(taxonomy.areRelated("E", "A"));

		assertFalse(taxonomy.areRelated("B", "F"));
		assertFalse(taxonomy.areRelated("F", "B"));
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
}
