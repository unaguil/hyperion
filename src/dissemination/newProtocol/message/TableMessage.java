package dissemination.newProtocol.message;

import peer.message.BroadcastMessage;
import peer.message.EnvelopeMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
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
	public TableMessage(final UpdateTable updateTable, final PeerID sender, final PayloadMessage payload) {
		super(sender);
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
}
