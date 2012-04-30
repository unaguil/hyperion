package graphcreation.collisionbased.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;

public class ConnectServicesMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<Service, Set<ServiceDistance>> remoteSuccessors = new HashMap<Service, Set<ServiceDistance>>();
	private final Map<Service, Set<ServiceDistance>> remoteAncestors = new HashMap<Service, Set<ServiceDistance>>();
	
	public ConnectServicesMessage() {
		
	}

	public ConnectServicesMessage(final PeerID source, final Map<Service, Set<ServiceDistance>> remoteSuccessors, final Map<Service, Set<ServiceDistance>> remoteAncestors) {
		super(source, Collections.<PeerID> emptySet());
		this.remoteAncestors.putAll(remoteAncestors);
		this.remoteSuccessors.putAll(remoteSuccessors);
	}

	public Map<Service, Set<ServiceDistance>> getRemoteSuccessors() {
		return remoteSuccessors;
	}

	public Map<Service, Set<ServiceDistance>> getRemoteAncestors() {
		return remoteAncestors;
	}

	@Override
	public String toString() {
		return super.toString() + " remoteAncestors: " + remoteAncestors + " remoteSuccessors: " + remoteSuccessors;
	}
	
	private Map<Service, Set<ServiceDistance>> deepCopyMap(final Map<Service, Set<ServiceDistance>> sourceMap) {
		final Map<Service, Set<ServiceDistance>> deepCopyMap = new HashMap<Service, Set<ServiceDistance>>();
		for (final Entry<Service, Set<ServiceDistance>> entry : sourceMap.entrySet()) {
			deepCopyMap.put(entry.getKey(), new HashSet<ServiceDistance>());
			deepCopyMap.get(entry.getKey()).addAll(entry.getValue());
		}
		return deepCopyMap;
	}

	@Override
	public PayloadMessage copy() {
		return new ConnectServicesMessage(getSource(), deepCopyMap(getRemoteSuccessors()), deepCopyMap(getRemoteAncestors()));
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		readMap(remoteSuccessors, in);
		readMap(remoteAncestors, in);
	}

	private void readMap(Map<Service, Set<ServiceDistance>> map, ObjectInput in) throws ClassNotFoundException, IOException {
		List<Service> keys = Arrays.asList((Service[])in.readObject());
		short size = in.readShort();
		List<Set<ServiceDistance>> values = new ArrayList<Set<ServiceDistance>>();
		for (int i = 0; i < size; i++) {
			Set<ServiceDistance> value = new HashSet<ServiceDistance>(Arrays.asList((ServiceDistance[])in.readObject()));
			values.add(value);
		}
		
		UnserializationUtils.fillMap(map, keys, values);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		writeMap(remoteSuccessors, out);
		writeMap(remoteAncestors, out);
	}

	private void writeMap(Map<Service, Set<ServiceDistance>> map, ObjectOutput out) throws IOException {
		out.writeObject(map.keySet().toArray(new Service[0]));
		out.writeShort(map.values().size());
		for (Set<ServiceDistance> set : map.values())
			out.writeObject(set.toArray(new ServiceDistance[0]));
	}
}
