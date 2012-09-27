/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

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

		list.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("1")));

		assertFalse(list.isEmpty());

		assertEquals(1, list.getList().size());

		list.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("2")));

		assertEquals(2, list.getList().size());

		list.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));

		assertEquals(2, list.getList().size());
	}

	@Test
	public void testRemoveEstimatedDistancesFrom() {
		final EstimatedDistanceList list = new EstimatedDistanceList();
		list.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("1")));
		list.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("2")));

		EstimatedDistance previousEstimatedDistance = list.removeEstimatedDistanceFrom(new PeerID("1"));

		assertNull(list.removeEstimatedDistanceFrom(new PeerID("3")));

		assertEquals(new EstimatedDistance(4, new PeerID("1")), previousEstimatedDistance);

		assertEquals(1, list.getList().size());

		previousEstimatedDistance = list.removeEstimatedDistanceFrom(new PeerID("2"));

		assertEquals(new EstimatedDistance(4, new PeerID("2")), previousEstimatedDistance);

		assertTrue(list.isEmpty());
	}

	@Test
	public void testGetEffectiveDistance() {
		final EstimatedDistanceList list = new EstimatedDistanceList();

		list.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));

		list.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("1")));

		assertEquals(new EstimatedDistance(5, new PeerID("1")), list.getEffectiveDistance());
	}

	@Test
	public void testSearch() {
		final EstimatedDistanceList list = new EstimatedDistanceList();

		list.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("2")));

		assertEquals(new EstimatedDistance(5, new PeerID("2")), list.search(new PeerID("2")));

		assertNull(list.search(new PeerID("1")));
	}

	@Test
	public void testEquals() {
		final EstimatedDistanceList list1 = new EstimatedDistanceList();
		list1.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));
		list1.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("1")));

		final EstimatedDistanceList list2 = new EstimatedDistanceList();
		list2.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));
		list2.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("1")));

		final EstimatedDistanceList list3 = new EstimatedDistanceList();
		list3.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));
		list3.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("2")));

		assertEquals(list1, list1);
		assertEquals(list1, list2);

		assertFalse(list1.equals(list3));
	}

	@Test
	public void testEqualsWithOptionalEntries() {
		final EstimatedDistanceList list1 = new EstimatedDistanceList();
		list1.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));
		list1.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("1")));

		final EstimatedDistanceList list2 = new EstimatedDistanceList();
		list2.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));
		list2.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("1")));

		final EstimatedDistanceList list3 = new EstimatedDistanceList();
		list3.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));
		list3.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("2")));

		final EstimatedDistanceList list4 = new EstimatedDistanceList();
		list4.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("2"), true));
		list4.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("1")));

		final EstimatedDistanceList list5 = new EstimatedDistanceList();
		list5.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("1")));

		assertEquals(list1, list1);
		assertEquals(list1, list2);

		assertFalse(list1.equals(list3));

		assertEquals(list4, list5);
		assertEquals(list5, list4);
	}

	@Test
	public void testGetOptionalEntriesSize() {
		final EstimatedDistanceList list1 = new EstimatedDistanceList();
		list1.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));
		list1.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("2")));
		list1.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("3")));

		assertEquals(0, list1.getOptionalEntriesSize());

		final EstimatedDistanceList list2 = new EstimatedDistanceList();
		list2.updateEstimatedDistance(new EstimatedDistance(3, new PeerID("1")));
		list2.updateEstimatedDistance(new EstimatedDistance(5, new PeerID("2"), true));
		list2.updateEstimatedDistance(new EstimatedDistance(4, new PeerID("3")));

		assertEquals(1, list2.getOptionalEntriesSize());
	}
}
