package dissemination;

import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import taxonomy.parameter.Parameter;

/**
 * This interface defines those method used for table changes notification
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface TableChangedListener {

	/**
	 * This method notifies changes in the parameters of the local table
	 * 
	 * @param neighbor
	 *            the neighbor where the changes came from
	 * @param newParameters
	 *            the added parameters
	 * @param removedParameters
	 *            the removed parameters
	 * @param removedLocalParameters
	 *            the set of removed parameters which where local
	 * @param changedParamaters
	 *            those parameters whose distance has changed (but not
	 *            completely removed).
	 * @return the payload will be included in the table message by the
	 *         dissemination layer
	 */
	public PayloadMessage parametersChanged(PeerID neighbor, Set<Parameter> newParameters, Set<Parameter> removedParameters, 
											Set<Parameter> removedLocalParameters, Map<Parameter, DistanceChange> changedParameters,
											List<PayloadMessage> payloadMessages);
}
