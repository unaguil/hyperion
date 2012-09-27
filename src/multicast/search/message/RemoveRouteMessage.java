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
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

/**
 * This class defines a message which is used to remove invalid routes
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RemoveRouteMessage extends RemoteMessage {
	
	private final Set<MessageID> lostRoutes = new HashSet<MessageID>();
	private final Set<MessageID> removedSearches = new HashSet<MessageID>();
	
	public RemoveRouteMessage() {
		super(MessageTypes.REMOVE_ROUTE_MESSAGE);
	}

	/**
	 * Constructor of the remove route message.
	 * 
	 * @param source
	 *            the source of the message
	 * @param lostRoutes
	 *            the routes which have to be removed
	 */
	public RemoveRouteMessage(final PeerID source, final Set<PeerID> expectedDestinations, final Set<MessageID> lostRoutes) {
		super(MessageTypes.REMOVE_ROUTE_MESSAGE, source, null, expectedDestinations);
		this.lostRoutes.addAll(lostRoutes);
	}

	/**
	 * Constructor of the remove route message using another message as base,
	 * 
	 * @param removeRouteMessage
	 *            the message used as base
	 * @param sender
	 *            the new sender of the message
	 * @param newDistance
	 *            the new distance traveled by the message
	 */
	public RemoveRouteMessage(final RemoveRouteMessage removeRouteMessage, final PeerID sender,
							  final Set<PeerID> expectedDestinations, final Set<MessageID> lostRoutes, 
							  final Set<MessageID> removedSearches, final int newDistance) {
		super(removeRouteMessage, sender, expectedDestinations, newDistance);
		this.lostRoutes.addAll(lostRoutes);
		this.removedSearches.addAll(removedSearches);
	}

	/**
	 * Gets the routes which have been lost
	 * 
	 * @return the lost routes
	 */
	public Set<MessageID> getLostRoutes() {
		return lostRoutes;
	}
	
	public Set<MessageID> getRemovedSearches() {
		return removedSearches;
	}
	
	@Override
	public String toString() {
		return super.toString() + " (LR: " + lostRoutes + ")";
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readMessageIDs(lostRoutes, in);
		SerializationUtils.readMessageIDs(removedSearches, in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(lostRoutes, out);
		SerializationUtils.writeCollection(removedSearches, out);
	}
}
