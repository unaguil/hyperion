package dissemination.newProtocol.ptable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Before;
import org.junit.Test;

import peer.peerid.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.TaxonomyException;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.ParameterFactory;

public class UpdateTableTest {
	
	private Taxonomy taxonomy;
	private UpdateTable updateTable1;
	
	@Before
	public void setUp() throws TaxonomyException, InvalidParameterIDException {
		taxonomy = new BasicTaxonomy();
		taxonomy.setRoot("Z");
		taxonomy.addChild("Z", "A");
		taxonomy.addChild("Z", "C");
		taxonomy.addChild("A", "B");
		
		updateTable1 = new UpdateTable();
		updateTable1.setAddition(ParameterFactory.createParameter("I-A"), 5, new PeerID("0"));
	}

	@Test
	public void testSerialization() throws IOException, ClassNotFoundException, InvalidParameterIDException {
		final UpdateTable updateTable = new UpdateTable();
		updateTable.setAddition(ParameterFactory.createParameter("I-A"), 3, new PeerID("0"));
		updateTable.setAddition(ParameterFactory.createParameter("I-B"), 3, new PeerID("0"));
		updateTable.setDelete(ParameterFactory.createParameter("I-B"), new PeerID("0"));

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream os = new ObjectOutputStream(baos);

		os.writeObject(updateTable);

		final byte[] data = baos.toByteArray();

		os.close();
		baos.close();

		final ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(data));
		final UpdateTable result = (UpdateTable) is.readObject();

		assertEquals(updateTable, result);
	} 
	
	@Test
	public void testMergeEqualMinorValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-A"), 3, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-A")).getDistance());
	}
	
	@Test
	public void testMergeNotRelatedMinorValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-C"), 3, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-A")).getDistance());
		assertEquals(3, updateTable1.getAddition(ParameterFactory.createParameter("I-C")).getDistance());
	}
	
	@Test
	public void testMergeNotRelatedGreaterValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-C"), 7, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-A")).getDistance());
		assertEquals(7, updateTable1.getAddition(ParameterFactory.createParameter("I-C")).getDistance());
	}
	
	@Test
	public void testMergeEqualGreaterValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-A"), 7, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(7, updateTable1.getAddition(ParameterFactory.createParameter("I-A")).getDistance());
	}
	
	@Test
	public void testMergeSubsumedMinorValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-B"), 3, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-A")).getDistance());
		
		assertNull(updateTable1.getAddition(ParameterFactory.createParameter("I-B")));
	}
	
	@Test
	public void testMergeSubsumedGreaterValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-B"), 7, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(7, updateTable1.getAddition(ParameterFactory.createParameter("I-A")).getDistance());
		
		assertNull(updateTable1.getAddition(ParameterFactory.createParameter("I-B")));
	}
	
	@Test
	public void testMergeSubsumeMinorValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-Z"), 3, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-Z")).getDistance());
		assertNull(updateTable1.getAddition(ParameterFactory.createParameter("I-A")));
	}
	
	@Test
	public void testMergeSubsumeGreaterValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-Z"), 7, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(7, updateTable1.getAddition(ParameterFactory.createParameter("I-Z")).getDistance());
		assertNull(updateTable1.getAddition(ParameterFactory.createParameter("I-A")));
	}
}
