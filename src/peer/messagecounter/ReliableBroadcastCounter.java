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
	
	private long rebroadcasted = 0;

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
	
	public void addRebroadcastedMessage() {
		rebroadcasted++;
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
	
	public long getRebroadcastedMessages() {
		return rebroadcasted;
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
		strBuilder.append("Rebroadcasted msgs: " + getRebroadcastedMessages() + "\n");
		strBuilder.append("*********************************\n");

		return strBuilder.toString();
	}
}
