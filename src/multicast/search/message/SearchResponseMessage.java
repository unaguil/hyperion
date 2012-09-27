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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;
import taxonomy.parameter.Parameter;

/**
 * This class defines those messages which are used to send a response message.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class SearchResponseMessage extends RemoteMulticastMessage {

	// the set of parameters found in the source node
	private final Set<Parameter> parameters = new HashSet<Parameter>();

	// the search route identifier which this message responds to
	private final MessageID respondedRouteID;
	
	public SearchResponseMessage() {
		super(MessageTypes.SEARCH_RESPONSE_MESSAGE);
		respondedRouteID = new MessageID();
	}

	/**
	 * Constructor of the search response message.
	 * @param source
	 *            the source of the message
	 * @param destination
	 *            the destination of the message
	 * @param foundParameters
	 *            the parameters found in this node
	 * @param payload
	 *            the payload of the response message
	 */
	public SearchResponseMessage(final PeerID source, final PeerID destination, final Set<Parameter> foundParameters, final BroadcastMessage payload, final MessageID respondedRouteID) {
		super(MessageTypes.SEARCH_RESPONSE_MESSAGE, source, Collections.singleton(destination), payload, false);
		this.parameters.addAll(foundParameters);
		this.respondedRouteID = respondedRouteID;
	}

	/**
	 * Constructor of the search response message. It uses another message as
	 * base.
	 * 
	 * @param searchResponseMessage
	 *            the message used as base
	 * @param sender
	 *            the new sender of the message
	 * @param through
	 *            the new peer used to send the message through
	 * @param respondingTo
	 *            the message this one is responding to
	 * @param newDistance
	 *            the new distance traversed by the message
	 */
	public SearchResponseMessage(final SearchResponseMessage searchResponseMessage, final PeerID sender, final PeerID through, final int newDistance) {
		super(searchResponseMessage, sender, Collections.singleton(through), newDistance);
		this.parameters.addAll(searchResponseMessage.parameters);
		this.respondedRouteID = searchResponseMessage.getRespondedRouteID();
	}

	/**
	 * Gets the parameters found in the remote node
	 * 
	 * @return a set containing the parameters found in the remote node
	 */
	public Set<Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Gets the remote destination of this message
	 * 
	 * @return the remote destination of this message
	 */

	public PeerID getRemoteDestination() {
		return getRemoteDestinations().iterator().next();
	}

	/**
	 * Gets the search route this message responds to
	 * 
	 * @return the search route this message responds to
	 */
	public MessageID getRespondedRouteID() {
		return respondedRouteID;
	}

	@Override
	public String toString() {
		return super.toString() + " P: " + getParameters();
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		try {
			final byte nParameters = in.readByte();
			for (int i = 0; i < nParameters; i++) {
				final Parameter p = Parameter.readParameter(in);
				parameters.add(p);
			}
		} catch (UnsupportedTypeException e) {
			throw new IOException(e);
		}
		
		respondedRouteID.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(parameters, out);
		respondedRouteID.write(out);
	}
}
