package peer.message;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import peer.peerid.PeerID;

public class BundleMessageTest {

	@Test
	public void testDestinationRemoval() {
		final PeerID source = new PeerID("0"); 
		final Set<PeerID> dests1 = new HashSet<PeerID>();
		dests1.add(new PeerID("1"));
		dests1.add(new PeerID("2"));
		dests1.add(new PeerID("3"));
		final MessageString msgStr1 = new MessageString(source, dests1, "hello");
		
		final Set<PeerID> dests2 = new HashSet<PeerID>();
		dests2.add(new PeerID("2"));
		dests2.add(new PeerID("3"));
		final MessageString msgStr2 = new MessageString(source, dests2, "hello");
		
		final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>();
		messages.add(msgStr1);
		messages.add(msgStr2);
		
		final BundleMessage bundleMessage = new BundleMessage(source, messages);
		assertEquals(dests1, bundleMessage.getExpectedDestinations());
		assertEquals(messages, bundleMessage.getPayloadMessages());
		
		bundleMessage.removeDestination(new PeerID("2"));
		Set<PeerID> expected = new HashSet<PeerID>();
		expected.add(new PeerID("1"));
		expected.add(new PeerID("3"));
		assertEquals(expected, bundleMessage.getExpectedDestinations());
		assertEquals(messages, bundleMessage.getPayloadMessages());
		
		bundleMessage.removeDestination(new PeerID("3"));
		expected = new HashSet<PeerID>();
		expected.add(new PeerID("1"));
		assertEquals(expected, bundleMessage.getExpectedDestinations());
		
		final List<BroadcastMessage> expectedMessages = new ArrayList<BroadcastMessage>();
		expectedMessages.add(msgStr1);
		assertEquals(expectedMessages, bundleMessage.getPayloadMessages());
		
		bundleMessage.removeDestination(new PeerID("1"));
		assertTrue(bundleMessage.getExpectedDestinations().isEmpty());
		assertTrue(bundleMessage.getPayloadMessages().isEmpty());
	}
	
	@Test
	public void testSerialization() throws IOException, UnsupportedTypeException {
		final PeerID source = new PeerID("0"); 
		final Set<PeerID> dests1 = new HashSet<PeerID>();
		dests1.add(new PeerID("1"));
		dests1.add(new PeerID("2"));
		dests1.add(new PeerID("3"));
		final MessageString msgStr1 = new MessageString(source, dests1, "hello");
		
		final Set<PeerID> dests2 = new HashSet<PeerID>();
		dests2.add(new PeerID("2"));
		dests2.add(new PeerID("3"));
		final MessageString msgStr2 = new MessageString(source, dests2, "hello");
		
		final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>();
		messages.add(msgStr1);
		messages.add(msgStr2);
		
		final BundleMessage message = new BundleMessage(source, messages);
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		message.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final BroadcastMessage result = MessageTypes.readBroadcastMessage(in);
		in.close();
		assertEquals(message, result);
		
		assertTrue(message.getExpectedDestinations().containsAll(result.getExpectedDestinations()));
		assertTrue(result.getExpectedDestinations().containsAll(message.getExpectedDestinations()));
	}
}
