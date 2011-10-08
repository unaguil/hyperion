package peer.messagecounter;

/**
 * This class is used for gathering statistical information about sent and
 * received messages.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public final class ReliableBroadcastCounter {

	private long broadcastedMessages = 0;
	private long deliveredMessages = 0;

	private long totalDeliveringTime = 0;

	public ReliableBroadcastCounter() {
		ReliableBroadcastTotalCounter.addCounter(this);
	}

	public void addBroadcastedMessage() {
		broadcastedMessages++;
	}

	public void addDeliveredMessage(final long deliveringTime) {
		deliveredMessages++;
		totalDeliveringTime += deliveringTime;
	}

	public long getBroadcastedMessages() {
		return broadcastedMessages;
	}

	public long getDeliveredMessages() {
		return deliveredMessages;
	}

	public long getFailedMessages() {
		return broadcastedMessages - deliveredMessages;
	}

	public double getAvgDeliveringTime() {
		if (deliveredMessages == 0)
			return 0;

		return totalDeliveringTime / (double) deliveredMessages;
	}

	@Override
	public String toString() {
		final StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("*********************************\n");
		strBuilder.append("Broadcasted msgs: " + getBroadcastedMessages() + "\n");
		strBuilder.append("Delivered msgs: " + getDeliveredMessages() + "\n");
		strBuilder.append("Failed msgs: " + getFailedMessages() + "\n");
		strBuilder.append("*********************************\n");

		return strBuilder.toString();
	}
}
