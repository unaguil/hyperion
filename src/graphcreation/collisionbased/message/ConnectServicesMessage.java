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

import graphcreation.collisionbased.ServiceDistance;
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

public class ConnectServicesMessage extends RemoteMessage {

	private final Map<Service, Set<ServiceDistance>> remoteSuccessors = new HashMap<Service, Set<ServiceDistance>>();
	private final Map<Service, Set<ServiceDistance>> remoteAncestors = new HashMap<Service, Set<ServiceDistance>>();
	
	public ConnectServicesMessage() {
		super(MessageTypes.CONNECT_SERVICES_MESSAGE);
	}

	public ConnectServicesMessage(final PeerID source, final Map<Service, Set<ServiceDistance>> remoteSuccessors, final Map<Service, Set<ServiceDistance>> remoteAncestors) {
		super(MessageTypes.CONNECT_SERVICES_MESSAGE, source, null, Collections.<PeerID> emptySet());
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
	public BroadcastMessage copy() {
		return new ConnectServicesMessage(getSource(), deepCopyMap(getRemoteSuccessors()), deepCopyMap(getRemoteAncestors()));
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readServiceMap(remoteSuccessors, in);
		SerializationUtils.readServiceMap(remoteAncestors, in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeServiceMap(remoteSuccessors, out);
		SerializationUtils.writeServiceMap(remoteAncestors, out);
	}
}
