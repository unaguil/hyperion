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
