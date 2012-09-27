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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multicast.SearchedParameter;

import org.junit.Test;

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;

public class RemoveParametersMessageTest {
	
	private final Taxonomy emptyTaxonomy = new BasicTaxonomy();
	
	@Test
	public void testSerialization() throws IOException, UnsupportedTypeException, InvalidParameterIDException {
		final Set<SearchedParameter> searchedParameters = new HashSet<SearchedParameter>();
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-1", emptyTaxonomy), 3));
		searchedParameters.add(new SearchedParameter(ParameterFactory.createParameter("I-2", emptyTaxonomy), 3));
		final Map<MessageID, Set<Parameter>> removedParameters = new HashMap<MessageID, Set<Parameter>>();
		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.add(ParameterFactory.createParameter("I-1", emptyTaxonomy));
		parameters.add(ParameterFactory.createParameter("O-10", emptyTaxonomy));
		removedParameters.put(new MessageID(new PeerID("3"), MessageIDGenerator.getNewID()), parameters);
		
		final RemoveParametersMessage removeParametersMessage = new RemoveParametersMessage(new PeerID("3"), Collections.<PeerID>emptySet(), removedParameters);
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		removeParametersMessage.write(out);
		out.close();
		
		assertEquals(35, bos.toByteArray().length);
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		final ObjectInputStream in = new ObjectInputStream(bis);
		
		final RemoveParametersMessage result = (RemoveParametersMessage) MessageTypes.readBroadcastMessage(in);
		in.close();
		assertEquals(removeParametersMessage, result);
		assertTrue(removeParametersMessage.getRemovedParameters().keySet().containsAll(result.getRemovedParameters().keySet()));
		assertTrue(result.getRemovedParameters().keySet().containsAll(removeParametersMessage.getRemovedParameters().keySet()));
		assertTrue(removeParametersMessage.getRemovedParameters().values().containsAll(result.getRemovedParameters().values()));
		assertTrue(result.getRemovedParameters().values().containsAll(removeParametersMessage.getRemovedParameters().values()));
	}
}
