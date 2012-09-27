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

package graphsearch.backward.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.peerid.PeerID;
import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;

public class MessagePart implements BSerializable {

	public static class Part implements BSerializable {

		private final byte total;
		private final byte pNumber;

		private final MessageID partitionID;
		
		public Part() {
			total = 0;
			pNumber = 0;
			partitionID = new MessageID();
		}

		public Part(final int total, final int pNumber, final MessageID partitionID) {
			this.total = (byte)total;
			this.pNumber = (byte)pNumber;
			this.partitionID = partitionID;
		}

		public int getTotal() {
			return total;
		}

		public int getPNumber() {
			return pNumber;
		}

		public MessageID getPartitionID() {
			return partitionID;
		}

		@Override
		public String toString() {
			return "[" + pNumber + "/" + total + ":" + partitionID + "]";
		}

		@Override
		public int hashCode() {
			int result = 17;
			result = 31 * result + total;
			result = 31 * result + pNumber;
			result = 31 * result + partitionID.hashCode();
			return result;
		}

		@Override
		public boolean equals(final Object o) {
			if (!(o instanceof Part))
				return false;

			final Part part = (Part) o;
			return this.total == part.total && this.pNumber == part.pNumber && this.partitionID.equals(part.partitionID);
		}

		@Override
		public void read(ObjectInputStream in) throws IOException {
			SerializationUtils.setFinalField(Part.class, this, "total", in.readByte());
			SerializationUtils.setFinalField(Part.class, this, "pNumber", in.readByte());
			partitionID.read(in);
		}

		@Override
		public void write(ObjectOutputStream out) throws IOException {
			out.writeByte(total);
			out.writeByte(pNumber);
			partitionID.write(out);
		}
	}

	// the string which represents the message partitions
	private final List<Part> parts = new ArrayList<Part>();

	private final MessageID rootID;
	
	public MessagePart() {
		rootID = new MessageID();
	}

	public MessagePart(final PeerID peerID) {
		this.rootID = new MessageID(peerID, MessageIDGenerator.getNewID());
	}

	private MessagePart(final List<Part> parts, final Part part, final MessageID rootID) {
		this.parts.addAll(parts);
		this.parts.add(part);
		this.rootID = rootID;
	}

	public int getSplitLevel() {
		return parts.size();
	}

	public Set<MessagePart> split(final int number, final PeerID peerID) {
		final Set<MessagePart> messageParts = new HashSet<MessagePart>();
		final MessageID messageID = new MessageID(peerID, MessageIDGenerator.getNewID());
		for (int i = 0; i < number; i++)
			messageParts.add(new MessagePart(this.parts, new Part(number, i, messageID), this.rootID));
		return messageParts;
	}

	public List<Part> getParts() {
		return parts;
	}

	public MessageID getRootID() {
		return rootID;
	}

	public static boolean areComplete(final Set<Part> checkedParts) {
		final Set<Integer> totalValues = new HashSet<Integer>();
		final Set<Integer> pNumberValues = new HashSet<Integer>();

		for (final Part part : checkedParts) {
			totalValues.add(Integer.valueOf(part.getTotal()));
			pNumberValues.add(Integer.valueOf(part.getPNumber()));
		}

		// All parts must have the same total number
		if (totalValues.size() != 1)
			return false;

		final int expectedTotal = totalValues.iterator().next().intValue();

		// check that pNumberValues contains all the numbers
		if (pNumberValues.size() != expectedTotal)
			return false;

		for (int i = 0; i < expectedTotal; i++)
			if (!pNumberValues.contains(Integer.valueOf(i)))
				return false;

		return true;
	}

	@Override
	public String toString() {
		return parts.toString();
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		rootID.read(in);
		
		final byte size = in.readByte();
		for (int i = 0; i < size; i++) {
			final Part part = new Part();
			part.read(in);
			parts.add(part);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		rootID.write(out);
		SerializationUtils.writeCollection(parts, out);
	}
}
