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

package peer.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

public class BundleMessage extends BroadcastMessage implements BigEnvelopeMessage {

	private final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>();
	
	public BundleMessage() {
		super(MessageTypes.BUNDLE_MESSAGE);
	}

	public BundleMessage(final PeerID sender, final List<BroadcastMessage> messages) {
		super(MessageTypes.BUNDLE_MESSAGE, sender, Collections.<PeerID> emptySet());
		
		addMessages(messages);
	}

	@Override
	public List<BroadcastMessage> getPayloadMessages() {
		return messages;
	}
	
	@Override
	public boolean hasPayload() {
		return !messages.isEmpty();
	}

	@Override
	public boolean removeDestination(final PeerID dest) {
		//Remove all those message whose destinations were removed
		for (final Iterator<BroadcastMessage> it = messages.iterator(); it.hasNext(); ) {
			BroadcastMessage broadcastMessage = it.next();
			if (broadcastMessage.removeDestination(dest)) {
				it.remove();
			}
		}
		
		//remove all destinations which have not related messages
		final Set<PeerID> reallyExpectedDestinations = new HashSet<PeerID>();
		for (final BroadcastMessage broadcastMessage : messages)
			reallyExpectedDestinations.addAll(broadcastMessage.getExpectedDestinations());
		
		expectedDestinations.retainAll(reallyExpectedDestinations);
		
		return expectedDestinations.isEmpty();
	}

	@Override
	public String toString() {
		return getType() +  " " + getMessageID() + " [" + messages.size() + "]";
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		 
		final byte nDest = in.readByte();
		for (int i = 0; i < nDest; i++) {
			final PeerID peerID = new PeerID();
			peerID.read(in);
			expectedDestinations.add(peerID);
		}
		
		try {
			final byte nMessages = in.readByte();
			for (int i = 0; i < nMessages; i++) {
				final BroadcastMessage message = MessageTypes.readBroadcastMessage(in);
				messages.add(message);
			}
		} catch (UnsupportedTypeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(expectedDestinations, out);
		SerializationUtils.writeCollection(messages, out);
	}
	
	public Set<BroadcastMessage> removeACKMessages() {
		final Set<BroadcastMessage> removedACKMessages = new HashSet<BroadcastMessage>();
		for (final Iterator<BroadcastMessage> it = messages.iterator(); it.hasNext();) {
			BroadcastMessage broadcastMessage = it.next();
			if (broadcastMessage instanceof ACKMessage) {
				removedACKMessages.add(broadcastMessage);
				it.remove();			
			}
		}
		return removedACKMessages;
	}
	
	public void addMessages(final List<BroadcastMessage> addedMessages) {
		this.messages.addAll(addedMessages);
		
		for (final BroadcastMessage message : addedMessages)
			expectedDestinations.addAll(message.getExpectedDestinations());
	}
}
