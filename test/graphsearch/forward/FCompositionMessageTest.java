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

package graphsearch.forward;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;
import graphsearch.forward.message.FCompositionMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;

import org.junit.Test;

import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;

public class FCompositionMessageTest {
	
	@Test
	public void testSerialization() throws IOException, UnsupportedTypeException {
		final ServiceDistance sDistance = new ServiceDistance(new Service("S1", new PeerID("4")), new Integer(3));
		final FCompositionMessage fCompositionMessage = new FCompositionMessage(new SearchID(new PeerID("3")), new Service("S0", new PeerID("0")), Collections.singleton(sDistance), 5, 230);
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		fCompositionMessage.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final FCompositionMessage result = (FCompositionMessage) MessageTypes.readBroadcastMessage(in);
		in.close();
		
		assertEquals(fCompositionMessage, result);
		assertTrue(fCompositionMessage.getComposition().containsAll(result.getComposition()));
		assertTrue(result.getComposition().containsAll(fCompositionMessage.getComposition()));
		
		assertTrue(fCompositionMessage.getDestServices().containsAll(result.getDestServices()));
		assertTrue(result.getDestServices().containsAll(fCompositionMessage.getDestServices()));
		
		assertEquals(fCompositionMessage.getHops(), result.getHops());
		assertEquals(fCompositionMessage.getDistance(), result.getDistance());
	}
	
	@Test
	public void testEquals() {
		final ServiceDistance sDistance = new ServiceDistance(new Service("S1", new PeerID("4")), new Integer(3));
		final FCompositionMessage fCompositionMessage = new FCompositionMessage(new SearchID(new PeerID("3")), new Service("S0", new PeerID("0")), Collections.singleton(sDistance), 5, 230);
		
		final ServiceDistance sDistance2 = new ServiceDistance(new Service("S1", new PeerID("4")), new Integer(3));
		final FCompositionMessage fCompositionMessage2 = new FCompositionMessage(new SearchID(new PeerID("3")), new Service("S1", new PeerID("0")), Collections.singleton(sDistance2), 5, 230);
						
		assertEquals(fCompositionMessage, fCompositionMessage);
		assertFalse(fCompositionMessage.equals(fCompositionMessage2));
	}
}
