package dissemination.newProtocol.ptable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import peer.peerid.PeerID;

public class EstimatedDistanceListTest {

	@Test
	public void testUpdateDistance() {
		final EstimatedDistanceList list = new EstimatedDistanceList();

		assertTrue(list.isEmpty());

		list.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("A")));

		assertFalse(list.isEmpty());

		assertEquals(1, list.getList().size());

		list.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("B")));

		assertEquals(2, list.getList().size());

		list.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));

		assertEquals(2, list.getList().size());
	}

	@Test
	public void testRemoveEstimatedDistancesFrom() {
		final EstimatedDistanceList list = new EstimatedDistanceList();
		list.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("A")));
		list.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("B")));

		EstimatedDistance previousEstimatedDistance = list.removeEstimatedDistanceFrom(new PeerID("A"));

		assertNull(list.removeEstimatedDistanceFrom(new PeerID("C")));

		assertEquals(new EstimatedDistance(4, new PeerID("A")), previousEstimatedDistance);

		assertEquals(1, list.getList().size());

		previousEstimatedDistance = list.removeEstimatedDistanceFrom(new PeerID("B"));

		assertEquals(new EstimatedDistance(4, new PeerID("B")), previousEstimatedDistance);

		assertTrue(list.isEmpty());
	}

	@Test
	public void testGetEffectiveDistance() {
		final EstimatedDistanceList list = new EstimatedDistanceList();

		list.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));

		list.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("A")));

		assertEquals(new EstimatedDistance(5, new PeerID("A")), list.getEffectiveDistance());
	}

	@Test
	public void testSearch() {
		final EstimatedDistanceList list = new EstimatedDistanceList();

		list.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("B")));

		assertEquals(new EstimatedDistance(5, new PeerID("B")), list.search(new PeerID("B")));

		assertNull(list.search(new PeerID("A")));
	}

	@Test
	public void testEquals() {
		final EstimatedDistanceList list1 = new EstimatedDistanceList();
		list1.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));
		list1.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("A")));

		final EstimatedDistanceList list2 = new EstimatedDistanceList();
		list2.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));
		list2.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("A")));

		final EstimatedDistanceList list3 = new EstimatedDistanceList();
		list3.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));
		list3.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("B")));

		assertEquals(list1, list1);
		assertEquals(list1, list2);

		assertFalse(list1.equals(list3));
	}

	@Test
	public void testEqualsWithOptionalEntries() {
		final EstimatedDistanceList list1 = new EstimatedDistanceList();
		list1.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));
		list1.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("A")));

		final EstimatedDistanceList list2 = new EstimatedDistanceList();
		list2.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));
		list2.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("A")));

		final EstimatedDistanceList list3 = new EstimatedDistanceList();
		list3.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));
		list3.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("B")));

		final EstimatedDistanceList list4 = new EstimatedDistanceList();
		list4.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("B"), true));
		list4.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("A")));

		final EstimatedDistanceList list5 = new EstimatedDistanceList();
		list5.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("A")));

		assertEquals(list1, list1);
		assertEquals(list1, list2);

		assertFalse(list1.equals(list3));

		assertEquals(list4, list5);
		assertEquals(list5, list4);
	}

	@Test
	public void testGetOptionalEntriesSize() {
		final EstimatedDistanceList list1 = new EstimatedDistanceList();
		list1.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));
		list1.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("B")));
		list1.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("C")));

		assertEquals(0, list1.getOptionalEntriesSize());

		final EstimatedDistanceList list2 = new EstimatedDistanceList();
		list2.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("A")));
		list2.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("B"), true));
		list2.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("C")));

		assertEquals(1, list2.getOptionalEntriesSize());
	}
}
