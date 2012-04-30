package peer.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import peer.peerid.PeerID;

public class MessageIDTest {

	@Test
	public void testEquals() {
		final MessageID messageID1 = new MessageID(new PeerID("0"), (short) 320);
		final MessageID messageID2 = new MessageID(new PeerID("3"), (short) 320);
		final MessageID messageID3 = new MessageID(new PeerID("0"), (short) 350);
		final MessageID messageID4 = new MessageID(new PeerID("0"), (short) 320);
		
		assertTrue(messageID1.equals(messageID1));
		assertFalse(messageID1.equals(messageID2));
		assertFalse(messageID1.equals(messageID3));
		assertTrue(messageID1.equals(messageID4));
	}
	
	@Test
	public void testSerialization() throws IOException {
		final MessageID messageID1 = new MessageID(new PeerID("3"), (short) 320);
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		messageID1.write(out);
		out.close();
		
		assertEquals(12, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final MessageID messageID2 = new MessageID();
		messageID2.read(in);
		in.close();
		
		assertEquals(messageID1, messageID2);
	}
}
