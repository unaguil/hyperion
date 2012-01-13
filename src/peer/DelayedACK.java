package peer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import peer.message.ACKMessage;
import peer.message.BroadcastMessage;
import peer.message.BundleMessage;
import util.WaitableThread;
import util.logger.Logger;
import util.timer.TimerTask;

class DelayedACK implements TimerTask {

	private final Set<ACKMessage> ackMessages = new HashSet<ACKMessage>();
	private final Random r = new Random();
	
	private final BasicPeer peer;
	
	private boolean running = false;
	
	private Delayer delayer;
	
	private final Logger logger = Logger.getLogger(DelayedACK.class);
	
	class Delayer extends WaitableThread {
		
		private final long delayTime;
		
		private final TimerTask task;
		
		public Delayer(final long delayTime, final TimerTask task) {
			this.delayTime = delayTime;
			this.task = task;
		}
		
		@Override
		public void run() {
			if (delayTime > 0) {
				try {
					WaitableThread.mySleep(delayTime);
				} catch (InterruptedException e) {
					interrupt();
				}
			} 
			
			if (!Thread.interrupted()) {
				try {
					task.perform();
				} catch (InterruptedException e) {
					interrupt();
				}
			}
			
			threadFinished();			
		}
	}
	
	public DelayedACK(final BasicPeer peer) {
		this.peer = peer;
	}
	
	public void enqueueACKMessage(final ACKMessage ackMessage, final int maxJitter) {
		synchronized (ackMessages) {			
			if (!running) {
				final long delayTime = r.nextInt(maxJitter);
				running = true;
				delayer = new Delayer(delayTime, this);
				delayer.start();
			}
			
			ackMessages.add(ackMessage);
		}
	}
	
	@Override
	public void perform() {
		final Set<BroadcastMessage> sentMessages = new HashSet<BroadcastMessage>();
		synchronized (ackMessages) {
			sentMessages.addAll(ackMessages);
			running = false;
		}
		
		logger.trace("Peer " + peer.getPeerID() + " sending bundled message with " + sentMessages.size() + " ACK messages");
		
		final BundleMessage bundleMessage = new BundleMessage(peer.getPeerID(), new ArrayList<BroadcastMessage>(sentMessages));
		peer.broadcast(bundleMessage);
	}
	
	public void stopAndWait() {
		synchronized (ackMessages) {
			if (running) {
				delayer.stopAndWait();
				running = false;
			}
		}
	}
}
