package peer.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

/**
 * Example implementation of a message.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class MessageString extends BroadcastMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String content;
	
	public MessageString() {
		content = null;
	}

	public MessageString(final PeerID sender, final List<PeerID> expectedDestinations, final String content) {
		super(sender, expectedDestinations);
		this.content = content;
	}

	@Override
	public String toString() {
		return content;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.setFinalField(MessageString.class, this, "content", in.readUTF());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeUTF(content);
	}
}
