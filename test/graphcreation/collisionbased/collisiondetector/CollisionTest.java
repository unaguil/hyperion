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

package graphcreation.collisionbased.collisiondetector;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.ParameterFactory;

public class CollisionTest {
	
	private final Taxonomy emptyTaxonomy = new BasicTaxonomy();

	@Test
	public void testSerialization() throws InvalidParameterIDException, IOException {
		final Collision collision = new Collision((InputParameter)ParameterFactory.createParameter("I-1", emptyTaxonomy), 
												  (OutputParameter)ParameterFactory.createParameter("O-1", emptyTaxonomy));
		

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		collision.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final Collision result = new Collision();
		result.read(in);
		in.close();
		
		assertEquals(collision, result);
	}
}
