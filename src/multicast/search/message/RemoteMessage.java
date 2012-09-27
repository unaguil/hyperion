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

package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import peer.message.EnvelopeMessage;
import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

/**
 * This abstract class defines those messages which can be sent to a node that
 * is not a direct neighbor of the current node.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public abstract class RemoteMessage extends BroadcastMessage implements EnvelopeMessage {

	// the identification of the remote message (it is different from the one
	// used in near broadcasting)
	private final MessageID remoteMessageID;

	// the traversed distance (number of hops) of the current message
	private final byte distance;
	
	// the payload of the message
	private final BroadcastMessage payload;
		
	public RemoteMessage(final byte mType) {
		super(mType);
		remoteMessageID = new MessageID();
		payload = null;
		distance = 0;
	}

	/**
	 * Constructs a remote message.
	 * 
	 * @param source
	 *            the remote source of the message
	 */
	public RemoteMessage(final byte mType, final PeerID source, final BroadcastMessage payload, final Set<PeerID> expectedDestinations) {
		super(mType, source, expectedDestinations);
		this.remoteMessageID = new MessageID(source, MessageIDGenerator.getNewID());
		this.distance = 0;
		this.payload = payload;
	}
	
	protected RemoteMessage(final byte mType, final RemoteMessage remoteMessage) {
		super(mType, remoteMessage.getSource(), remoteMessage.getExpectedDestinations());
		this.remoteMessageID = remoteMessage.getRemoteMessageID();
		this.distance = (byte) remoteMessage.getDistance();
		this.payload = remoteMessage.getPayload();
	}

	/**
	 * Constructs a remote message with a given initial traversed distance.
	 * 
	 * @param source
	 *            the remote source of the message
	 * @param distance
	 *            the initial traversed distance by the message
	 */
	public RemoteMessage(final byte mType, final PeerID source, final BroadcastMessage payload, final Set<PeerID> expectedDestinations, final int distance) {
		super(mType, source, expectedDestinations);
		this.remoteMessageID = new MessageID(source, MessageIDGenerator.getNewID());
		this.distance = (byte)distance;
		this.payload = payload;
	}

	/**
	 * Constructs a remote message which uses another one as base
	 * 
	 * @param remoteMessage
	 *            the message used to construct this one
	 * @param sender
	 *            the new sender of the message
	 * @param respondingTo
	 *            the message this one responds to
	 */
	public RemoteMessage(final RemoteMessage remoteMessage, final PeerID sender, final Set<PeerID> expectedDestinations, final int newDistance) {
		super(remoteMessage.mType, sender, expectedDestinations);
		this.remoteMessageID = remoteMessage.getRemoteMessageID();
		this.distance = (byte)newDistance;
		this.payload = remoteMessage.getPayload();
	}

	/**
	 * Gets the remote source node of the message
	 * 
	 * @return the remote source node of the message
	 */
	public PeerID getSource() {
		return remoteMessageID.getPeer();
	}

	/**
	 * Gets the remote identifier of this message
	 * 
	 * @return the remote identifier of this message
	 */
	public MessageID getRemoteMessageID() {
		return remoteMessageID;
	}

	/**
	 * Gets the distance traversed by the message.
	 * 
	 * @return the distance traversed by the message
	 */
	public int getDistance() {
		return distance;
	}
	
	@Override
	public BroadcastMessage getPayload() {
		return payload;
	}
	
	@Override
	public boolean hasPayload() {
		return payload != null;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof RemoteMessage))
			return false;

		final RemoteMessage remoteMessage = (RemoteMessage) o;
		return this.remoteMessageID.equals(remoteMessage.remoteMessageID);
	}

	@Override
	public int hashCode() {
		return remoteMessageID.hashCode();
	}

	@Override
	public String toString() {
		return super.toString() + " R:" + getRemoteMessageID() + " D:" + getDistance() + "";
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		remoteMessageID.read(in);
		SerializationUtils.setFinalField(RemoteMessage.class, this, "distance", in.readByte());
		
		try {
			final boolean hasPayload = in.readBoolean();
			if (hasPayload) {
				final BroadcastMessage message = MessageTypes.readBroadcastMessage(in);
				SerializationUtils.setFinalField(RemoteMessage.class, this, "payload", message);
			}
		} catch (UnsupportedTypeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		remoteMessageID.write(out);
		out.writeByte(distance);
		
		out.writeBoolean(payload != null);
		if (payload != null)
			payload.write(out);
	}
	
	@Override
	public String getType() {
		if (payload != null)
			return super.getType() + "(" + payload.getType() + ")";
		
		return super.getType();
	}
	
	public static Set<PeerID> removePropagatedNeighbors(final RemoteMessage remoteMessage, final ReliableBroadcastPeer peer) {
		final Set<PeerID> neighbors = new HashSet<PeerID>(peer.getDetector().getCurrentNeighbors());
		neighbors.remove(remoteMessage.getSender());
		if (!remoteMessage.getSource().equals(peer.getPeerID()))
			neighbors.removeAll(remoteMessage.getExpectedDestinations());
		return neighbors;
	}
}
