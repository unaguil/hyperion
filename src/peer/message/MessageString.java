package peer.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;

import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

/**
 * Example implementation of a message.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class MessageString extends BroadcastMessage {

	private final String content;
	
	public MessageString() {
		super(MessageTypes.MESSAGE_STRING);
		content = new String();
	}

	public MessageString(final PeerID sender, final Set<PeerID> expectedDestinations, final String content) {
		super(MessageTypes.MESSAGE_STRING, sender, expectedDestinations);
		this.content = content;
	}

	@Override
	public String toString() {
		return content;
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);		
		SerializationUtils.setFinalField(MessageString.class, this, "content", in.readUTF());
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);		
		out.writeUTF(content);
	}
}
