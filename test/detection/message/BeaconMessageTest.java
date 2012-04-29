package detection.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;

public class BeaconMessageTest {

	@Test
	public void testEquals() {
		final BeaconMessage message1 = new BeaconMessage(new PeerID("0"));
		final BeaconMessage message2 = new BeaconMessage(new PeerID("1"));
		
		assertTrue(message1.equals(message1));
		assertFalse(message1.equals(message2));
	}
	
	@Test
	public void testSerialization() throws IOException, UnsupportedTypeException {
		final BeaconMessage message1 = new BeaconMessage();
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		message1.write(out);
		out.close();
		
		assertEquals(13, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final BroadcastMessage message2 = MessageTypes.readBroadcastMessage(in);
		in.close();
		assertEquals(message1, message2);
	}
}
