package dissemination.newProtocol.ptable;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import peer.peerid.PeerID;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.ParameterFactory;

public class UpdateTableTest {

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
}
