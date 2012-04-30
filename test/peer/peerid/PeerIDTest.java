package peer.peerid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

public class PeerIDTest {

	@Test
	public void testEquals() {
		final PeerID peerID1 = new PeerID("0");
		final PeerID peerID2 = new PeerID("1");
		final PeerID peerID3 = new PeerID("0");
		
		assertTrue(peerID1.equals(peerID1));
		assertFalse(peerID1.equals(peerID2));
		assertTrue(peerID1.equals(peerID3));
	}
	
	@Test
	public void testSerialization() throws IOException {
		final PeerID peerID1 = new PeerID("1");
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		peerID1.write(out);
		out.close();
		
		assertEquals(10, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final PeerID peerID2 = new PeerID();
		peerID2.read(in);
		in.close();
		
		assertEquals(peerID1, peerID2);
	}
}
