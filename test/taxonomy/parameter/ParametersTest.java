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

package taxonomy.parameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Before;
import org.junit.Test;

import peer.message.UnsupportedTypeException;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;

public class ParametersTest {

	Parameter i1, i2, i3;
	Parameter o1, o2, o3;
	
	final Taxonomy taxonomy = new BasicTaxonomy();

	@Before
	public void setUp() throws Exception {
		i1 = ParameterFactory.createParameter("I-1", taxonomy);
		i2 = ParameterFactory.createParameter("I-2", taxonomy);
		i3 = ParameterFactory.createParameter("I-3", taxonomy);

		o1 = ParameterFactory.createParameter("O-1", taxonomy);
		o2 = ParameterFactory.createParameter("O-2", taxonomy);
		o3 = ParameterFactory.createParameter("O-3", taxonomy);
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
	
	@Test
	public void testInputSerialization() throws IOException, UnsupportedTypeException {		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		i1.write(out);
		out.close();
		
		assertEquals(9, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final Parameter p = Parameter.readParameter(in);
		in.close();
		assertEquals(i1, p);
	}
	
	@Test
	public void testOutputSerialization() throws IOException, UnsupportedTypeException {		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		o1.write(out);
		out.close();
		
		assertEquals(9, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final Parameter p = Parameter.readParameter(in);
		in.close();
		assertEquals(o1, p);
	}
}
