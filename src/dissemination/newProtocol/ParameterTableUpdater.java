/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package dissemination.newProtocol;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.CommunicationLayer;
import peer.RegisterCommunicationLayerException;
import peer.ReliableBroadcastPeer;
import peer.message.BroadcastMessage;
import peer.peerid.PeerID;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.UnmodifiableTaxonomy;
import taxonomy.parameter.Parameter;
import util.logger.Logger;
import config.Configuration;
import detection.NeighborEventsListener;
import dissemination.DistanceChange;
import dissemination.ParameterDisseminator;
import dissemination.TableChangedListener;
import dissemination.newProtocol.message.TableMessage;
import dissemination.newProtocol.ptable.ParameterTable;
import dissemination.newProtocol.ptable.ParameterTable.UpdateResult;
import dissemination.newProtocol.ptable.UpdateTable;

/**
 * This class implements the dissemination functionality using a reactive
 * parameter table update. This implementation reacts to the changes in
 * neighbors to propagate and update parameter table information
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class ParameterTableUpdater implements CommunicationLayer, NeighborEventsListener, ParameterDisseminator {

	// the maximum distance parameters are disseminated
	private int MAX_DISTANCE = 5; // default value

	// listeners for upper layers notifications
	private final TableChangedListener tableChangedListener;

	// the reference to the communication peer
	private final ReliableBroadcastPeer peer;

	// Used during local parameters addition and removal
	private final Object mutex = new Object();
	private final Set<Parameter> addLocalParameters = new HashSet<Parameter>();
	private final Set<Parameter> removeLocalParameters = new HashSet<Parameter>();

	// the taxonomy used by the dissemination layer
	private final BasicTaxonomy taxonomy = new BasicTaxonomy();
	private final Taxonomy unmodifiableTaxonomy = new UnmodifiableTaxonomy(taxonomy);

	// the local parameter table of this node
	private ParameterTable pTable;

	private final Logger logger = Logger.getLogger(ParameterTableUpdater.class);

	/**
	 * Constructor of the reactive parameter table updater.
	 * 
	 * @param peer
	 *            the peer which holds the table
	 * @param tableChangedListener
	 *            listener for local table changes
	 * @param neighborListener
	 *            listener for neighbor changes. It is used to propagate
	 *            detector information to upper layers.
	 */
	public ParameterTableUpdater(final ReliableBroadcastPeer peer, final TableChangedListener tableChangedListener) {
		this.peer = peer;
		this.tableChangedListener = tableChangedListener;

		// Register messages
		final Set<Class<? extends BroadcastMessage>> messageClasses = new HashSet<Class<? extends BroadcastMessage>>();
		messageClasses.add(TableMessage.class);
		try {
			// Table messages must not be automatically responded.
			peer.addCommunicationLayer(this, messageClasses);
		} catch (final RegisterCommunicationLayerException e) {
			logger.error("Peer " + peer.getPeerID() + " had problem registering communication layer: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * dissemination.newProtocol.ParameterDisseminator#addLocalParameter(taxonomy
	 * .parameter.Parameter)
	 */
	@Override
	public boolean addLocalParameter(final Parameter parameter) {
		synchronized (mutex) {
			// Check if the message was scheduled for removal and is not committed
			if (removeLocalParameters.contains(parameter)) {
				// Cancel non-committed removal
				removeLocalParameters.remove(parameter);
				return true;
			}
	
			// Check local table for parameter existence
			if (!pTable.isLocalParameter(parameter)) {
				// Add parameter for further addition
				addLocalParameters.add(parameter);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean removeLocalParameter(final Parameter parameter) {
		synchronized (mutex) {
			// Check if parameter was scheduled for addition and is not committed
			if (addLocalParameters.contains(parameter)) {
				addLocalParameters.remove(parameter);
				return true;
			}
	
			// Check local table for parameter existence
			if (pTable.isLocalParameter(parameter)) {
				removeLocalParameters.add(parameter);
				return true;
			}
		}

		return false;
	}

	static public class CommitedParameters {

		public final Set<Parameter> addedParameters = new HashSet<Parameter>();
		public final Set<Parameter> removedParameters = new HashSet<Parameter>();

		public CommitedParameters(final Set<Parameter> addedParameters, final Set<Parameter> removedParameters) {
			addedParameters.addAll(addedParameters);
			removedParameters.addAll(removedParameters);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dissemination.newProtocol.ParameterDisseminator#commit()
	 */
	@Override
	public void commit() {
		final UpdateTable finalUpdateTable = new UpdateTable();
				
		final Set<Parameter> addedParameters = new HashSet<Parameter>();
		final Set<Parameter> newParameters = new HashSet<Parameter>();
		final Set<Parameter> removedParameters = new HashSet<Parameter>();
		
		final Set<Parameter> parametersBeforeUpdate = pTable.getParameters();
		
		final Map<Parameter, DistanceChange> changedParameters = new HashMap<Parameter, DistanceChange>();
		
		synchronized (mutex) {			
			for (final Parameter parameter : pTable.getParameters())
				changedParameters.put(parameter, new DistanceChange(pTable.getEstimatedDistance(parameter), 0));
				
			if (!addLocalParameters.isEmpty()) {
				final UpdateTable updateTable = pTable.addLocalParameters(addLocalParameters);
				finalUpdateTable.add(updateTable);
			} 
	
			if (!removeLocalParameters.isEmpty()) {
				final UpdateTable updateTable = pTable.removeLocalParameters(removeLocalParameters);
				finalUpdateTable.add(updateTable);
			}
			
			addedParameters.addAll(addLocalParameters);
			
			// Parameters have been processed. Remove them
			addLocalParameters.clear();
			removeLocalParameters.clear();
			
			checkParameters(newParameters, removedParameters, changedParameters, parametersBeforeUpdate);
		}
		
		logger.trace("Peer " + peer.getPeerID() + " added new parameters " + newParameters + " to local table");
		logger.trace("Peer " + peer.getPeerID() + " removed parameters " + removedParameters + " from local table");

		// Notify added and removed parameters
		final BroadcastMessage payload = notifyTableChangedListener(peer.getPeerID(), addedParameters, removedParameters, removedParameters, changedParameters, addedParameters, Collections.<BroadcastMessage> emptyList());

		// Send the tables using a table message only if neighbors exist
		final Set<PeerID> currentNeighbors = peer.getDetector().getCurrentNeighbors();
		if (!currentNeighbors.isEmpty())
			sendUpdateTableMessage(finalUpdateTable, currentNeighbors, payload);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dissemination.newProtocol.ParameterDisseminator#getTaxonomy()
	 */
	@Override
	public Taxonomy getTaxonomy() {
		return unmodifiableTaxonomy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * dissemination.newProtocol.ParameterDisseminator#isLocalParameter(taxonomy
	 * .parameter.Parameter)
	 */
	@Override
	public boolean isLocalParameter(final Parameter parameter) {
		synchronized (mutex) {
			return pTable.isLocalParameter(parameter);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * dissemination.newProtocol.ParameterDisseminator#subsumesLocalParameter
	 * (taxonomy.parameter.Parameter)
	 */
	@Override
	public Set<Parameter> subsumesLocalParameter(final Parameter parameter) {
		synchronized (mutex) {
			return pTable.subsumesLocalParameter(parameter);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * dissemination.newProtocol.ParameterDisseminator#getEstimatedDistance(
	 * taxonomy.parameter.Parameter)
	 */
	@Override
	public int getEstimatedDistance(final Parameter parameter) {
		synchronized (mutex) {
			return pTable.getEstimatedDistance(parameter);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * dissemination.newProtocol.ParameterDisseminator#getDistance(taxonomy.
	 * parameter.Parameter, peer.PeerID)
	 */
	//TODO This methods should be renamed. It is not the distance but the dissemination value
	@Override
	public int getDistance(final Parameter p, final PeerID neighbor) {
		synchronized (mutex) {
			return pTable.getDistance(p, neighbor);
		}
	}
	
	@Override
	public int getDistanceTo(final Parameter p) {
		return MAX_DISTANCE - getEstimatedDistance(p);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dissemination.newProtocol.ParameterDisseminator#getParameters()
	 */
	@Override
	public Set<Parameter> getParameters() {
		synchronized (mutex) {
			return pTable.getParameters();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dissemination.newProtocol.ParameterDisseminator#getLocalParameters()
	 */
	@Override
	public Set<Parameter> getLocalParameters() {
		synchronized (mutex) {
			return pTable.getLocalParameters();
		}
	}

	@Override
	public void neighborsChanged(final Set<PeerID> newNeighbors, Set<PeerID> lostNeighbors) {
		if (!newNeighbors.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " detected appearance of neighbors: " + newNeighbors);
			
			boolean sendUpdate = false;
			final UpdateTable newNeighborTable;
			
			synchronized (mutex) {
				// Enqueue action only if table is not empty
				newNeighborTable = pTable.getNewNeighborTable();
				if (!newNeighborTable.isEmpty())
					sendUpdate = true;
			}
			
			if (sendUpdate)
				sendUpdateTableMessage(newNeighborTable, newNeighbors, null);
	
		}
		
		if (!lostNeighbors.isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " detected dissapearance of neighbors: " + lostNeighbors);
			// Remove entries in pTable which were obtained from disappeared
			// neighbors and send message to neighbors
			final UpdateTable finalUpdateTable = new UpdateTable();
			
			synchronized (mutex) {
				for (final PeerID neighbor : lostNeighbors) {
					for (final Parameter p : pTable.getParameters(neighbor)) {
						final UpdateTable removalTable = new UpdateTable();
						removalTable.setDelete(p, neighbor);
						final UpdateResult updateResult = pTable.updateTable(removalTable, neighbor);
						finalUpdateTable.add(updateResult.getUpdateTable());
					}
				}
			}

			logger.trace("Peer " + peer.getPeerID() + " table after removal: " + pTable);

			logger.trace("Peer " + peer.getPeerID() + " update table: " + finalUpdateTable);

			if (!finalUpdateTable.isEmpty())
				sendUpdateTableMessage(finalUpdateTable, peer.getDetector().getCurrentNeighbors(), null);
		}
		
		tableChangedListener.neighborsChanged(newNeighbors, lostNeighbors);
	}
	
	@Override
	public void init() {
		try {
			final String maxDistanceStr = Configuration.getInstance().getProperty("dissemination.maxDistance");
			final int maxDistance = Integer.parseInt(maxDistanceStr);
			setDisseminationTTL(maxDistance);
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		try {
			final String taxonomyFile = Configuration.getInstance().getProperty("dissemination.taxonomyFile");

			if (taxonomyFile != null) {
				// Load the taxonomy from the configuration file
				final FileInputStream fis = new FileInputStream(taxonomyFile);
				taxonomy.readFromXML(fis);
				fis.close();
				logger.info("Peer " + peer.getPeerID() + " taxonomy file " + taxonomyFile + " loaded");
			}
		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " had problem loading configuration: " + e.getMessage());
		}

		// Sets neighbor listener for this layer using detector layer
		peer.getDetector().addNeighborListener(this);

		// Create the parameter table using loaded configuration
		pTable = new ParameterTable(this, peer.getPeerID(), getTaxonomy());
	}

	@Override
	public void messageReceived(final BroadcastMessage message, final long receptionTime) {
		// Check if message is a table message
		if (message instanceof TableMessage) {
			final TableMessage tableMessage = (TableMessage) message;

			logger.trace("Peer " + peer.getPeerID() + " received table message " + message);
			processTableMessage(tableMessage);
		}
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		synchronized (mutex) {
			pTable.saveToXML(os);
		}
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
		throw new UnsupportedOperationException("ParameterTableUpdater.readFromXML method not implemented!");
	}

	@Override
	public void stop() {
	}

	private void processTableMessage(final TableMessage tableMessage) {
		if (!tableMessage.getUpdateTable().isEmpty()) {
			logger.trace("Peer " + peer.getPeerID() + " updating table " + pTable + " with update table " + tableMessage.getUpdateTable().pretty(taxonomy) + " from neighbor " + tableMessage.getSender());
			
			final Set<Parameter> newParameters = new HashSet<Parameter>();
			final Set<Parameter> removedParameters = new HashSet<Parameter>();
			
			final Map<Parameter, DistanceChange> changedParameters = new HashMap<Parameter, DistanceChange>();
			
			final UpdateResult updateResult;

			synchronized (mutex) {
				// Get parameters before update
				final Set<Parameter> parametersBeforeUpdate = pTable.getParameters();
	
				for (final Parameter parameter : pTable.getParameters())
					changedParameters.put(parameter, new DistanceChange(pTable.getEstimatedDistance(parameter), 0));
	
				updateResult = pTable.updateTable(tableMessage.getUpdateTable(), tableMessage.getSender());
	
				checkParameters(newParameters, removedParameters, changedParameters, parametersBeforeUpdate);
			}
			
			String pTableStatus;
			synchronized (mutex) {
				pTableStatus = pTable.toString();
			}
			logger.trace("Peer " + peer.getPeerID() + " local table after update " + pTableStatus);

			// Notify table changes to listeners
			final BroadcastMessage payload = notifyTableChangedListener(tableMessage.getSender(), newParameters, removedParameters, new HashSet<Parameter>(), changedParameters, tableMessage.getUpdateTable().getAdditions(), tableMessage.getPayloadMessages());

			logger.trace("Peer " + peer.getPeerID() + " addedParameters: " + newParameters + " removedParameters: " + removedParameters);
			logger.trace("Peer " + peer.getPeerID() + " adding payload " + payload + " to table message");

			sendUpdateTableMessage(updateResult.getUpdateTable(), BroadcastMessage.removePropagatedNeighbors(tableMessage, peer), payload);
		}
	}

	private void checkParameters(final Set<Parameter> newParameters, final Set<Parameter> removedParameters, final Map<Parameter, DistanceChange> changedParameters, final Set<Parameter> parametersBeforeUpdate) {
		// Get parameters after update
		final Set<Parameter> parametersAfterUpdate = pTable.getParameters();

		// obtain distance changes
		for (final Parameter parameter : parametersAfterUpdate) {
			if (changedParameters.containsKey(parameter)) {
				final DistanceChange dChange = changedParameters.get(parameter);
				changedParameters.put(parameter, new DistanceChange(dChange.getPreviousValue(), pTable.getEstimatedDistance(parameter)));
			} else
				changedParameters.put(parameter, new DistanceChange(0, pTable.getEstimatedDistance(parameter)));
		}

		// If parameters were added or removed
		if (!parametersAfterUpdate.equals(parametersBeforeUpdate)) {
			final Set<Parameter> tempAddedParameters = new HashSet<Parameter>(parametersAfterUpdate);
			final Set<Parameter> tempRemovedParameters = new HashSet<Parameter>(parametersBeforeUpdate);

			// Obtain added parameters
			tempAddedParameters.removeAll(parametersBeforeUpdate);

			// Obtain removed parameters
			final Set<Parameter> differenceParameters = new HashSet<Parameter>(parametersAfterUpdate);
			differenceParameters.removeAll(tempAddedParameters);
			tempRemovedParameters.removeAll(differenceParameters);

			newParameters.addAll(tempAddedParameters);
			removedParameters.addAll(tempRemovedParameters);
		}
	}

	// Sends a message which contains a table to be added or removed
	private void sendUpdateTableMessage(final UpdateTable updateTable, final Set<PeerID> destNeighbors, final BroadcastMessage payload) {
		// Only send message if tables are non empty
		if (!updateTable.isEmpty()) {
			TableMessage tableMessage;
			
			tableMessage = new TableMessage(peer.getPeerID(), destNeighbors, updateTable, payload);

			logger.trace("Peer " + peer.getPeerID() + " sending update table message " + tableMessage);
			
			final String payloadType = (payload == null)?"null":payload.getType();
			logger.debug("Peer " + peer.getPeerID() + " sending update table message with payload type of " + payloadType);
			// Perform the broadcasting of table message
			peer.enqueueBroadcast(tableMessage, this);
		} else
			logger.trace("Peer " + peer.getPeerID() + " update table is empty and is not sent");
	}

	// Notifies the table changed listener
	private BroadcastMessage notifyTableChangedListener(final PeerID neighbor, final Set<Parameter> newParameters, 
													  final Set<Parameter> removedParameters, final Set<Parameter> removedLocalParameters,
													  final Map<Parameter, DistanceChange> changedParameters, 
													  final Set<Parameter> tableAdditions, final List<BroadcastMessage> payloadMessages) {
		if (tableChangedListener != null)
			return tableChangedListener.parametersChanged(neighbor, newParameters, removedParameters, removedLocalParameters, changedParameters, tableAdditions, payloadMessages);

		return null;
	}

	@Override
	public boolean merge(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
		for (final BroadcastMessage waitingMessage : waitingMessages) {
			if (waitingMessage instanceof TableMessage) { 
				TableMessage waitingTableMessage = (TableMessage) waitingMessage;
				TableMessage sendingTableMessage = (TableMessage) sendingMessage;
				waitingTableMessage.merge(sendingTableMessage, getTaxonomy());
				return true;
			}
		}
		return false;
	}

	@Override
	public int getMaxDistance() {
		return MAX_DISTANCE;
	}

	@Override
	public void setDisseminationTTL(final int disseminationTTL) {
		MAX_DISTANCE = disseminationTTL;
		logger.info("Peer " + peer.getPeerID() + " set MAX_DISTANCE to " + MAX_DISTANCE);
	}
}
