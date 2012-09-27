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

package graphcreation.collisionbased.collisiondetector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.TaxonomyException;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class CollisionDetectorTest {

	private final static Taxonomy emptyTaxonomy = new BasicTaxonomy();

	@Test
	public void testGetParametersColliding() throws InvalidParameterIDException {
		final Set<Parameter> allParameters = new HashSet<Parameter>();
		allParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		allParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		allParameters.add(ParameterFactory.createParameter("I-10", emptyTaxonomy));

		allParameters.add(ParameterFactory.createParameter("O-3", emptyTaxonomy));
		allParameters.add(ParameterFactory.createParameter("O-4", emptyTaxonomy));
		allParameters.add(ParameterFactory.createParameter("O-5", emptyTaxonomy));

		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("O-1", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("O-2", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("O-6", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-3", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-4", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-7", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("I-8", emptyTaxonomy));
		newParameters.add(ParameterFactory.createParameter("O-8", emptyTaxonomy));

		// New parameters are also in the all parameters when the
		// getParametersColliding function is called.
		allParameters.addAll(newParameters);

		final Set<Collision> detectedCollisions = CollisionDetector.getParametersColliding(newParameters, allParameters, true, emptyTaxonomy);
		assertEquals(5, detectedCollisions.size());

		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-1", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-1", emptyTaxonomy))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-2", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-2", emptyTaxonomy))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-3", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-3", emptyTaxonomy))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-4", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-4", emptyTaxonomy))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-8", emptyTaxonomy), (OutputParameter) ParameterFactory.createParameter("O-8", emptyTaxonomy))));
	}

	@Test
	public void testGetParametersCollidingWithTaxonomy() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final Set<Parameter> allParameters = new HashSet<Parameter>();
		allParameters.add(ParameterFactory.createParameter("I-A", taxonomy));
		allParameters.add(ParameterFactory.createParameter("I-C", taxonomy));
		allParameters.add(ParameterFactory.createParameter("I-1", taxonomy));

		allParameters.add(ParameterFactory.createParameter("O-B", taxonomy));
		allParameters.add(ParameterFactory.createParameter("I-3", taxonomy));
		allParameters.add(ParameterFactory.createParameter("O-C", taxonomy));
		allParameters.add(ParameterFactory.createParameter("O-2", taxonomy));

		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("O-B", taxonomy));
		newParameters.add(ParameterFactory.createParameter("I-3", taxonomy));
		newParameters.add(ParameterFactory.createParameter("O-C", taxonomy));
		newParameters.add(ParameterFactory.createParameter("O-2", taxonomy));

		// New parameters are also in the all parameters when the
		// getParametersColliding function is called.
		allParameters.addAll(newParameters);

		final Set<Collision> detectedCollisions = CollisionDetector.getParametersColliding(newParameters, allParameters, true, taxonomy);
		assertEquals(2, detectedCollisions.size());

		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-A", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-B", taxonomy))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-C", taxonomy), (OutputParameter) ParameterFactory.createParameter("O-C", taxonomy))));
	}
}
