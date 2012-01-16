package graphsearch;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import peer.message.MessageID;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class SearchID implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final MessageID messageID;
	
	public SearchID() {
		messageID = null;
	}

	public SearchID(final PeerID startPeer) {
		messageID = new MessageID(startPeer);
	}

	@Override
	public int hashCode() {
		return messageID.hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof SearchID))
			return false;

		final SearchID searchID = (SearchID) o;
		return this.messageID.equals(searchID.messageID);
	}

	@Override
	public String toString() {
		return messageID.toString();
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(SearchID.class, this, "messageID", in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(messageID);		
	}
}
