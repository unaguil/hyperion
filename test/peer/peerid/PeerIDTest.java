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

package peer.peerid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

public class PeerIDTest {

	@Test
	public void testEquals() {
		final PeerID peerID1 = new PeerID("0");
		final PeerID peerID2 = new PeerID("1");
		final PeerID peerID3 = new PeerID("0");
		
		assertTrue(peerID1.equals(peerID1));
		assertFalse(peerID1.equals(peerID2));
		assertTrue(peerID1.equals(peerID3));
	}
	
	@Test
	public void testSerialization() throws IOException {
		final PeerID peerID1 = new PeerID("1");
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		peerID1.write(out);
		out.close();
		
		assertEquals(10, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final PeerID peerID2 = new PeerID();
		peerID2.read(in);
		in.close();
		
		assertEquals(peerID1, peerID2);
	}
}
