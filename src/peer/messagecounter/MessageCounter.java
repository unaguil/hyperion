package peer.messagecounter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import peer.message.BroadcastMessage;
import util.logger.Logger;

/**
 * This class is used for gathering statistical information about sent and
 * received messages.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public final class MessageCounter {

	final static class Info {

		private long numMessages = 0;
		private long updateTime = 0;

		public void inc() {
			numMessages++;
			updateTime = System.currentTimeMillis() - Logger.getDeltaTime();
		}

		public long getNumMessages() {
			return numMessages;
		}

		public long getUpdateTime() {
			return updateTime;
		}
	}

	private final Map<Class<? extends BroadcastMessage>, Info> received = new HashMap<Class<? extends BroadcastMessage>, Info>();
	private final Map<Class<? extends BroadcastMessage>, Info> sent = new HashMap<Class<? extends BroadcastMessage>, Info>();
	private final Map<Class<? extends BroadcastMessage>, Info> broadcasted = new HashMap<Class<? extends BroadcastMessage>, Info>();
	private final Map<Class<? extends BroadcastMessage>, Info> receivedPacket = new HashMap<Class<? extends BroadcastMessage>, Info>();

	private long totalProcessTime = 0;
	private long totalProcessedMessages = 0;

	private long maxMessageSize = 0;
	private long totalSize = 0;

	public MessageCounter() {
		TotalMessageCounter.addCounter(this);
	}

	public void addReceived(final Class<? extends BroadcastMessage> clazz) {
		inc(received, clazz);
	}
	
	public void addReceivedPacket(final Class<? extends BroadcastMessage> clazz) {
		inc(receivedPacket, clazz);
	}

	public void addSent(final Class<? extends BroadcastMessage> clazz) {
		inc(sent, clazz);
	}
	
	public void addBroadcasted(final Class<? extends BroadcastMessage> clazz) {
		inc(broadcasted, clazz);
	}

	public void addMessageSize(final int size) {
		maxMessageSize = size > maxMessageSize ? size : maxMessageSize;
		totalSize += size;
	}

	public synchronized void addProcessTime(final long processTime) {
		totalProcessTime += processTime;
		totalProcessedMessages++;
	}

	private void inc(final Map<Class<? extends BroadcastMessage>, Info> map, final Class<? extends BroadcastMessage> clazz) {
		if (!map.containsKey(clazz))
			map.put(clazz, new Info());

		final Info info = map.get(clazz);
		info.inc();
	}

	public long getReceived(final Class<? extends BroadcastMessage> clazz) {
		if (received.containsKey(clazz))
			return received.get(clazz).getNumMessages();

		return 0;
	}

	public long getSent(final Class<? extends BroadcastMessage> clazz) {
		if (sent.containsKey(clazz))
			return sent.get(clazz).getNumMessages();

		return 0;
	}

	public long getReceived() {
		return sumCollection(received.values());
	}

	public long getSent() {
		return sumCollection(sent.values());
	}
	
	public long getBroadcasted() {
		return sumCollection(broadcasted.values());
	}
	
	public long getReceivedPacket() {
		return sumCollection(receivedPacket.values());
	}

	public long getMaxMessageSize() {
		return maxMessageSize;
	}

	public float getAvgMessageSize() {
		if (getSent() == 0)
			return 0.0f;

		return totalSize / (float) getSent();
	}

	private long sumCollection(final Collection<Info> values) {
		long total = 0;
		for (final Info info : values)
			total += info.getNumMessages();
		return total;
	}

	public Set<Class<? extends BroadcastMessage>> getClasses() {
		final Set<Class<? extends BroadcastMessage>> classes = new HashSet<Class<? extends BroadcastMessage>>();

		classes.addAll(received.keySet());
		classes.addAll(sent.keySet());

		return classes;
	}

	public long getLastSentMessageTime(final Class<? extends BroadcastMessage> clazz) {
		if (sent.containsKey(clazz))
			return sent.get(clazz).getUpdateTime();

		return 0;
	}

	public long getLastReceivedMessageTime(final Class<? extends BroadcastMessage> clazz) {
		if (received.containsKey(clazz))
			return received.get(clazz).getUpdateTime();

		return 0;
	}

	public synchronized float getAvgProcessTime() {
		if (totalProcessedMessages == 0)
			return 0;
		return (totalProcessTime / (float) totalProcessedMessages);
	}

	@Override
	public String toString() {
		final StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("*********************************\n");
		for (final Class<? extends BroadcastMessage> clazz : received.keySet()) {
			strBuilder.append(clazz.getName() + " received : " + getReceived(clazz) + "\n");
			strBuilder.append(clazz.getName() + " sent: " + getSent(clazz) + "\n");
		}
		strBuilder.append("Broadcasted packets: " + getBroadcasted() + "\n");
		strBuilder.append("Received packets: " + getReceivedPacket() + "\n");
		strBuilder.append("Received msgs: " + getReceived() + "\n");
		strBuilder.append("Sent msgs: " + getSent() + "\n");
		strBuilder.append("Avg process time: " + getAvgProcessTime() + "\n");
		strBuilder.append("*********************************\n");

		return strBuilder.toString();
	}
}
