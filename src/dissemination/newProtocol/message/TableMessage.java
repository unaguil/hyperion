package dissemination.newProtocol.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import peer.message.BroadcastMessage;
import peer.message.EnvelopeMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;
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
public class TableMessage extends BroadcastMessage implements EnvelopeMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final UpdateTable updateTable;

	// the message contained as payload
	private final PayloadMessage payload;
	
	public TableMessage() {
		updateTable = null;
		payload = null;
	}

	/**
	 * Constructor of the table message
	 * 
	 * @param updateTable
	 *            a table containing the updates for neighbors
	 * @param sender
	 *            the sender of the message
	 * @param op
	 *            the operation of this table message
	 * @param payload
	 *            the message payload
	 */
	public TableMessage(final PeerID sender, final List<PeerID> expectedDestinations, final UpdateTable updateTable, final PayloadMessage payload) {
		super(sender, expectedDestinations);
		this.updateTable = updateTable;
		this.payload = payload;
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
	public PayloadMessage getPayload() {
		return payload;
	}

	@Override
	public boolean hasPayload() {
		return payload != null;
	}

	@Override
	public String toString() {
		return super.toString() + " " + getType() + " " + updateTable.toString();
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.setFinalField(TableMessage.class, this, "updateTable", in.readObject());
		UnserializationUtils.setFinalField(TableMessage.class, this, "payload", in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(updateTable);
		out.writeObject(payload);
	}
}
