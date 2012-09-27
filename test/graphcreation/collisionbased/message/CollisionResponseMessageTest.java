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

package graphcreation.collisionbased.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import graphcreation.services.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;

public class CollisionResponseMessageTest {
	
	@Test
	public void testSerialization() throws IOException, UnsupportedTypeException {
		final Map<Service, Byte> serviceTable = new HashMap<Service, Byte>();
		serviceTable.put(new Service("S0", new PeerID("0")), new Byte((byte) 0x01));
		serviceTable.put(new Service("S0", new PeerID("0")), new Byte((byte) 0x02));
		final CollisionResponseMessage collisionResponseMessage = new CollisionResponseMessage(new PeerID("3"), serviceTable);
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		collisionResponseMessage.write(out);
		out.close();
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		final CollisionResponseMessage result = (CollisionResponseMessage) MessageTypes.readBroadcastMessage(in);
		in.close();
		
		assertEquals(collisionResponseMessage, result);
		assertTrue(collisionResponseMessage.getServices().containsAll(result.getServices()));
		assertTrue(result.getServices().containsAll(collisionResponseMessage.getServices()));
		
		for (final Service service : collisionResponseMessage.getServices()) {
			assertEquals(collisionResponseMessage.getDistance(service), result.getDistance(service)); 
		}
	}
}
