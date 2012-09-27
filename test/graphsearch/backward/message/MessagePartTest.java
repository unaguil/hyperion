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

package graphsearch.backward.message;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import peer.peerid.PeerID;

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
