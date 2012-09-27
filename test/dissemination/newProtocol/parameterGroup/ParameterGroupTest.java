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

		generalGroup = new ParameterGroup(ParameterFactory.createParameter("I-A", taxonomy), taxonomy);

		specialGroup = new ParameterGroup(ParameterFactory.createParameter("O-G", taxonomy), taxonomy);
	}

	@Test
	public void testEquals() throws InvalidParameterIDException {
		assertEquals(generalGroup, new ParameterGroup(ParameterFactory.createParameter("I-A", taxonomy), taxonomy));

		assertFalse(generalGroup.equals(new ParameterGroup(ParameterFactory.createParameter("I-B", taxonomy), taxonomy)));

		assertFalse(generalGroup.equals(new ParameterGroup(ParameterFactory.createParameter("O-A", taxonomy), taxonomy)));

		assertFalse(generalGroup.equals(specialGroup));
	}

	@Test
	public void testBelongs() throws InvalidParameterIDException {
		assertTrue(generalGroup.belongs(ParameterFactory.createParameter("I-A", taxonomy)));

		assertTrue(generalGroup.belongs(ParameterFactory.createParameter("I-B", taxonomy)));

		assertTrue(generalGroup.belongs(ParameterFactory.createParameter("I-D", taxonomy)));

		assertFalse(specialGroup.belongs(ParameterFactory.createParameter("I-G", taxonomy)));

		assertFalse(specialGroup.belongs(ParameterFactory.createParameter("I-A", taxonomy)));

		assertTrue(specialGroup.belongs(ParameterFactory.createParameter("O-A", taxonomy)));
	}

	@Test
	public void testAdd() throws InvalidParameterIDException {
		assertTrue(generalGroup.add(ParameterFactory.createParameter("I-A", taxonomy)));
		assertEquals(ParameterFactory.createParameter("I-A", taxonomy), generalGroup.getCurrentParameter());
		assertTrue(generalGroup.add(ParameterFactory.createParameter("I-E", taxonomy)));
		assertEquals(ParameterFactory.createParameter("I-A", taxonomy), generalGroup.getCurrentParameter());
		assertTrue(generalGroup.add(ParameterFactory.createParameter("I-G", taxonomy)));
		assertEquals(ParameterFactory.createParameter("I-A", taxonomy), generalGroup.getCurrentParameter());

		assertFalse(specialGroup.add(ParameterFactory.createParameter("I-C", taxonomy)));
		assertEquals(ParameterFactory.createParameter("O-G", taxonomy), specialGroup.getCurrentParameter());
		assertTrue(specialGroup.add(ParameterFactory.createParameter("O-C", taxonomy)));
		assertEquals(ParameterFactory.createParameter("O-C", taxonomy), specialGroup.getCurrentParameter());
		assertTrue(specialGroup.add(ParameterFactory.createParameter("O-A", taxonomy)));
		assertEquals(ParameterFactory.createParameter("O-A", taxonomy), specialGroup.getCurrentParameter());
	}

	@Test
	public void testGetCurrentParameter() throws InvalidParameterIDException {
		assertEquals(ParameterFactory.createParameter("I-A", taxonomy), generalGroup.getCurrentParameter());
		assertEquals(ParameterFactory.createParameter("O-G", taxonomy), specialGroup.getCurrentParameter());
	}
}
