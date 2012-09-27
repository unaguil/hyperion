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

package peer.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import peer.peerid.PeerID;

public class BroadcastMessageTest {
	
	class DummyMessage extends BroadcastMessage {

		private static final byte DUMMY_TYPE = 0x03;
		
		public DummyMessage() {
			super(DUMMY_TYPE);
		}
		
		public DummyMessage(final PeerID sender, final Set<PeerID> expectedDestinations) {
			super(DUMMY_TYPE, sender, expectedDestinations);
		}
	}
	
	@Test
	public void testSerialization() throws IOException {
		final DummyMessage message = new DummyMessage(new PeerID("3"), Collections.<PeerID>emptySet());
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		message.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final BroadcastMessage result = new DummyMessage();
		in.readByte(); //read mType byte
		result.read(in);
		in.close();
		assertEquals(message, result);
		assertTrue(message.getExpectedDestinations().containsAll(result.getExpectedDestinations()));
	}
}
