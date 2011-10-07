package peer.standalone;

import graphcreation.graph.extendedServiceGraph.ExtendedServiceGraph;
import graphcreation.services.Service;
import graphsearch.CompositionListener;
import graphsearch.CompositionSearch;
import graphsearch.SearchID;
import graphsearch.forward.ForwardCompositionSearch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketException;
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

	private final Logger logger = Logger.getLogger(StandAlonePeer.class);

	public StandAlonePeer(String configurationFile) {
		Configuration.setFile(configurationFile);
		
		peer = new BasicPeer(this);
	}

	public void init(PeerID peerID) throws IOException {
		peer.initPeer(peerID);
	}

	@Override
	public void init() throws IOException {
		compositionSearch = new ForwardCompositionSearch(peer, this);
		
		socket = new DatagramSocket(LISTEN_PORT);
		socketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), DEST_PORT);

		logger.info("Starting peer " + peer.getPeerID() + " listening on port " + LISTEN_PORT);
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

	@Override
	public void loadData() {
		// TODO Auto-generated method stub
	}

	public static void main(String args[]) throws IOException {
		if (args.length < 2) {
			System.out.println("Usage: StandAlonePeer [ConfigurationFile] [peerID]");
			System.exit(0);
		}

		StandAlonePeer peer = new StandAlonePeer(args[0]);

		peer.init(new PeerID(args[1]));
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
}
