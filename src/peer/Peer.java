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

package peer;

import java.io.IOException;
import java.util.Set;

import peer.message.BroadcastMessage;
import peer.message.MessageReceivedListener;
import peer.message.MessageSentListener;
import peer.peerid.PeerID;

public interface Peer {

	public void addCommunicationLayer(CommunicationLayer layer, Set<Class<? extends BroadcastMessage>> messageClasses) throws RegisterCommunicationLayerException;

	public void addReceivingListener(Class<? extends BroadcastMessage> messageClass, MessageReceivedListener receivedListener) throws AlreadyRegisteredListenerException;

	public void setHearListener(MessageReceivedListener hearListener);

	public void addSentListener(MessageSentListener sentListener);

	public void directBroadcast(BroadcastMessage message);

	public PeerID getPeerID();

	public void initPeer(PeerID peerID) throws IOException;

	public void stopPeer();

	public void printStatistics();
	
	public boolean isInitialized();
}
