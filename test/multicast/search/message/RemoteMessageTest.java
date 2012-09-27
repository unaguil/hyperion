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

package multicast.search.message;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import peer.message.BroadcastMessage;
import peer.message.MessageStringPayload;
import peer.peerid.PeerID;

public class RemoteMessageTest {
	
	class DummyMessage extends RemoteMessage {

		private static final byte DUMMY_TYPE = 0x03;
		
		public DummyMessage() {
			super(DUMMY_TYPE);
		}
		
		public DummyMessage(final PeerID source, final BroadcastMessage payload, final Set<PeerID> expectedDestinations) {
			super(DUMMY_TYPE, source, payload, expectedDestinations);
		}
	}
	
	@Test
	public void testSerialization() throws IOException {
		final DummyMessage message = new DummyMessage(new PeerID("3"), new MessageStringPayload(new PeerID("3"), "Hola, mundo!"), Collections.<PeerID>emptySet());
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		message.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final RemoteMessage result = new DummyMessage();
		in.readByte(); //read mType byte
		result.read(in);
		in.close();
		assertEquals(message, result);
		assertEquals(message.getPayload(), result.getPayload());
	}
}
