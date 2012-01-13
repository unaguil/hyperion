package peer;

import java.util.ArrayDeque;
import java.util.Deque;

import peer.message.BroadcastMessage;
import util.WaitableThread;

class ReceivedProcessor {

	private final Deque<BroadcastMessage> receivedMessages = new ArrayDeque<BroadcastMessage>();
	
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
				
				BroadcastMessage message = null; 
				synchronized (receivedMessages) {
					if (!receivedMessages.isEmpty())
						message = receivedMessages.poll();
				}
				
				if (message != null)
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
