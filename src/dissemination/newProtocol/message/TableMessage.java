package dissemination.newProtocol.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import peer.message.BigEnvelopeMessage;
import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;
import taxonomy.Taxonomy;
import dissemination.newProtocol.ptable.UpdateTable;

/**
 * A broadcast message which contains a parameter table. It can also contain
 * other broadcast messages as payload.
 * 
 * There are two types of table messages: add and remove. Used to add new
 * information or to delete entries respectively.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class TableMessage extends BroadcastMessage implements BigEnvelopeMessage {

	private final UpdateTable updateTable;

	// the message contained as payload
	private final List<BroadcastMessage> payloadMessages = new ArrayList<BroadcastMessage>();
	
	public TableMessage() {
		super(MessageTypes.TABLE_MESSAGE);
		updateTable = new UpdateTable();
	}

	/**
	 * Constructor of the table message
	 * @param sender
	 *            the sender of the message
	 * @param updateTable
	 *            a table containing the updates for neighbors
	 * @param payload
	 *            the message payload
	 * @param op
	 *            the operation of this table message
	 */
	public TableMessage(final PeerID sender, final Set<PeerID> expectedDestinations, final UpdateTable updateTable, final BroadcastMessage payload) {
		super(MessageTypes.TABLE_MESSAGE, sender, expectedDestinations);
		this.updateTable = updateTable;
		if (payload != null)
			this.payloadMessages.add(payload);
	}

	/**
	 * Gets the update table contained in this message
	 * 
	 * @return the update table contained in this message
	 */
	public UpdateTable getUpdateTable() {
		return updateTable;
	}

	@Override
	public List<BroadcastMessage> getPayloadMessages() {
		return payloadMessages;
	}

	@Override
	public boolean hasPayload() {
		return !payloadMessages.isEmpty();
	}

	@Override
	public String toString() {
		return super.toString() + " " + getType() + " " + updateTable.toString();
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		updateTable.read(in);
		
		try {
			final byte nMessages = in.readByte();
			for (int i = 0; i < nMessages; i++) {
				final BroadcastMessage message = MessageTypes.readBroadcastMessage(in);
				payloadMessages.add(message);
			}
		} catch (UnsupportedTypeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		updateTable.write(out);
		SerializationUtils.writeCollection(payloadMessages, out);
	}

	public void merge(final BroadcastMessage broadcastMessage, final Taxonomy taxonomy) {
		addExpectedDestinations(broadcastMessage.getExpectedDestinations());
		
		final TableMessage tableMessage = (TableMessage) broadcastMessage;
		updateTable.merge(tableMessage.updateTable, taxonomy);
		payloadMessages.addAll(tableMessage.payloadMessages);		
	}
}
