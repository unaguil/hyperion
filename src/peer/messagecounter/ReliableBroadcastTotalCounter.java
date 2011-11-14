package peer.messagecounter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import util.logger.Logger;

/**
 * This class provides statistical information about total sent and received
 * messages.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public final class ReliableBroadcastTotalCounter {

	private static final List<ReliableBroadcastCounter> msgCounters = Collections.synchronizedList(new ArrayList<ReliableBroadcastCounter>());

	private static final Logger logger = Logger.getLogger(ReliableBroadcastTotalCounter.class);

	public static void addCounter(final ReliableBroadcastCounter msgCounter) {
		msgCounters.add(msgCounter);
	}

	public static long getTotalBroadcasted() {
		long total = 0;
		for (final ReliableBroadcastCounter msgCounter : msgCounters)
			total += msgCounter.getBroadcastedMessages();
		return total;
	}

	public static long getTotalDelivered() {
		long total = 0;
		for (final ReliableBroadcastCounter msgCounter : msgCounters)
			total += msgCounter.getDeliveredMessages();
		return total;
	}

	public static long getTotalFailed() {
		long total = 0;
		for (final ReliableBroadcastCounter msgCounter : msgCounters)
			total += msgCounter.getFailedMessages();
		return total;
	}
	
	public static long getTotalRebroadcasted() {
		long total = 0;
		for (final ReliableBroadcastCounter msgCounter : msgCounters)
			total += msgCounter.getRebroadcastedMessages();
		return total;
	}

	public static double getDeliveredRatio() {
		final long totalBroadcasted = getTotalBroadcasted();
		if (totalBroadcasted == 0)
			return 0;
		return getTotalDelivered() / (float) totalBroadcasted;
	}

	public static double getAvgDeliveringTime() {
		if (msgCounters.isEmpty())
			return 0;

		double total = 0;
		synchronized (msgCounters) {
			for (final ReliableBroadcastCounter msgCounter : msgCounters)
				total += msgCounter.getAvgDeliveringTime();
		}
		return total / msgCounters.size();
	}

	public static void logStatistics() {
		logger.info("Total reliable broadcasted messages: " + getTotalBroadcasted());
		logger.info("Total delivered messages: " + getTotalDelivered());
		logger.info("Total failed messages: " + getTotalFailed());
		logger.info("Total rebroadcasted messages: " + getTotalRebroadcasted());
		logger.info("Delivered ratio: " + getDeliveredRatio());
		logger.info("Avg delivering time: " + getAvgDeliveringTime());
	}
}
