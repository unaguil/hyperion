package dissemination;

import java.util.List;
import java.util.Map;
import java.util.Set;

import detection.NeighborEventsListener;

import peer.message.BroadcastMessage;
import peer.peerid.PeerID;
import taxonomy.parameter.Parameter;

/**
 * This interface defines those method used for table changes notification
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public interface TableChangedListener extends NeighborEventsListener {

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
	 * @param addedParameters TODO
	 * @param changedParamaters
	 *            those parameters whose distance has changed (but not
	 *            completely removed).
	 * @return the payload will be included in the table message by the
	 *         dissemination layer
	 */
	public BroadcastMessage parametersChanged(PeerID neighbor, Set<Parameter> newParameters, Set<Parameter> removedParameters, 
											Set<Parameter> removedLocalParameters, Map<Parameter, DistanceChange> changedParameters,
											Set<Parameter> addedParameters, List<BroadcastMessage> payloadMessages);
}
