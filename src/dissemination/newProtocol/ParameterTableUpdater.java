package dissemination.newProtocol;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import peer.CommunicationLayer;
import peer.Peer;
import peer.PeerID;
import peer.PeerIDSet;
import peer.RegisterCommunicationLayerException;
import peer.message.BroadcastMessage;
import peer.message.PayloadMessage;
import taxonomy.BasicTaxonomy;
import taxonomy.Taxonomy;
import taxonomy.UnmodifiableTaxonomy;
import taxonomy.parameter.Parameter;
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

		// Sets neighbor listener for this layer using detector layer
		peer.getDetector().addNeighborListener(this);

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
	public synchronized boolean addLocalParameter(final Parameter parameter) {
		// Check if the message was scheduled for removal and is not commited
		if (removeLocalParameters.contains(parameter)) {
			// Cancel non-commited removal
			removeLocalParameters.remove(parameter);
			return true;
		}

		// Check local table for parameter existence
		if (!pTable.isLocalParameter(parameter)) {
			// Add parameter for further addition
			addLocalParameters.add(parameter);
			return true;
		}

		return false;
	}

	@Override
	public synchronized boolean removeLocalParameter(final Parameter parameter) {
		// Check if parameter was scheduled for addition and is not commited
		if (addLocalParameters.contains(parameter)) {
			addLocalParameters.remove(parameter);
			return true;
		}

		// Check local table for parameter existence
		if (pTable.isLocalParameter(parameter)) {
			removeLocalParameters.add(parameter);
			return true;
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
	public synchronized CommitedParameters commit() {
		final UpdateTable finalUpdateTable = new UpdateTable();

		final Map<Parameter, DistanceChange> changedParameters = new HashMap<Parameter, DistanceChange>();
		for (final Parameter parameter : pTable.getParameters())
			changedParameters.put(parameter, new DistanceChange(pTable.getEstimatedDistance(parameter), 0));

		if (!addLocalParameters.isEmpty()) {
			final UpdateTable updateTable = pTable.addLocalParameters(addLocalParameters);
			
				logger.trace("Peer " + peer.getPeerID() + " added parameters " + addLocalParameters + " to local table");
			finalUpdateTable.merge(updateTable);
		}

		if (!removeLocalParameters.isEmpty()) {
			final UpdateTable updateTable = pTable.removeLocalParameters(removeLocalParameters);
			
				logger.trace("Peer " + peer.getPeerID() + " removed parameters " + removeLocalParameters + " from local table");
			finalUpdateTable.merge(updateTable);
		}

		// obtain distance changes
		for (final Parameter parameter : pTable.getParameters())
			if (changedParameters.containsKey(parameter)) {
				final DistanceChange dChange = changedParameters.get(parameter);
				changedParameters.put(parameter, new DistanceChange(dChange.getPreviousValue(), pTable.getEstimatedDistance(parameter)));
			} else
				changedParameters.put(parameter, new DistanceChange(0, pTable.getEstimatedDistance(parameter)));

		// Notify added and removed parameters
		final PayloadMessage payload = notifyTableChangedListener(peer.getPeerID(), addLocalParameters, removeLocalParameters, removeLocalParameters, changedParameters, null);

		// Send the tables using a table message only if neighbors exist
		if (!peer.getDetector().getCurrentNeighbors().isEmpty())
			sendUpdateTableMessage(finalUpdateTable, payload);

		final CommitedParameters commitedParameters = new CommitedParameters(addLocalParameters, removeLocalParameters);

		// Parameters have been processed. Remove them
		addLocalParameters.clear();
		removeLocalParameters.clear();

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
	public synchronized boolean isLocalParameter(final Parameter parameter) {
		return pTable.isLocalParameter(parameter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * dissemination.newProtocol.ParameterDisseminator#subsumesLocalParameter
	 * (taxonomy.parameter.Parameter)
	 */
	@Override
	public synchronized Set<Parameter> subsumesLocalParameter(final Parameter parameter) {
		return pTable.subsumesLocalParameter(parameter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * dissemination.newProtocol.ParameterDisseminator#getEstimatedDistance(
	 * taxonomy.parameter.Parameter)
	 */
	@Override
	public synchronized int getEstimatedDistance(final Parameter parameter) {
		return pTable.getEstimatedDistance(parameter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * dissemination.newProtocol.ParameterDisseminator#getDistance(taxonomy.
	 * parameter.Parameter, peer.PeerID)
	 */
	@Override
	public synchronized int getDistance(final Parameter p, final PeerID neighbor) {
		return pTable.getDistance(p, neighbor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dissemination.newProtocol.ParameterDisseminator#getParameters()
	 */
	@Override
	public synchronized Set<Parameter> getParameters() {
		return pTable.getParameters();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see dissemination.newProtocol.ParameterDisseminator#getLocalParameters()
	 */
	@Override
	public synchronized Set<Parameter> getLocalParameters() {
		return pTable.getLocalParameters();
	}

	@Override
	public synchronized void appearedNeighbors(final PeerIDSet neighbors) {
		
			logger.trace("Peer " + peer.getPeerID() + " detected appearance of neighbors: " + neighbors);
		// Enqueue action only if table is not empty
		final UpdateTable updateTable = pTable.getNewNeighborTable();

		if (!updateTable.isEmpty())
			notifyNeighbors(neighbors);

		neighborListener.appearedNeighbors(neighbors);
	}

	@Override
	public synchronized void dissapearedNeighbors(final PeerIDSet neighbors) {
		
			logger.trace("Peer " + peer.getPeerID() + " detected dissapearance of neighbors: " + neighbors);
		// Remove entries in pTable which were obtained from disappeared
		// neighbors and send message to neighbors
		final UpdateTable finalUpdateTable = new UpdateTable();
		for (final PeerID neighbor : neighbors)
			for (final Parameter p : pTable.getParameters(neighbor)) {
				final UpdateTable removalTable = new UpdateTable();
				removalTable.setDelete(p, neighbor);
				final UpdateTable updateTable = pTable.updateTable(removalTable, neighbor);
				finalUpdateTable.merge(updateTable);
			}

		
			logger.trace("Peer " + peer.getPeerID() + " table after removal: " + pTable);

		
			logger.trace("Peer " + peer.getPeerID() + " update table: " + finalUpdateTable);

		if (!finalUpdateTable.isEmpty())
			sendUpdateTableMessage(finalUpdateTable, null);

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

		// Create the parameter table using loaded configuration
		pTable = new ParameterTable(MAX_DISTANCE, peer.getPeerID(), getTaxonomy());
	}

	@Override
	public synchronized void messageReceived(final BroadcastMessage message, final long receptionTime) {
		// Check if message is a table message
		if (message instanceof TableMessage) {
			final TableMessage tableMessage = (TableMessage) message;
			
				logger.trace("Peer " + peer.getPeerID() + " received table message " + message);
			processTableMessage(tableMessage);
		}
	}

	@Override
	public synchronized void saveToXML(final OutputStream os) throws IOException {
		pTable.saveToXML(os);
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
		throw new UnsupportedOperationException("ParameterTableUpdater.readFromXML method not implemented!");
	}

	@Override
	public void stop() {
	}

	private void notifyNeighbors(final PeerIDSet neighbors) {
		final UpdateTable newNeighborTable = pTable.getNewNeighborTable();

		final PeerIDSet destinations = new PeerIDSet();

		final PeerIDSet currentNeighbors = peer.getDetector().getCurrentNeighbors();

		for (final PeerID neighbor : neighbors)
			if (currentNeighbors.contains(neighbor))
				destinations.addPeer(neighbor);

		sendUpdateTableMessage(newNeighborTable, null);
	}

	private void processTableMessage(final TableMessage tableMessage) {
		if (!tableMessage.getUpdateTable().isEmpty()) {
			
				logger.trace("Peer " + peer.getPeerID() + " updating table " + pTable + " with update table " + tableMessage.getUpdateTable() + " from neighbor " + tableMessage.getSender());

			// Get parameters before update
			final Set<Parameter> parametersBeforeUpdate = pTable.getParameters();

			final Map<Parameter, DistanceChange> changedParameters = new HashMap<Parameter, DistanceChange>();
			for (final Parameter parameter : pTable.getParameters())
				changedParameters.put(parameter, new DistanceChange(pTable.getEstimatedDistance(parameter), 0));

			final UpdateTable updateTable = pTable.updateTable(tableMessage.getUpdateTable(), tableMessage.getSender());

			
				logger.trace("Peer " + peer.getPeerID() + " local table after update " + pTable);

			// Get parameters after update
			final Set<Parameter> parametersAfterUpdate = pTable.getParameters();

			PayloadMessage payload = null;

			// obtain distance changes
			for (final Parameter parameter : pTable.getParameters())
				if (changedParameters.containsKey(parameter)) {
					final DistanceChange dChange = changedParameters.get(parameter);
					changedParameters.put(parameter, new DistanceChange(dChange.getPreviousValue(), pTable.getEstimatedDistance(parameter)));
				} else
					changedParameters.put(parameter, new DistanceChange(0, pTable.getEstimatedDistance(parameter)));

			final Set<Parameter> addedParameters = new HashSet<Parameter>();
			final Set<Parameter> removedParameters = new HashSet<Parameter>();

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

			// Notify table changes to listeners
			payload = notifyTableChangedListener(tableMessage.getSender(), addedParameters, removedParameters, new HashSet<Parameter>(), changedParameters, tableMessage.getPayload());

			
				logger.trace("Peer " + peer.getPeerID() + " addedParameters: " + addedParameters + " removedParameters: " + removedParameters);
			logger.trace("Peer " + peer.getPeerID() + " adding payload " + payload + " to table message");

			// Send a new table message with a specified payload and responding
			// to received message
			sendUpdateTableMessage(updateTable, payload);
		}
	}

	// Sends a message which contains a table to be added or removed
	private void sendUpdateTableMessage(final UpdateTable updateTable, final PayloadMessage payload) {
		// Only send message if tables are non empty
		if (!updateTable.isEmpty()) {
			TableMessage tableMessage;

			tableMessage = new TableMessage(updateTable, peer.getPeerID(), payload);

			
				logger.trace("Peer " + peer.getPeerID() + " sending update table message " + tableMessage);
			// Perform the broadcasting of table message
			peer.enqueueBroadcast(tableMessage);
		} else 
			logger.trace("Peer " + peer.getPeerID() + " update table is empty and is not sent");
	}

	// Notifies the table changed listener
	private PayloadMessage notifyTableChangedListener(final PeerID neighbor, final Set<Parameter> addedParameters, final Set<Parameter> removedParameters, final Set<Parameter> removedLocalParameters,
			final Map<Parameter, DistanceChange> changedParameters, final PayloadMessage payload) {
		if (tableChangedListener != null)
			return tableChangedListener.parametersChanged(neighbor, addedParameters, removedParameters, removedLocalParameters, changedParameters, payload);

		return null;
	}
}
