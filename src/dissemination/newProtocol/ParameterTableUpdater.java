package dissemination.newProtocol;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.CommunicationLayer;
import peer.Peer;
import peer.RegisterCommunicationLayerException;
import peer.message.BroadcastMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import peer.peerid.PeerIDSet;
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
	private final NeighborEventsListener neighborListener;
	private final TableChangedListener tableChangedListener;

	// the reference to the communication peer
	private final Peer peer;

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
	public ParameterTableUpdater(final Peer peer, final TableChangedListener tableChangedListener, final NeighborEventsListener neighborListener) {
		this.peer = peer;
		this.tableChangedListener = tableChangedListener;
		this.neighborListener = neighborListener;

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
	public CommitedParameters commit() {
		final UpdateTable finalUpdateTable = new UpdateTable();
		
		final Set<Parameter> addedLocalParameters = new HashSet<Parameter>();
		final Set<Parameter> removedLocalParameters = new HashSet<Parameter>();
		
		final Map<Parameter, DistanceChange> changedParameters = new HashMap<Parameter, DistanceChange>();
		
		synchronized (mutex) {			
			for (final Parameter parameter : pTable.getParameters())
				changedParameters.put(parameter, new DistanceChange(pTable.getEstimatedDistance(parameter), 0));
			
			if (!addLocalParameters.isEmpty()) {
				final UpdateTable updateTable = pTable.addLocalParameters(addLocalParameters);
				finalUpdateTable.merge(updateTable);
			}
	
			if (!removeLocalParameters.isEmpty()) {
				final UpdateTable updateTable = pTable.removeLocalParameters(removeLocalParameters);
				finalUpdateTable.merge(updateTable);
			}
	
			// obtain distance changes
			for (final Parameter parameter : pTable.getParameters()) {
				if (changedParameters.containsKey(parameter)) {
					final DistanceChange dChange = changedParameters.get(parameter);
					changedParameters.put(parameter, new DistanceChange(dChange.getPreviousValue(), pTable.getEstimatedDistance(parameter)));
				} else
					changedParameters.put(parameter, new DistanceChange(0, pTable.getEstimatedDistance(parameter)));
			}
			
			addedLocalParameters.addAll(addLocalParameters);
			removedLocalParameters.addAll(removeLocalParameters);
			
			// Parameters have been processed. Remove them
			addLocalParameters.clear();
			removeLocalParameters.clear();
		}
		
		logger.trace("Peer " + peer.getPeerID() + " added parameters " + addedLocalParameters + " to local table");
		logger.trace("Peer " + peer.getPeerID() + " removed parameters " + removedLocalParameters + " from local table");

		// Notify added and removed parameters
		final PayloadMessage payload = notifyTableChangedListener(peer.getPeerID(), addedLocalParameters, removedLocalParameters, removedLocalParameters, changedParameters, null);

		// Send the tables using a table message only if neighbors exist
		final PeerIDSet currentNeighbors = peer.getDetector().getCurrentNeighbors();
		if (!currentNeighbors.isEmpty())
			sendUpdateTableMessage(finalUpdateTable, currentNeighbors, payload);

		final CommitedParameters commitedParameters = new CommitedParameters(addedLocalParameters, removedLocalParameters);

		return commitedParameters;
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
	public void appearedNeighbors(final PeerIDSet neighbors) {
		logger.trace("Peer " + peer.getPeerID() + " detected appearance of neighbors: " + neighbors);
		
		boolean sendUpdate = false;
		final UpdateTable newNeighborTable;
		
		synchronized (mutex) {
			// Enqueue action only if table is not empty
			newNeighborTable = pTable.getNewNeighborTable();
			if (!newNeighborTable.isEmpty())
				sendUpdate = true;
		}
		
		if (sendUpdate)
			sendUpdateTableMessage(newNeighborTable, neighbors, null);

		neighborListener.appearedNeighbors(neighbors);
	}

	@Override
	public void dissapearedNeighbors(final PeerIDSet neighbors) {
		logger.trace("Peer " + peer.getPeerID() + " detected dissapearance of neighbors: " + neighbors);
		// Remove entries in pTable which were obtained from disappeared
		// neighbors and send message to neighbors
		final UpdateTable finalUpdateTable = new UpdateTable();
		
		synchronized (mutex) {
			for (final PeerID neighbor : neighbors) {
				for (final Parameter p : pTable.getParameters(neighbor)) {
					final UpdateTable removalTable = new UpdateTable();
					removalTable.setDelete(p, neighbor);
					final UpdateTable updateTable = pTable.updateTable(removalTable, neighbor);
					finalUpdateTable.merge(updateTable);
				}
			}
		}

		logger.trace("Peer " + peer.getPeerID() + " table after removal: " + pTable);

		logger.trace("Peer " + peer.getPeerID() + " update table: " + finalUpdateTable);

		if (!finalUpdateTable.isEmpty())
			sendUpdateTableMessage(finalUpdateTable, peer.getDetector().getCurrentNeighbors(), null);

		neighborListener.dissapearedNeighbors(neighbors);
	}

	@Override
	public void init() {
		try {
			final String maxDistanceStr = Configuration.getInstance().getProperty("dissemination.maxDistance");
			MAX_DISTANCE = Integer.parseInt(maxDistanceStr);
			logger.info("Peer " + peer.getPeerID() + " set MAX_DISTANCE to " + MAX_DISTANCE);
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
		pTable = new ParameterTable(MAX_DISTANCE, peer.getPeerID(), getTaxonomy());
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
			logger.trace("Peer " + peer.getPeerID() + " updating table " + pTable + " with update table " + tableMessage.getUpdateTable() + " from neighbor " + tableMessage.getSender());
			
			final Set<Parameter> addedParameters = new HashSet<Parameter>();
			final Set<Parameter> removedParameters = new HashSet<Parameter>();
			
			final Map<Parameter, DistanceChange> changedParameters = new HashMap<Parameter, DistanceChange>();
			
			final UpdateTable updateTable;

			synchronized (mutex) {
				// Get parameters before update
				final Set<Parameter> parametersBeforeUpdate = pTable.getParameters();
	
				for (final Parameter parameter : pTable.getParameters())
					changedParameters.put(parameter, new DistanceChange(pTable.getEstimatedDistance(parameter), 0));
	
				updateTable = pTable.updateTable(tableMessage.getUpdateTable(), tableMessage.getSender());
	
				// Get parameters after update
				final Set<Parameter> parametersAfterUpdate = pTable.getParameters();
	
				// obtain distance changes
				for (final Parameter parameter : pTable.getParameters()) {
					final DistanceChange dChange = changedParameters.get(parameter);
					if (changedParameters.containsKey(parameter)) {
						if (dChange.getPreviousValue() != pTable.getEstimatedDistance(parameter))
							changedParameters.put(parameter, new DistanceChange(dChange.getPreviousValue(), pTable.getEstimatedDistance(parameter)));
						else
							changedParameters.remove(parameter);
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
	
					addedParameters.addAll(tempAddedParameters);
					removedParameters.addAll(tempRemovedParameters);
				}
			}
			
			String pTableStatus;
			synchronized (mutex) {
				pTableStatus = pTable.toString();
			}
			logger.trace("Peer " + peer.getPeerID() + " local table after update " + pTableStatus);

			// Notify table changes to listeners
			final PayloadMessage payload = notifyTableChangedListener(tableMessage.getSender(), addedParameters, removedParameters, new HashSet<Parameter>(), changedParameters, tableMessage.getPayload());

			logger.trace("Peer " + peer.getPeerID() + " addedParameters: " + addedParameters + " removedParameters: " + removedParameters);
			logger.trace("Peer " + peer.getPeerID() + " adding payload " + payload + " to table message");

			sendUpdateTableMessage(updateTable, peer.getDetector().getCurrentNeighbors(), payload);
		}
	}

	// Sends a message which contains a table to be added or removed
	private void sendUpdateTableMessage(final UpdateTable updateTable, final PeerIDSet destNeighbors, final PayloadMessage payload) {
		// Only send message if tables are non empty
		if (!updateTable.isEmpty()) {
			TableMessage tableMessage;
			
			tableMessage = new TableMessage(peer.getPeerID(), destNeighbors.getPeerSet(), updateTable, payload);

			logger.trace("Peer " + peer.getPeerID() + " sending update table message " + tableMessage);
			
			final String payloadType = (payload == null)?"null":payload.getType();
			logger.debug("Peer " + peer.getPeerID() + " sending update table message with payload type of " + payloadType);
			// Perform the broadcasting of table message
			peer.enqueueBroadcast(tableMessage, this);
		} else
			logger.trace("Peer " + peer.getPeerID() + " update table is empty and is not sent");
	}

	// Notifies the table changed listener
	private PayloadMessage notifyTableChangedListener(final PeerID neighbor, final Set<Parameter> addedParameters, final Set<Parameter> removedParameters, final Set<Parameter> removedLocalParameters, final Map<Parameter, DistanceChange> changedParameters, final PayloadMessage payload) {
		if (tableChangedListener != null)
			return tableChangedListener.parametersChanged(neighbor, addedParameters, removedParameters, removedLocalParameters, changedParameters, payload);

		return null;
	}

	@Override
	public BroadcastMessage isDuplicatedMessage(List<BroadcastMessage> waitingMessages, BroadcastMessage sendingMessage) {
		for (final BroadcastMessage waitingMessage : waitingMessages) {
			if (waitingMessage instanceof TableMessage) {
				TableMessage waitingTableMessage = (TableMessage) waitingMessage;
				TableMessage sendingTableMessage = (TableMessage) sendingMessage;
				if (sendingTableMessage.getUpdateTable().equals(waitingTableMessage.getUpdateTable()))
					return waitingTableMessage;
			}
		}
		return null;
	}

	@Override
	public int getMaxDistance() {
		return MAX_DISTANCE;
	}
}
