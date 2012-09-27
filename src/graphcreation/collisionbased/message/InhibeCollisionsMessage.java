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

public class InhibeCollisionsMessage extends RemoteMessage {

	private final Set<Inhibition> inhibitedCollisions = new HashSet<Inhibition>();
	
	public InhibeCollisionsMessage() {
		super(MessageTypes.INHIBE_COLLISIONS_MESSAGE);
	}

	public InhibeCollisionsMessage(final PeerID source, final Set<Inhibition> inhibitions) {
		super(MessageTypes.INHIBE_COLLISIONS_MESSAGE, source, null, Collections.<PeerID> emptySet());
		inhibitedCollisions.addAll(inhibitions);
	}

	public Set<Inhibition> getInhibedCollisions() {
		return inhibitedCollisions;
	}

	@Override
	public String toString() {
		return inhibitedCollisions.toString();
	}

	@Override
	public BroadcastMessage copy() {
		return new InhibeCollisionsMessage(getSource(), getInhibedCollisions());
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		final byte sCollisions = in.readByte();
		for (int i = 0; i < sCollisions; i++) {
			final Inhibition inhibition = new Inhibition();
			inhibition.read(in);
			inhibitedCollisions.add(inhibition);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(inhibitedCollisions, out);
	}
}
