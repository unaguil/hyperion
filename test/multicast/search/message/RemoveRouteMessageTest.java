package multicast.search.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;

public class RemoveRouteMessageTest {
	
	@Test
	public void testSerialization() throws IOException, UnsupportedTypeException {
		final Set<MessageID> lostRoutes = new HashSet<MessageID>();
		lostRoutes.add(new MessageID(new PeerID("0"), MessageIDGenerator.getNewID()));
		lostRoutes.add(new MessageID(new PeerID("1"), MessageIDGenerator.getNewID()));
		lostRoutes.add(new MessageID(new PeerID("2"), MessageIDGenerator.getNewID()));
		
		final RemoveRouteMessage removeRouteMessage = new RemoveRouteMessage(new PeerID("3"), Collections.<PeerID>emptySet(), lostRoutes);
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		removeRouteMessage.write(out);
		out.close();
		
		assertEquals(41, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final RemoveRouteMessage result = (RemoveRouteMessage) MessageTypes.readBroadcastMessage(in);
		in.close();
		assertEquals(removeRouteMessage, result);
		assertTrue(removeRouteMessage.getLostRoutes().containsAll(result.getLostRoutes()));
		assertTrue(result.getLostRoutes().containsAll(removeRouteMessage.getLostRoutes()));
	}
}
