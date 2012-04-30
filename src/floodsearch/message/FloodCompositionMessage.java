package floodsearch.message;

import graphcreation.services.Service;
import graphsearch.SearchID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.message.MessageIDGenerator;
import peer.message.MessageTypes;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

public class FloodCompositionMessage extends BroadcastMessage {
	
	private final SearchID searchID;
	
	private final MessageID version;
	
	// stores the services in the order they are added to the composition
	private final Set<Service> compositionServices = new HashSet<Service>();
	
	private short hops = 0;
	
	public FloodCompositionMessage() {
		super(MessageTypes.FLOOD_COMPOSITION_MESSAGE);
		searchID = new SearchID();
		version = new MessageID();
	}
	
	public FloodCompositionMessage(final PeerID sender, final FloodCompositionMessage fCompositionMessage) {
		super(MessageTypes.FLOOD_COMPOSITION_MESSAGE, sender, Collections.<PeerID> emptySet());
		this.searchID = fCompositionMessage.getSearchID();
		this.compositionServices.addAll(fCompositionMessage.getComposition());
		this.version = new MessageID(sender, MessageIDGenerator.getNewID());
		this.hops = fCompositionMessage.getHops();
	}

	public FloodCompositionMessage(final PeerID sender, final SearchID searchID) {
		super(MessageTypes.FLOOD_COMPOSITION_MESSAGE, sender, Collections.<PeerID> emptySet());
		this.searchID = searchID;
		this.version = new MessageID(sender, MessageIDGenerator.getNewID());
	}
	
	public void setHops(final short hops) {
		this.hops = hops;
	}
	
	public void addService(final Service service) {
		compositionServices.add(service);
	}
	
	public Set<Service> getComposition() {
		return compositionServices;
	}
	
	public SearchID getSearchID() {
		return searchID;
	}
	
	public MessageID getVersion() {
		return version;
	}
	
	public short getHops() {
		return hops;
	}
	
	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readServices(compositionServices, in);
		searchID.read(in);
		version.read(in);
		SerializationUtils.setFinalField(FloodCompositionMessage.class, this, "hops", in.readShort());
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);		
		
		SerializationUtils.writeCollection(compositionServices, out);
		searchID.write(out);
		version.write(out);
		out.writeShort(hops);
	}
}
