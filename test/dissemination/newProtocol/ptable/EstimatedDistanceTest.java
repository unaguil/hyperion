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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

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
		eDistance1 = new EstimatedDistance(5, new PeerID("1"));
		eDistance2 = new EstimatedDistance(3, new PeerID("2"));
		eDistance3 = new EstimatedDistance(5, new PeerID("1"));
		eDistance4 = new EstimatedDistance(5, new PeerID("2"));
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
