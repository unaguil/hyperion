package peer.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import peer.peerid.PeerID;

public class BroadcastMessageTest {
	
	class DummyMessage extends BroadcastMessage {

		private static final byte DUMMY_TYPE = 0x03;
		
		public DummyMessage() {
			super(DUMMY_TYPE);
		}
		
		public DummyMessage(final PeerID sender, final Set<PeerID> expectedDestinations) {
			super(DUMMY_TYPE, sender, expectedDestinations);
		}
	}
	
	@Test
	public void testSerialization() throws IOException {
		final DummyMessage message = new DummyMessage(new PeerID("3"), Collections.<PeerID>emptySet());
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		message.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final BroadcastMessage result = new DummyMessage();
		in.readByte(); //read mType byte
		result.read(in);
		in.close();
		assertEquals(message, result);
		assertTrue(message.getExpectedDestinations().containsAll(result.getExpectedDestinations()));
	}
}
