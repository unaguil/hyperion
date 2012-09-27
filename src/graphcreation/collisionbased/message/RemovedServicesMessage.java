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
import java.util.HashSet;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

public class RemovedServicesMessage extends RemoteMessage {

	private final Set<Service> lostServices = new HashSet<Service>();
	
	public RemovedServicesMessage() {
		super (MessageTypes.REMOVED_SERVICE_MESSAGE);
	}

	public RemovedServicesMessage(final PeerID source, final Set<Service> lostServices) {
		super(MessageTypes.REMOVED_SERVICE_MESSAGE, source, null, Collections.<PeerID> emptySet());
		this.lostServices.addAll(lostServices);
	}

	public Set<Service> getLostServices() {
		return lostServices;
	}

	@Override
	public String toString() {
		return super.toString() + " " + lostServices;
	}

	@Override
	public BroadcastMessage copy() {
		return new RemovedServicesMessage(getSource(), getLostServices());
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readServices(lostServices, in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(lostServices, out);
	}
}
