package peer;

import java.util.ArrayList;
import java.util.List;

import peer.message.BroadcastMessage;
import util.WaitableThread;

class ReceivedProcessor {

	private final List<BroadcastMessage> receivedMessages = new ArrayList<BroadcastMessage>();
	
	private final long SLEEP_TIME = 3;
	
	private BasicPeer peer;
	
	private final Processor processor = new Processor(); 
	
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
					peer.processMessage(message);
			}
			
			threadFinished();
		}
	}
	
	public ReceivedProcessor(final BasicPeer peer) {
		this.peer = peer;
	}
	
	public void start() {
		processor.start();
	}
	
	public void stopAndWait() {
		processor.stopAndWait();
	}
	
	public void enqueuReceivedMessage(final BroadcastMessage broadcastMessage) {
		synchronized (receivedMessages) {
			receivedMessages.add(broadcastMessage);
		}
	}
}
