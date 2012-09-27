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

package peer;

import java.util.ArrayList;
import java.util.List;

import peer.message.BroadcastMessage;
import peer.message.MessageReceivedListener;
import peer.messagecounter.MessageCounter;
import util.WaitableThread;

class ReceivedProcessor {

	private final List<BroadcastMessage> receivedMessages = new ArrayList<BroadcastMessage>();
	
	private final long SLEEP_TIME = 3;
	
	private BasicPeer peer;
	
	private final Processor processor = new Processor(); 
	
	private final ReceivingThread receivingThread;
	
	private class Processor extends WaitableThread {
		
		@Override
		public void run() {
			while (!Thread.interrupted()) {
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					interrupt();
				}
				
				final List<BroadcastMessage> messages = new ArrayList<BroadcastMessage>(); 
				synchronized (receivedMessages) {
					messages.addAll(receivedMessages);
					receivedMessages.clear();
				}
				
				for (final BroadcastMessage message : messages)
					peer.receiveMessage(message);
			}
			
			threadFinished();
		}
	}
	
	public ReceivedProcessor(final BasicPeer peer, final MessageCounter msgCounter) {
		this.peer = peer;
		this.receivingThread = new ReceivingThread(this, peer, msgCounter);
	}
	
	public void start() {
		processor.start();
		receivingThread.start();
	}
	
	public void stopAndWait() {
		receivingThread.stopAndWait();
		processor.stopAndWait();
	}
	
	public void enqueuReceivedMessage(final BroadcastMessage broadcastMessage) {
		synchronized (receivedMessages) {
			receivedMessages.add(broadcastMessage);
		}
	}
	
	public BasicPeer getPeer() {
		return peer;
	}

	public void setHearListerner(MessageReceivedListener hearListener) {
		receivingThread.setHearListener(hearListener);
	}
}
