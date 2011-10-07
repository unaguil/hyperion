package dissemination.peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import peer.BasicPeer;
import peer.PeerID;
import peer.PeerIDSet;
import peer.message.PayloadMessage;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;
import taxonomy.parameterList.ParameterList;
import util.logger.Logger;
import detection.NeighborEventsListener;
import dissemination.DistanceChange;
import dissemination.ParameterDisseminator;
import dissemination.TableChangedListener;
import dissemination.newProtocol.ParameterTableUpdater;

/**
 * This is an implementation of a peer which uses and tests the parameter
 * dissemination layer. It is intended for testing purposes.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class Peer extends BasicPeer implements TableChangedListener, NeighborEventsListener {

	private static final String REMOVE_PARAMETER = "removeParameter";

	private static final String ADD_PARAMETER = "addParameter";

	private static final String PARAMETERS_DIR = "parameters";

	private final ParameterDisseminator pDisseminator;

	private final Logger myLogger = Logger.getLogger(Peer.class);

	public Peer() {
		pDisseminator = new ParameterTableUpdater(this, this, this);
	}

	@Override
	protected boolean peerCommands(final String command, final String[] args) {
		if (command.equals(ADD_PARAMETER)) {
			if (args.length > 0)
				try {
					for (final String arg : args) {
						final Parameter parameter = ParameterFactory.createParameter(arg);
						myLogger.info("Peer " + this.getPeerID() + " adding local parameter: " + parameter);
						pDisseminator.addLocalParameter(parameter);
					}

					myLogger.info("Peer " + this.getPeerID() + " commiting local parameter changes");
					pDisseminator.commit();

					return true;

				} catch (final InvalidParameterIDException ipe) {
					myLogger.error("Peer " + getPeerID() + " processed invalid parameter. " + ipe.getMessage());
					return false;
				}
			myLogger.error("Peer " + this.getPeerID() + " " + ADD_PARAMETER + " had no arguments");
		} else if (command.equals(REMOVE_PARAMETER)) {
			if (args.length > 0)
				try {
					for (final String arg : args) {
						final Parameter parameter = ParameterFactory.createParameter(arg);
						myLogger.info("Peer " + this.getPeerID() + " removing local parameter: " + parameter);
						pDisseminator.removeLocalParameter(parameter);
					}

					myLogger.info("Peer " + this.getPeerID() + " commiting local parameter changes");
					pDisseminator.commit();

					return true;
				} catch (final InvalidParameterIDException ipe) {
					myLogger.error("Peer " + getPeerID() + " processed invalid parameter. " + ipe.getMessage());
				}
			myLogger.error("Peer " + this.getPeerID() + " " + REMOVE_PARAMETER + " had no arguments");
		}
		return false;
	}

	@Override
	public void loadData() {
		try {
			final String xmlPath = getParametersFilePath(getPeerID());
			final ParameterList pList = new ParameterList(xmlPath);

			myLogger.info("Peer " + getPeerID() + " loading parameters: " + pList + " from file " + xmlPath);

			for (final Parameter parameter : pList.getParameterSet()) {
				myLogger.info("Peer " + this.getPeerID() + " adding local parameter: " + parameter);
				pDisseminator.addLocalParameter(parameter);
			}

			myLogger.info("Peer " + this.getPeerID() + " commiting local parameter changes");
			pDisseminator.commit();

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private String getParametersFilePath(final PeerID peerID) {
		return PARAMETERS_DIR + File.separator + "Parameters" + peerID + ".xml";
	}

	private String getPTableFilePath(final PeerID peerID) {
		return TEMP_DIR + File.separator + "PTable" + peerID + ".xml";
	}

	public void printDisseminationTable() {
		final String xmlPath = getPTableFilePath(getPeerID());
		try {
			final FileOutputStream f = new FileOutputStream(xmlPath);
			pDisseminator.saveToXML(f);
			f.close();
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
	}

	@Override
	public void printOutputs() {
		printDisseminationTable();
	}

	@Override
	public PayloadMessage parametersChanged(final PeerID neighbor, final Set<Parameter> addedParameters, final Set<Parameter> removedParameters, final Set<Parameter> removedLocalParameters, final Map<Parameter, DistanceChange> changedParameters,
			final PayloadMessage payload) {
		return null;
	}

	@Override
	public void appearedNeighbors(final PeerIDSet neighbours) {
	}

	@Override
	public void dissapearedNeighbors(final PeerIDSet neighbours) {
	}
}
