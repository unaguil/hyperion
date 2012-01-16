package dissemination.newProtocol.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import peer.message.BigEnvelopeMessage;
import peer.message.BroadcastMessage;
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
public class TableMessage extends BroadcastMessage implements BigEnvelopeMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final UpdateTable updateTable;

	// the message contained as payload
	private final List<PayloadMessage> payloadMessages = new ArrayList<PayloadMessage>();
	
	public TableMessage() {
		updateTable = null;
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
	public TableMessage(final PeerID sender, final Set<PeerID> expectedDestinations, final UpdateTable updateTable, final PayloadMessage payload) {
		super(sender, expectedDestinations);
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
	public List<PayloadMessage> getPayloadMessages() {
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
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		expectedDestinations.addAll(Arrays.asList((PeerID[])in.readObject()));
		
		UnserializationUtils.setFinalField(TableMessage.class, this, "updateTable", in.readObject());
		payloadMessages.addAll(Arrays.asList((PayloadMessage[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(expectedDestinations.toArray(new PeerID[0]));
		
		out.writeObject(updateTable);
		out.writeObject(payloadMessages.toArray(new PayloadMessage[0]));
	}

	@Override
	public void merge(final BroadcastMessage broadcastMessage) {
		super.merge(broadcastMessage);
		
		final TableMessage tableMessage = (TableMessage) broadcastMessage;
		updateTable.merge(tableMessage.updateTable);
		
		payloadMessages.addAll(tableMessage.payloadMessages);
	}
}
