package graphsearch.backward.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class MessagePart implements Externalizable {

	public static class Part implements Externalizable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private final int total;
		private final int pNumber;

		private final MessageID partitionID;
		
		public Part() {
			total = 0;
			pNumber = 0;
			partitionID = null;
		}

		public Part(final int total, final int pNumber, final MessageID partitionID) {
			this.total = total;
			this.pNumber = pNumber;
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
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			UnserializationUtils.setFinalField(Part.class, this, "total", in.readInt());
			UnserializationUtils.setFinalField(Part.class, this, "pNumber", in.readInt());
			UnserializationUtils.setFinalField(Part.class, this, "partitionID", in.readObject());
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeInt(total);
			out.writeInt(pNumber);
			out.writeObject(partitionID);
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the string which represents the message partitions
	private final List<Part> parts = new ArrayList<Part>();

	private final MessageID rootID;
	
	public MessagePart() {
		rootID = null;
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
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(MessagePart.class, this, "rootID", in.readObject());
		parts.addAll(Arrays.asList((Part[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(rootID);
		out.writeObject(parts.toArray(new Part[0]));
	}
}
