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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import peer.message.BroadcastMessage;
import peer.message.MessageStringPayload;
import peer.peerid.PeerID;

public class RemoteMulticastMessageTest {
	
	class DummyMessage extends RemoteMulticastMessage {

		private static final byte DUMMY_TYPE = 0x03;
		
		public DummyMessage() {
			super(DUMMY_TYPE);
		}
		
		public DummyMessage(final PeerID source, final BroadcastMessage payload, final Set<PeerID> remoteDestinations) {
			super(DUMMY_TYPE, source, remoteDestinations, payload, true);
		}
	}
	
	@Test
	public void testSerialization() throws IOException {
		final Set<PeerID> remoteDestinations = new HashSet<PeerID>();
		remoteDestinations.add(new PeerID("0"));
		remoteDestinations.add(new PeerID("1"));
		remoteDestinations.add(new PeerID("2"));
		final DummyMessage message = new DummyMessage(new PeerID("3"), new MessageStringPayload(new PeerID("3"), "Hola, mundo!"), remoteDestinations);
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		message.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final RemoteMulticastMessage result = new DummyMessage();
		in.readByte(); //read mType byte
		result.read(in);
		in.close();
		assertEquals(message, result);
		assertTrue(message.getRemoteDestinations().containsAll(result.getRemoteDestinations()));
		assertTrue(result.getRemoteDestinations().containsAll(message.getRemoteDestinations()));
		assertTrue(message.getThroughPeers().containsAll(result.getThroughPeers()));
		assertTrue(result.getThroughPeers().containsAll(message.getThroughPeers()));
	}
}
