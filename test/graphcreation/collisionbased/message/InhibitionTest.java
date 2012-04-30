package graphcreation.collisionbased.message;

import static org.junit.Assert.assertEquals;
import graphcreation.collisionbased.collisiondetector.Collision;

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

public class InhibitionTest {
	
	private final Taxonomy emptyTaxonomy = new BasicTaxonomy();

	@Test
	public void testSerialization() throws InvalidParameterIDException, IOException {
		final Collision collision = new Collision((InputParameter)ParameterFactory.createParameter("I-1", emptyTaxonomy), 
												  (OutputParameter)ParameterFactory.createParameter("O-1", emptyTaxonomy));

		final Inhibition inhibition = new Inhibition(collision);

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		inhibition.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final Inhibition result = new Inhibition();
		result.read(in);
		in.close();
		
		assertEquals(inhibition, result);
	}
}
