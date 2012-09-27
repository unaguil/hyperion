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

package graphcreation.collisionbased.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

public class ForwardMessage extends RemoteMessage {

	private final Set<PeerID> destinations = new HashSet<PeerID>();
	
	public ForwardMessage() {
		super(MessageTypes.FORWARD_MESSAGE);
	}

	public ForwardMessage(final PeerID source, final BroadcastMessage payload, final Set<PeerID> destinations) {
		super(MessageTypes.FORWARD_MESSAGE, source, payload, Collections.<PeerID> emptySet());
		this.destinations.addAll(destinations);
	}

	public Set<PeerID> getDestinations() {
		return destinations;
	}

	@Override
	public BroadcastMessage copy() {
		return new ForwardMessage(getSource(), getPayload().copy(), getDestinations());
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readPeers(destinations, in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(destinations, out);
	}
}
