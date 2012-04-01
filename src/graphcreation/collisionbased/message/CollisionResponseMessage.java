package graphcreation.collisionbased.message;

import graphcreation.services.Service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;

public class CollisionResponseMessage extends RemoteMessage implements PayloadMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<Service, Byte> serviceDistanceTable = new HashMap<Service, Byte>();
	
	public CollisionResponseMessage() {
		
	}

	public CollisionResponseMessage(final PeerID source, final Map<Service, Byte> serviceTable) {
		super(source, Collections.<PeerID> emptySet());
		serviceDistanceTable.putAll(serviceTable);
	}
	
	protected CollisionResponseMessage(final CollisionResponseMessage collisionResponseMessage) {
		super(collisionResponseMessage);
		
		for (final Entry<Service, Byte> entry : collisionResponseMessage.serviceDistanceTable.entrySet())
			serviceDistanceTable.put(entry.getKey(), new Byte(entry.getValue().byteValue()));
	}

	public Set<Service> getServices() {
		return new HashSet<Service>(serviceDistanceTable.keySet());
	}

	public Integer getDistance(final Service s) {
		return new Integer(serviceDistanceTable.get(s).byteValue());
	}

	public boolean removeServices(final Set<Service> services) {
		boolean removed = false;
		for (final Service service : services)
			if (serviceDistanceTable.containsKey(service)) {
				serviceDistanceTable.remove(service);
				removed = true;
			}
		return removed;
	}
	
	public Set<OutputParameter> getOutputParameters() {
		final Set<OutputParameter> outputParameters = new HashSet<OutputParameter>();
		for (final Service service : serviceDistanceTable.keySet())
			outputParameters.addAll(service.getOutputParams());
		return outputParameters;
	}
	
	public Set<InputParameter> getInputParameters() {
		final Set<InputParameter> inputParameters = new HashSet<InputParameter>();
		for (final Service service : serviceDistanceTable.keySet())
			inputParameters.addAll(service.getInputParams());
		return inputParameters;
	}

	@Override
	public String toString() {
		return super.toString() + " " + serviceDistanceTable;
	}

	@Override
	public PayloadMessage copy() {		
		return new CollisionResponseMessage(this);
	}

	public void addDistance(final int distance) {
		for (final Service service : serviceDistanceTable.keySet()) {
			byte newDistance = (byte)(serviceDistanceTable.get(service).intValue() + distance);
			serviceDistanceTable.put(service, new Byte(newDistance));
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.readMap(serviceDistanceTable, in);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(serviceDistanceTable.keySet().toArray(new Service[0]));
		out.writeObject(serviceDistanceTable.values().toArray(new Byte[0]));
	}
}
