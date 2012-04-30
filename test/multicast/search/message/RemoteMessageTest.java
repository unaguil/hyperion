package multicast.search.message;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import peer.message.BroadcastMessage;
import peer.message.MessageStringPayload;
import peer.peerid.PeerID;

public class RemoteMessageTest {
	
	class DummyMessage extends RemoteMessage {

		private static final byte DUMMY_TYPE = 0x03;
		
		public DummyMessage() {
			super(DUMMY_TYPE);
		}
		
		public DummyMessage(final PeerID source, final BroadcastMessage payload, final Set<PeerID> expectedDestinations) {
			super(DUMMY_TYPE, source, payload, expectedDestinations);
		}
	}
	
	@Test
	public void testSerialization() throws IOException {
		final DummyMessage message = new DummyMessage(new PeerID("3"), new MessageStringPayload(new PeerID("3"), "Hola, mundo!"), Collections.<PeerID>emptySet());
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		message.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final RemoteMessage result = new DummyMessage();
		in.readByte(); //read mType byte
		result.read(in);
		in.close();
		assertEquals(message, result);
		assertEquals(message.getPayload(), result.getPayload());
	}
}
