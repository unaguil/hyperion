package graphsearch.backward.message;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import peer.PeerID;

public class MessagePartTest {

	@Test
	public void testGetPartitionLevel() {
		final MessagePart messagePart = new MessagePart(new PeerID("0"));
		assertEquals(0, messagePart.getSplitLevel());

		// First split
		Set<MessagePart> messsageParts = messagePart.split(3, new PeerID("1"));
		for (final MessagePart part : messsageParts)
			assertEquals(1, part.getSplitLevel());

		// Second split
		messsageParts = messsageParts.iterator().next().split(2, new PeerID("2"));
		for (final MessagePart part : messsageParts)
			assertEquals(2, part.getSplitLevel());
	}
}
