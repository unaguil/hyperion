package dissemination.newProtocol.ptable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import peer.peerid.PeerID;

public class EstimatedDistanceTest {

	private EstimatedDistance eDistance1, eDistance2, eDistance3, eDistance4;

	@Before
	public void setUp() throws Exception {
		eDistance1 = new EstimatedDistance(5, new PeerID("A"));
		eDistance2 = new EstimatedDistance(3, new PeerID("B"));
		eDistance3 = new EstimatedDistance(5, new PeerID("A"));
		eDistance4 = new EstimatedDistance(5, new PeerID("B"));
	}

	@Test
	public void testEquality() {
		assertEquals(eDistance1, eDistance1);
		assertEquals(eDistance2, eDistance2);
		assertEquals(eDistance3, eDistance3);
		assertEquals(eDistance4, eDistance4);

		assertFalse(eDistance1.equals(eDistance2));
		assertEquals(eDistance1, eDistance3);
		assertFalse(eDistance1.equals(eDistance4));
	}

	@Test
	public void testCompare() {
		assertTrue(eDistance1.compareTo(eDistance1) == 0);
		assertTrue(eDistance1.compareTo(eDistance2) > 0);
		assertTrue(eDistance2.compareTo(eDistance1) < 0);
		assertTrue(eDistance1.compareTo(eDistance3) == 0);
		assertFalse(eDistance1.compareTo(eDistance4) == 0);
		assertTrue(eDistance2.compareTo(eDistance4) < 0);
		assertTrue(eDistance4.compareTo(eDistance2) > 0);
		assertTrue(eDistance3.compareTo(eDistance4) < 0);
	}
}
