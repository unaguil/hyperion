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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import peer.peerid.PeerID;

public class MessageIDTest {

	@Test
	public void testEquals() {
		final MessageID messageID1 = new MessageID(new PeerID("0"), (short) 320);
		final MessageID messageID2 = new MessageID(new PeerID("3"), (short) 320);
		final MessageID messageID3 = new MessageID(new PeerID("0"), (short) 350);
		final MessageID messageID4 = new MessageID(new PeerID("0"), (short) 320);
		
		assertTrue(messageID1.equals(messageID1));
		assertFalse(messageID1.equals(messageID2));
		assertFalse(messageID1.equals(messageID3));
		assertTrue(messageID1.equals(messageID4));
	}
	
	@Test
	public void testSerialization() throws IOException {
		final MessageID messageID1 = new MessageID(new PeerID("3"), (short) 320);
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		messageID1.write(out);
		out.close();
		
		assertEquals(12, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final MessageID messageID2 = new MessageID();
		messageID2.read(in);
		in.close();
		
		assertEquals(messageID1, messageID2);
	}
}
