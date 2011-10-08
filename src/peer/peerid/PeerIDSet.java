package peer.peerid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import serialization.xml.XMLSerializable;

/**
 * A class which defines a set of peer ids (non-duplicates).
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public final class PeerIDSet implements Serializable, Iterable<PeerID>, XMLSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final static String NEIGHBOURS_TAG = "neighbours";
	private final static String PEER_TAG = "peer";
	private final static String ID_ATTRIB = "id";

	// back end set
	private final Set<PeerID> peerSet = new HashSet<PeerID>();

	/**
	 * Constructs an empty peer set
	 */
	public PeerIDSet() {
	}

	/**
	 * Constructs a peer set copying references from other
	 * 
	 * @param pSet
	 *            the peer set where references are copied from
	 */
	public PeerIDSet(final PeerIDSet pSet) {
		for (final PeerID peer : pSet)
			peerSet.add(peer);
	}

	/**
	 * Constructs a peer set copying the references from a collection.
	 * Duplicates are removed.
	 * 
	 * @param peers
	 *            a collection containing peers.
	 */
	public PeerIDSet(final Collection<PeerID> peers) {
		addPeers(peers);
	}

	/**
	 * Adds a collection of peers to the set. Duplicates are removed.
	 * 
	 * @param peers
	 *            the collection of peers to add.
	 */
	public void addPeers(final Collection<PeerID> peers) {
		for (final PeerID peer : peers)
			peerSet.add(peer);
	}

	/**
	 * Adds all the peers contained in the passed peer set.
	 * 
	 * @param peerIDSet
	 *            the peer set whose contains are added.
	 */
	public void addPeers(final PeerIDSet peerIDSet) {
		this.peerSet.addAll(peerIDSet.peerSet);
	}

	/**
	 * Adds a peer to the peer set
	 * 
	 * @param peer
	 */
	public void addPeer(final PeerID peer) {
		peerSet.add(peer);
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		final Element root = doc.createElement(NEIGHBOURS_TAG);
		doc.appendChild(root);
		for (final PeerID peer : peerSet) {
			final Element peerElement = doc.createElement(PEER_TAG);
			peerElement.setAttribute(ID_ATTRIB, peer.toString());
			root.appendChild(peerElement);
		}

		Transformer transformer = null;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (final TransformerConfigurationException tce) {
			throw new IOException(tce);
		}

		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(os);
		try {
			transformer.transform(source, result);
		} catch (final TransformerException te) {
			throw new IOException(te);
		}
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		Document document = null;
		try {
			document = docBuilder.parse(is);
		} catch (final SAXException e) {
			throw new IOException(e);
		}

		final NodeList peerEntries = document.getElementsByTagName(PEER_TAG);
		for (int i = 0; i < peerEntries.getLength(); i++) {
			final Element peerElement = (Element) peerEntries.item(i);
			final String id = peerElement.getAttribute(ID_ATTRIB);
			peerSet.add(new PeerID(id));
		}
	}

	@Override
	public String toString() {
		return peerSet.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof PeerIDSet))
			return false;

		final PeerIDSet nList = (PeerIDSet) o;
		return this.peerSet.equals(nList.peerSet);
	}

	@Override
	public int hashCode() {
		return peerSet.hashCode();
	}

	@Override
	public Iterator<PeerID> iterator() {
		return peerSet.iterator();
	}

	/**
	 * Checks if the set contains the passed peer.
	 * 
	 * @param peerID
	 *            the peer to check if it is contained in the set
	 * @return true if the peer is contained in the set, false otherwise
	 */
	public boolean contains(final PeerID peerID) {
		return peerSet.contains(peerID);
	}

	/**
	 * Tells if the peer set is empty
	 * 
	 * @return true if the peer set is empty, false otherwise
	 */
	public boolean isEmpty() {
		return peerSet.isEmpty();
	}

	/**
	 * Tries to remove the passed peer from the set
	 * 
	 * @param peerID
	 *            the peer to remove
	 * @return true if the peer was removed, false otherwise
	 */
	public boolean remove(final PeerID peerID) {
		return peerSet.remove(peerID);
	}

	/**
	 * Gets the size of the peer id set
	 * 
	 * @return the size of the set
	 */
	public int size() {
		return peerSet.size();
	}

	/**
	 * Clears the collection of peers
	 */
	public void clear() {
		peerSet.clear();
	}

	public void remove(final PeerIDSet peers) {
		for (final PeerID peerID : peers)
			remove(peerID);
	}

	public Set<PeerID> getPeerSet() {
		return new HashSet<PeerID>(peerSet);
	}
}
