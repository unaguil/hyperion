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

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.message.MessageStringPayload;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class SearchResponseMessageTest {
	
	private final Taxonomy emptyTaxonomy = new BasicTaxonomy();
	
	@Test
	public void testSerialization() throws IOException, UnsupportedTypeException, InvalidParameterIDException {
		final Set<Parameter> foundParameters = new HashSet<Parameter>();
		foundParameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		foundParameters.add(ParameterFactory.createParameter("I-2", emptyTaxonomy));
		
		final SearchResponseMessage searchResponseMessage = new SearchResponseMessage(new PeerID("0"), new PeerID("3"), 
						foundParameters, new MessageStringPayload(new PeerID("0"), "Hola, mundo"), new MessageID(new PeerID("3"), MessageIDGenerator.getNewID()));
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		searchResponseMessage.write(out);
		out.close();
		
		assertEquals(65, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final SearchResponseMessage result = (SearchResponseMessage) MessageTypes.readBroadcastMessage(in);
		in.close();
		assertEquals(searchResponseMessage, result);
		assertEquals(searchResponseMessage.getRespondedRouteID(), result.getRespondedRouteID());
		assertTrue(searchResponseMessage.getParameters().containsAll(result.getParameters()));
		assertTrue(result.getParameters().containsAll(searchResponseMessage.getParameters()));
	}
}
