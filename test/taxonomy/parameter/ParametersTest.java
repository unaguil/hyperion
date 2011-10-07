package taxonomy.parameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class ParametersTest {

	Parameter i1, i2, i3;
	Parameter o1, o2, o3;

	@Before
	public void setUp() throws Exception {
		i1 = ParameterFactory.createParameter("I-A");
		i2 = ParameterFactory.createParameter("I-B");
		i3 = ParameterFactory.createParameter("I-C");

		o1 = ParameterFactory.createParameter("O-A");
		o2 = ParameterFactory.createParameter("O-B");
		o3 = ParameterFactory.createParameter("O-C");
	}

	@Test
	public void testInstanceOf() {
		assertTrue(i1 instanceof InputParameter);
		assertTrue(i2 instanceof InputParameter);
		assertTrue(i3 instanceof InputParameter);

		assertTrue(o1 instanceof OutputParameter);
		assertTrue(o2 instanceof OutputParameter);
		assertTrue(o3 instanceof OutputParameter);

		assertFalse(i1 instanceof OutputParameter);
		assertFalse(i2 instanceof OutputParameter);
		assertFalse(i3 instanceof OutputParameter);

		assertTrue(i1 instanceof InputParameter);
		assertTrue(i2 instanceof InputParameter);
		assertTrue(i3 instanceof InputParameter);
	}

	@Test
	public void testEquals() {
		assertEquals(i1, i1);
		assertEquals(i2, i2);
		assertEquals(i3, i3);
		assertFalse(i1.equals(i2));
		assertFalse(i1.equals(i3));
		assertFalse(i2.equals(i3));

		assertEquals(o1, o1);
		assertEquals(o2, o2);
		assertEquals(o3, o3);
		assertFalse(o1.equals(o2));
		assertFalse(o1.equals(o3));
		assertFalse(o2.equals(o3));

		assertFalse(i1.equals(o1));
		assertFalse(i2.equals(o2));
		assertFalse(i3.equals(o3));

		assertFalse(i1.equals(o2));
		assertFalse(i1.equals(o3));

		assertFalse(i2.equals(o3));
	}
}
