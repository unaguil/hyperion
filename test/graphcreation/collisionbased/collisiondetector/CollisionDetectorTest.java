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
		allParameters.add(ParameterFactory.createParameter("I-A"));
		allParameters.add(ParameterFactory.createParameter("I-B"));
		allParameters.add(ParameterFactory.createParameter("I-C"));

		allParameters.add(ParameterFactory.createParameter("O-D"));
		allParameters.add(ParameterFactory.createParameter("O-E"));
		allParameters.add(ParameterFactory.createParameter("O-F"));

		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("O-A"));
		newParameters.add(ParameterFactory.createParameter("O-B"));
		newParameters.add(ParameterFactory.createParameter("O-G"));
		newParameters.add(ParameterFactory.createParameter("I-D"));
		newParameters.add(ParameterFactory.createParameter("I-E"));
		newParameters.add(ParameterFactory.createParameter("I-H"));
		newParameters.add(ParameterFactory.createParameter("I-I"));
		newParameters.add(ParameterFactory.createParameter("O-I"));

		// New parameters are also in the all parameters when the
		// getParametersColliding function is called.
		allParameters.addAll(newParameters);

		final Set<Collision> detectedCollisions = CollisionDetector.getParametersColliding(newParameters, allParameters, true, emptyTaxonomy);
		assertEquals(5, detectedCollisions.size());

		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-A"))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-B"), (OutputParameter) ParameterFactory.createParameter("O-B"))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-D"), (OutputParameter) ParameterFactory.createParameter("O-D"))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-E"), (OutputParameter) ParameterFactory.createParameter("O-E"))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-I"), (OutputParameter) ParameterFactory.createParameter("O-I"))));
	}

	@Test
	public void testGetParametersCollidingWithTaxonomy() throws InvalidParameterIDException, TaxonomyException {
		final Taxonomy taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");

		final Set<Parameter> allParameters = new HashSet<Parameter>();
		allParameters.add(ParameterFactory.createParameter("I-A"));
		allParameters.add(ParameterFactory.createParameter("I-C"));
		allParameters.add(ParameterFactory.createParameter("I-F"));

		allParameters.add(ParameterFactory.createParameter("O-B"));
		allParameters.add(ParameterFactory.createParameter("I-D"));
		allParameters.add(ParameterFactory.createParameter("O-C"));
		allParameters.add(ParameterFactory.createParameter("O-G"));

		final Set<Parameter> newParameters = new HashSet<Parameter>();
		newParameters.add(ParameterFactory.createParameter("O-B"));
		newParameters.add(ParameterFactory.createParameter("I-D"));
		newParameters.add(ParameterFactory.createParameter("O-C"));
		newParameters.add(ParameterFactory.createParameter("O-G"));

		// New parameters are also in the all parameters when the
		// getParametersColliding function is called.
		allParameters.addAll(newParameters);

		final Set<Collision> detectedCollisions = CollisionDetector.getParametersColliding(newParameters, allParameters, true, taxonomy);
		assertEquals(2, detectedCollisions.size());

		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-A"), (OutputParameter) ParameterFactory.createParameter("O-B"))));
		assertTrue(detectedCollisions.contains(new Collision((InputParameter) ParameterFactory.createParameter("I-C"), (OutputParameter) ParameterFactory.createParameter("O-C"))));
	}
}
