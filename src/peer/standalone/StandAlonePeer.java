package peer.standalone;

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
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import config.Configuration;

import peer.BasicPeer;
import peer.Peer;
import peer.PeerBehavior;
import peer.peerid.PeerID;

public class StandAlonePeer implements PeerBehavior, CompositionListener {

	// Directory used to output information
	protected static final String TEMP_DIR = "tmp";

	// local socket address
	private java.net.InetSocketAddress socketAddress;
	
	private ServiceList findServices;

	private final Map<SearchID, List<ExtendedServiceGraph>> receivedCompositions = new HashMap<SearchID, List<ExtendedServiceGraph>>();

	// Basic peer
	protected final Peer peer;

	// The UDP socket used by the peer for communication.
	private DatagramSocket socket;

	private static final int SO_TIMEOUT = 5;

	private static final int BUFF_SIZE = 65536; // TODO Check this value
	private byte[] recvBuffer = new byte[BUFF_SIZE];

	private static final int LISTEN_PORT = 5555;
	private static final int DEST_PORT = 6666;

	private CompositionSearch compositionSearch;
	
	private final String servicesDir;

	private final Logger logger = Logger.getLogger(StandAlonePeer.class);

	public StandAlonePeer(String configurationFile, String servicesDir) {
		Configuration.setFile(configurationFile);
		
		this.servicesDir = servicesDir;
		
		peer = new BasicPeer(this);
		
		compositionSearch = new ForwardCompositionSearch(peer, this);
	}

	public void start(PeerID peerID) throws IOException {
		peer.initPeer(peerID);
	}

	@Override
	public void init() throws IOException {
		socket = new DatagramSocket(LISTEN_PORT);
		socketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), DEST_PORT);

		logger.info("Starting peer " + peer.getPeerID() + " listening on port " + LISTEN_PORT);
		
		loadData();
	}

	@Override
	public void broadcast(final byte[] data) throws IOException {
		// Create a new datagram packet and send it using the socket
		final DatagramPacket p = new DatagramPacket(data, data.length, socketAddress);
		socket.send(p);
	}

	@Override
	public byte[] receiveData() {
		// Creates the reception buffer and packet
		final DatagramPacket packet = new DatagramPacket(recvBuffer, recvBuffer.length);

		byte[] data = null;
		try {
			socket.setSoTimeout(SO_TIMEOUT);
			socket.receive(packet);
			data = packet.getData();
		} catch (SocketException e) {} 
		catch (IOException e) {}
		
		return data;
	}

	public static void main(String args[]) throws IOException {
		if (args.length < 3) {
			System.out.println("Usage: StandAlonePeer [PeerID] [ConfigurationFile] [ServicesDir]");
			System.exit(0);
		}

		StandAlonePeer peer = new StandAlonePeer(args[1], args[2]);

		peer.start(new PeerID(args[0]));
	}

	@Override
	public void compositionFound(ExtendedServiceGraph composition, SearchID searchID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compositionTimeExpired(SearchID searchID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compositionsLost(SearchID searchID, ExtendedServiceGraph invalidComposition) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compositionModified(SearchID searchID, Set<Service> removedServices) {
		// TODO Auto-generated method stub

	}
	
	public void loadData() {
		try {
			final String xmlPath = getServicesFilePath(peer.getPeerID());
			final ServiceList sList = new ServiceList(xmlPath, peer.getPeerID());
			compositionSearch.manageLocalServices(sList, new ServiceList());

		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getServicesFilePath(final PeerID peerID) {
		return servicesDir + File.separator + "Services" + peerID + ".xml";
	}
}
