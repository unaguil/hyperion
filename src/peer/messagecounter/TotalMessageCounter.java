package peer.messagecounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import peer.message.BroadcastMessage;

/**
 * This class provides statistical information about total sent and received
 * messages.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class TotalMessageCounter {

	private static final List<MessageCounter> msgCounters = Collections.synchronizedList(new ArrayList<MessageCounter>());

	private static final Logger logger = Logger.getLogger(TotalMessageCounter.class);

	public static void addCounter(final MessageCounter msgCounter) {
		msgCounters.add(msgCounter);
	}

	public static long getTotalReceived() {
		long total = 0;
		for (final MessageCounter msgCounter : msgCounters)
			total += msgCounter.getReceived();
		return total;
	}

	public static long getTotalSent() {
		long total = 0;
		for (final MessageCounter msgCounter : msgCounters)
			total += msgCounter.getSent();
		return total;
	}

	public static long getTotalReceived(final Class<? extends BroadcastMessage> clazz) {
		long total = 0;
		for (final MessageCounter msgCounter : msgCounters)
			total += msgCounter.getReceived(clazz);
		return total;
	}

	public static long getTotalSent(final Class<? extends BroadcastMessage> clazz) {
		long total = 0;
		for (final MessageCounter msgCounter : msgCounters)
			total += msgCounter.getSent(clazz);
		return total;
	} 

	public static long getLastTimeReceived(final Class<? extends BroadcastMessage> clazz) {
		final List<Long> values = new ArrayList<Long>();
		for (final MessageCounter msgCounter : msgCounters)
			values.add(Long.valueOf(msgCounter.getLastReceivedMessageTime(clazz)));

		return Collections.max(values).longValue();
	}

	public static long getLastTimeSent(final Class<? extends BroadcastMessage> clazz) {
		final List<Long> values = new ArrayList<Long>();
		for (final MessageCounter msgCounter : msgCounters)
			values.add(Long.valueOf(msgCounter.getLastSentMessageTime(clazz)));

		return Collections.max(values).longValue();
	}

	public static Set<Class<? extends BroadcastMessage>> getClasses() {
		final Set<Class<? extends BroadcastMessage>> classes = new HashSet<Class<? extends BroadcastMessage>>();
		for (final MessageCounter msgCounger : msgCounters)
			classes.addAll(msgCounger.getClasses());
		return classes;
	}

	public static float getAvgProcessTimeMillis() {
		if (msgCounters.isEmpty())
			return 0;

		long totalProcessTime = 0;

		for (final MessageCounter msgCounter : msgCounters)
			totalProcessTime += msgCounter.getAvgProcessTime();

		return (totalProcessTime / (float) msgCounters.size()) / 1000000.0f;
	}
	
	public static float getAvgMessageSize() {
		if (msgCounters.isEmpty())
			return 0.0f;
		
		float total = 0.0f;
		for (final MessageCounter msgCounter : msgCounters)
			total += msgCounter.getAvgMessageSize();
		return total / msgCounters.size();
	}
	
	public static long getMaxMessageSize() {
		long maxMessageSize = 0;
		for (final MessageCounter msgCounter : msgCounters)
			maxMessageSize = msgCounter.getMaxMessageSize() > maxMessageSize?msgCounter.getMaxMessageSize():maxMessageSize;
		return maxMessageSize;
	}

	public static void displayStatistics() {
		final StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("*********************************\n");
		final Set<Class<? extends BroadcastMessage>> classes = getClasses();
		for (final Class<? extends BroadcastMessage> clazz : classes) {
			strBuilder.append(clazz.getName() + " sent: " + getTotalSent(clazz) + "\n");
			strBuilder.append(clazz.getName() + " last time sent: " + getLastTimeSent(clazz) + "\n");
			strBuilder.append(clazz.getName() + " received : " + getTotalReceived(clazz) + "\n");
			strBuilder.append(clazz.getName() + " last time received : " + getLastTimeReceived(clazz) + "\n");

		}
		strBuilder.append("Total received messages: " + getTotalReceived() + "\n");
		strBuilder.append("Total sent messages: " + getTotalSent() + "\n");
		strBuilder.append("Avg process time: " + getAvgProcessTimeMillis() + " ms\n");
		strBuilder.append("Avg message size: " + getAvgMessageSize() + " bytes\n");
		strBuilder.append("Max message size: " + getMaxMessageSize() + " bytes\n");
		strBuilder.append("*********************************\n");
		System.out.println(strBuilder.toString());
	}

	public static void logStatistics() {
		final Set<Class<? extends BroadcastMessage>> classes = getClasses();
		for (final Class<? extends BroadcastMessage> clazz : classes) {
			logger.info(clazz.getName() + " sent: " + getTotalSent(clazz));
			logger.info(clazz.getName() + " last time sent: " + getLastTimeSent(clazz));
			logger.info(clazz.getName() + " received: " + getTotalReceived(clazz));
			logger.info(clazz.getName() + " last time received : " + getLastTimeReceived(clazz));
		}
		logger.info("Total received messages: " + getTotalReceived());
		logger.info("Total sent messages: " + getTotalSent());
		logger.info("Avg process time: " + getAvgProcessTimeMillis() + " ms");
		logger.info("Avg message size: " + getAvgMessageSize() + " bytes");
		logger.info("Max message size: " + getMaxMessageSize() + " bytes");
	}
}
