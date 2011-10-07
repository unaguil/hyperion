package dissemination.newProtocol.parameterGroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.ParameterFactory;
import dissemination.newProtocol.ptable.ParameterGroup;

public class ParameterGroupTest {

	private Taxonomy taxonomy;
	private ParameterGroup generalGroup, specialGroup;

	@Before
	public void setUp() throws Exception {
		taxonomy = new BasicTaxonomy();

		taxonomy.setRoot("A");

		taxonomy.addChild("A", "B");
		taxonomy.addChild("A", "C");
		taxonomy.addChild("B", "E");
		taxonomy.addChild("B", "D");
		taxonomy.addChild("C", "F");
		taxonomy.addChild("C", "G");

		generalGroup = new ParameterGroup(ParameterFactory.createParameter("I-A"), taxonomy);

		specialGroup = new ParameterGroup(ParameterFactory.createParameter("O-G"), taxonomy);
	}

	@Test
	public void testEquals() throws InvalidParameterIDException {
		assertEquals(generalGroup, new ParameterGroup(ParameterFactory.createParameter("I-A"), taxonomy));

		assertFalse(generalGroup.equals(new ParameterGroup(ParameterFactory.createParameter("I-B"), taxonomy)));

		assertFalse(generalGroup.equals(new ParameterGroup(ParameterFactory.createParameter("O-A"), taxonomy)));

		assertFalse(generalGroup.equals(specialGroup));
	}

	@Test
	public void testBelongs() throws InvalidParameterIDException {
		assertTrue(generalGroup.belongs(ParameterFactory.createParameter("I-A")));

		assertTrue(generalGroup.belongs(ParameterFactory.createParameter("I-B")));

		assertTrue(generalGroup.belongs(ParameterFactory.createParameter("I-D")));

		assertFalse(specialGroup.belongs(ParameterFactory.createParameter("I-G")));

		assertFalse(specialGroup.belongs(ParameterFactory.createParameter("I-A")));

		assertTrue(specialGroup.belongs(ParameterFactory.createParameter("O-A")));
	}

	@Test
	public void testAdd() throws InvalidParameterIDException {
		assertTrue(generalGroup.add(ParameterFactory.createParameter("I-A")));
		assertEquals(ParameterFactory.createParameter("I-A"), generalGroup.getCurrentParameter());
		assertTrue(generalGroup.add(ParameterFactory.createParameter("I-E")));
		assertEquals(ParameterFactory.createParameter("I-A"), generalGroup.getCurrentParameter());
		assertTrue(generalGroup.add(ParameterFactory.createParameter("I-G")));
		assertEquals(ParameterFactory.createParameter("I-A"), generalGroup.getCurrentParameter());

		assertFalse(specialGroup.add(ParameterFactory.createParameter("I-C")));
		assertEquals(ParameterFactory.createParameter("O-G"), specialGroup.getCurrentParameter());
		assertTrue(specialGroup.add(ParameterFactory.createParameter("O-C")));
		assertEquals(ParameterFactory.createParameter("O-C"), specialGroup.getCurrentParameter());
		assertTrue(specialGroup.add(ParameterFactory.createParameter("O-A")));
		assertEquals(ParameterFactory.createParameter("O-A"), specialGroup.getCurrentParameter());
	}

	@Test
	public void testGetCurrentParameter() throws InvalidParameterIDException {
		assertEquals(ParameterFactory.createParameter("I-A"), generalGroup.getCurrentParameter());
		assertEquals(ParameterFactory.createParameter("O-G"), specialGroup.getCurrentParameter());
	}
}
