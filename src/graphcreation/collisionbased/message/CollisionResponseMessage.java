/*
*   Copyright (c) 2012 Unai Aguilera
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*
*  
*   Author: Unai Aguilera <unai.aguilera@deusto.es>
*/

package graphcreation.collisionbased.message;

import graphcreation.services.Service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;

public class CollisionResponseMessage extends RemoteMessage {

	private final Map<Service, Byte> serviceDistanceTable = new HashMap<Service, Byte>();
	
	public CollisionResponseMessage() {
		super(MessageTypes.COLLISION_RESPONSE_MESSAGE);
	}

	public CollisionResponseMessage(final PeerID source, final Map<Service, Byte> serviceTable) {
		super(MessageTypes.COLLISION_RESPONSE_MESSAGE, source, null, Collections.<PeerID> emptySet());
		serviceDistanceTable.putAll(serviceTable);
	}
	
	protected CollisionResponseMessage(final CollisionResponseMessage collisionResponseMessage) {
		super(MessageTypes.COLLISION_RESPONSE_MESSAGE, collisionResponseMessage);
		
		for (final Entry<Service, Byte> entry : collisionResponseMessage.serviceDistanceTable.entrySet())
			serviceDistanceTable.put(entry.getKey(), new Byte(entry.getValue().byteValue()));
	}

	public Set<Service> getServices() {
		return new HashSet<Service>(serviceDistanceTable.keySet());
	}
	
	public Set<Byte> getDistances() {
		return new HashSet<Byte>(serviceDistanceTable.values());
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
	public BroadcastMessage copy() {		
		return new CollisionResponseMessage(this);
	}

	public void addDistance(final int distance) {
		for (final Service service : serviceDistanceTable.keySet()) {
			byte newDistance = (byte)(serviceDistanceTable.get(service).intValue() + distance);
			serviceDistanceTable.put(service, new Byte(newDistance));
		}
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		final byte entrySize = in.readByte();
		for (int i = 0; i < entrySize; i++) {
			final Service service = new Service();
			service.read(in);
			final Byte value = new Byte(in.readByte());
			serviceDistanceTable.put(service, value);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeByteMap(serviceDistanceTable, out);
	}
}
