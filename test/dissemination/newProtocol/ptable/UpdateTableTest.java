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
		updateTable1.setAddition(ParameterFactory.createParameter("I-A", taxonomy), 5, new PeerID("0"));
	}

	@Test
	public void testSerialization() throws IOException, InvalidParameterIDException {
		final UpdateTable updateTable = new UpdateTable();
		updateTable.setAddition(ParameterFactory.createParameter("I-A", taxonomy), 3, new PeerID("0"));
		updateTable.setAddition(ParameterFactory.createParameter("I-B", taxonomy), 3, new PeerID("0"));
		updateTable.setDelete(ParameterFactory.createParameter("I-B", taxonomy), new PeerID("0"));

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(baos);
		updateTable.write(out);
		out.close();
		
		final byte[] data = baos.toByteArray();

		final ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
		final UpdateTable result = new UpdateTable();
		result.read(in);
		in.close();

		assertEquals(updateTable, result);
	} 
	
	@Test
	public void testMergeEqualMinorValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-A", taxonomy), 3, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-A", taxonomy)).getDistance());
	}
	
	@Test
	public void testMergeNotRelatedMinorValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-C", taxonomy), 3, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-A", taxonomy)).getDistance());
		assertEquals(3, updateTable1.getAddition(ParameterFactory.createParameter("I-C", taxonomy)).getDistance());
	}
	
	@Test
	public void testMergeNotRelatedGreaterValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-C", taxonomy), 7, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-A", taxonomy)).getDistance());
		assertEquals(7, updateTable1.getAddition(ParameterFactory.createParameter("I-C", taxonomy)).getDistance());
	}
	
	@Test
	public void testMergeEqualGreaterValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-A", taxonomy), 7, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(7, updateTable1.getAddition(ParameterFactory.createParameter("I-A", taxonomy)).getDistance());
	}
	
	@Test
	public void testMergeSubsumedMinorValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-B", taxonomy), 3, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-A", taxonomy)).getDistance());
		
		assertNull(updateTable1.getAddition(ParameterFactory.createParameter("I-B", taxonomy)));
	}
	
	@Test
	public void testMergeSubsumedGreaterValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-B", taxonomy), 7, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(7, updateTable1.getAddition(ParameterFactory.createParameter("I-A", taxonomy)).getDistance());
		
		assertNull(updateTable1.getAddition(ParameterFactory.createParameter("I-B", taxonomy)));
	}
	
	@Test
	public void testMergeSubsumeMinorValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-Z", taxonomy), 3, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(5, updateTable1.getAddition(ParameterFactory.createParameter("I-Z", taxonomy)).getDistance());
		assertNull(updateTable1.getAddition(ParameterFactory.createParameter("I-A", taxonomy)));
	}
	
	@Test
	public void testMergeSubsumeGreaterValue() throws InvalidParameterIDException {		
		final UpdateTable updateTable2 = new UpdateTable();
		updateTable2.setAddition(ParameterFactory.createParameter("I-Z", taxonomy), 7, new PeerID("0"));
		
		updateTable1.merge(updateTable2, taxonomy);
		
		assertEquals(7, updateTable1.getAddition(ParameterFactory.createParameter("I-Z", taxonomy)).getDistance());
		assertNull(updateTable1.getAddition(ParameterFactory.createParameter("I-A", taxonomy)));
	}
}
