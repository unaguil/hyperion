package standalone;

import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.services.Service;
import graphcreation.services.ServiceList;
import graphsearch.CompositionListener;
import graphsearch.CompositionSearch;
import graphsearch.SearchID;
import graphsearch.forward.ForwardCompositionSearch;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Set;

import peer.BasicPeer;
import peer.CommProvider;
import peer.Peer;
import peer.message.BroadcastMessage;
import peer.peerid.PeerID;
import util.logger.Logger;
import config.Configuration;

public class StandAlonePeer implements CommProvider, CompositionListener {

	// Directory used to output information
	protected static final String TEMP_DIR = "tmp";

	// Basic peer
	protected final Peer peer;

	// The UDP socket used by the peer for communication.
	private MulticastSocket socket;
	private InetAddress group;

	private static final int SO_TIMEOUT = 5;

	private static final int BUFF_SIZE = 65536; // TODO Check this value
	private final byte[] recvBuffer = new byte[BUFF_SIZE];

	private final CompositionSearch compositionSearch;

	private final String servicesDir;

	private static final String MULTICAST_GROUP = "230.0.0.1";
	private static final int DEFAULT_PORT = 5555;

	private final Logger logger = Logger.getLogger(StandAlonePeer.class);

	public StandAlonePeer(final String configurationFile, final String servicesDir) {
		Logger.setDeltaTime(System.currentTimeMillis());
		Configuration.setFile(configurationFile);

		this.servicesDir = servicesDir;

		peer = new BasicPeer(this);

		compositionSearch = new ForwardCompositionSearch(peer, this);
	}

	public void start(final PeerID peerID) throws IOException {
		peer.initPeer(peerID);
	}

	protected void stop() {
		peer.stopPeer();
		peer.printStatistics();
	}

	@Override
	public void initComm() throws IOException {
		socket = new MulticastSocket(DEFAULT_PORT);
		group = InetAddress.getByName(MULTICAST_GROUP);
		socket.joinGroup(group);

		logger.info("Peer " + peer.getPeerID() + " joined to multicast group " + MULTICAST_GROUP + " on port " + DEFAULT_PORT);

		loadData();
	}

	@Override
	public void broadcast(final byte[] data) throws IOException {
		// Create a new datagram packet and send it using the socket
		final DatagramPacket p = new DatagramPacket(data, data.length, group, DEFAULT_PORT);
		socket.send(p);
	}

	@Override
	public byte[] receiveData() throws IOException {
		// Creates the reception buffer and packet
		final DatagramPacket packet = new DatagramPacket(recvBuffer, recvBuffer.length);
		socket.setSoTimeout(SO_TIMEOUT);
		socket.receive(packet);
		return packet.getData();
	}

	@Override
	public boolean isValid(final BroadcastMessage message) {
		return !message.getSender().equals(peer.getPeerID());
	}

	@Override
	public void stopComm() throws IOException {
		socket.leaveGroup(group);
		socket.close();

		logger.info("Peer " + peer.getPeerID() + " leaved multicast group " + MULTICAST_GROUP);
	}

	public static void main(final String args[]) throws IOException {
		if (args.length < 3) {
			System.out.println("Usage: StandAlonePeer [PeerID] [ConfigurationFile] [ServicesDir]");
			System.exit(0);
		}

		final StandAlonePeer peer = new StandAlonePeer(args[1], args[2]);

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				peer.stop();
			}
		});

		peer.start(new PeerID(args[0]));
	}

	@Override
	public void compositionFound(final ExtendedServiceGraph composition, final SearchID searchID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compositionTimeExpired(final SearchID searchID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compositionsLost(final SearchID searchID, final ExtendedServiceGraph invalidComposition) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compositionModified(final SearchID searchID, final Set<Service> removedServices) {
		// TODO Auto-generated method stub

	}

	public void loadData() {
		try {
			final String xmlPath = getServicesFilePath(peer.getPeerID());
			final ServiceList sList = new ServiceList(xmlPath, peer.getPeerID());
			logger.info("Peer " + peer.getPeerID() + " adding " + sList.size() + " local services");
			compositionSearch.manageLocalServices(sList, new ServiceList());

		} catch (final Exception e) {
			logger.error("Peer " + peer.getPeerID() + " error loading data." + e.getMessage());
		}
	}

	private String getServicesFilePath(final PeerID peerID) {
		return servicesDir + File.separator + "Services" + peerID + ".xml";
	}
}
