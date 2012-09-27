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
import java.util.HashSet;
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
public class GeneralizeSearchMessage extends RemoteMessage {


	// the new parameters
	private final Set<Parameter> parameters = new HashSet<Parameter>();

	// the route identifiers which parameters are generalized
	private final Set<MessageID> routeIDs = new HashSet<MessageID>();
	
	public GeneralizeSearchMessage() {
		super(MessageTypes.GENERALIZE_SEARCH_MESSAGE);
	}

	/**
	 * Constructor of the class.
	 * 
	 * @param parameters
	 *            the generalized parameters
	 * @param routeIDs
	 *            the identifier of the routes parameters whose parameters are
	 *            generalized
	 * @param source
	 *            the source of the message
	 */
	public GeneralizeSearchMessage(final PeerID source, final Set<PeerID> expectedDestinations, final Set<Parameter> parameters, final Set<MessageID> routeIDs) {
		super(MessageTypes.GENERALIZE_SEARCH_MESSAGE, source, null, expectedDestinations);
		this.parameters.addAll(parameters);
		this.routeIDs.addAll(routeIDs);
	}

	/**
	 * Constructor of the message using another message as base.
	 * 
	 * @param generalizeSearchMessage
	 *            the message to use as base
	 * @param sender
	 *            the new sender of the message
	 * @param through
	 *            the neighbor used to send the message
	 * @param newDistance
	 *            the new distance for this message
	 */
	public GeneralizeSearchMessage(final GeneralizeSearchMessage generalizeSearchMessage, final PeerID sender, final Set<PeerID> expectedDestinations, final int newDistance) {
		super(generalizeSearchMessage, sender, expectedDestinations, newDistance);
		this.routeIDs.addAll(generalizeSearchMessage.routeIDs);
		this.parameters.addAll(generalizeSearchMessage.parameters);
	}

	/**
	 * Gets the generalized parameters
	 * 
	 * @return the removed parameters
	 */
	public Set<Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Gets the route identifiers
	 * 
	 * @return the route identifier
	 */
	public Set<MessageID> getRouteIDs() {
		return routeIDs;
	}

	@Override
	public String toString() {
		return super.toString() + " P: " + getParameters() + " Routes: " + getRouteIDs();
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
			throw new IOException();
		}
		
		final byte nRoutes = in.readByte();
		for (int i = 0; i < nRoutes; i++) {
			final MessageID messageID = new MessageID();
			messageID.read(in);
			routeIDs.add(messageID);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(parameters, out);
		SerializationUtils.writeCollection(routeIDs, out);
	}
}
