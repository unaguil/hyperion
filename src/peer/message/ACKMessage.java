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
import java.util.Collections;

import peer.peerid.PeerID;

public class ACKMessage extends BroadcastMessage {

	private final MessageID respondingTo;
	
	public ACKMessage() {
		super(MessageTypes.ACK_MESSAGE);
		respondingTo = new MessageID();
	}

	public ACKMessage(final PeerID sender, final MessageID respondingTo) {
		super(MessageTypes.ACK_MESSAGE, sender, Collections.<PeerID> emptySet());
		this.respondingTo = respondingTo;
	}

	public MessageID getRespondedMessageID() {
		return respondingTo;
	}

	@Override
	public String toString() {
		return getType() + " " + respondingTo;
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		respondingTo.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		respondingTo.write(out);
	}
	
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ACKMessage))
			return false;
		
		final ACKMessage ackMessage = (ACKMessage)o;
		return ackMessage.respondingTo.equals(this.respondingTo);
	}
	
	@Override
	public int hashCode() {
		return respondingTo.hashCode();
	}
}
