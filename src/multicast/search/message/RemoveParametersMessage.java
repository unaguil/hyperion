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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import peer.message.MessageID;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;
import taxonomy.parameter.Parameter;

/**
 * This class defines a message used to remove a parameter route.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RemoveParametersMessage extends RemoteMessage {

	private final Map<MessageID, Set<Parameter>> removedParameters = new HashMap<MessageID, Set<Parameter>>();

	public RemoveParametersMessage() {
		super(MessageTypes.REMOVE_PARAM_MESSAGE);
	}
	
	/**
	 * Constructor of the class.
	 * 
	 * @param parameters
	 *            the removed parameters
	 * @param routeIDs
	 *            the identifier of the routes parameters are removed from
	 * @param source
	 *            the source of the message
	 */
	public RemoveParametersMessage(final PeerID source, final Set<PeerID> expectedDestinations, final Map<MessageID, Set<Parameter>> removedParameters) {
		super(MessageTypes.REMOVE_PARAM_MESSAGE, source, null, expectedDestinations);
		this.removedParameters.putAll(removedParameters);
	}

	/**
	 * Constructor of the message using another message as base.
	 * 
	 * @param removeParamRouteMessage
	 *            the message to use as base
	 * @param sender
	 *            the new sender of the message
	 * @param through
	 *            the neighbor used to send the message
	 * @param newDistance
	 *            the new distance for this message
	 */
	public RemoveParametersMessage(final RemoveParametersMessage removeParamRouteMessage, final PeerID sender, final Set<PeerID> expectedDestinations, final int newDistance) {
		super(removeParamRouteMessage, sender, expectedDestinations, newDistance);
		this.removedParameters.putAll(removeParamRouteMessage.removedParameters);
	}

	public Map<MessageID, Set<Parameter>> getRemovedParameters() {
		return removedParameters;
	}

	@Override
	public String toString() {
		return super.toString() + " removedParameters: " + removedParameters;
	}
	
	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		try {
			SerializationUtils.readParametersMap(removedParameters, in);
		} catch (UnsupportedTypeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeParametersMap(removedParameters, out);
	}
}
