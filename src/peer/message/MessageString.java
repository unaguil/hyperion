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

package peer.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

/**
 * Example implementation of a message.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class MessageString extends BroadcastMessage {

	private final String content;
	
	public MessageString() {
		super(MessageTypes.MESSAGE_STRING);
		content = new String();
	}

	public MessageString(final PeerID sender, final Set<PeerID> expectedDestinations, final String content) {
		super(MessageTypes.MESSAGE_STRING, sender, expectedDestinations);
		this.content = content;
	}

	@Override
	public String toString() {
		return content;
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);		
		SerializationUtils.setFinalField(MessageString.class, this, "content", in.readUTF());
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);		
		out.writeUTF(content);
	}
}
