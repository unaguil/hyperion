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

package graphsearch.backward.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.message.BroadcastMessage;
import peer.message.MessageID;
import peer.message.MessageTypes;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

public class BCompositionMessage extends RemoteMessage {

	// stores the services in the order they are added to the composition
	private final Set<Service> compositionServices = new HashSet<Service>();

	// the destination services of this message
	private final Set<ServiceDistance> destServices = new HashSet<ServiceDistance>();
	// a table which saves the distance among services
	private final Map<Service, Set<ServiceDistance>> ancestorDistances = new HashMap<Service, Set<ServiceDistance>>();

	// the service which originates the message
	private final Service sourceService;

	// the identifier of the current search
	private final SearchID searchID;

	// the message partition
	private final MessagePart messagePart;

	private final byte ttl;

	private final short remainingTime;
	
	public BCompositionMessage() {
		super(MessageTypes.BCOMPOSITION_MESSAGE);
		sourceService = new Service();
		searchID = new SearchID();
		messagePart = new MessagePart();
		ttl = 0;
		remainingTime = 0;
	}

	/**
	 * Constructor of the forward composition message
	 * 
	 * @param searchID
	 *            the identifier of the current search
	 * @param sourceService
	 *            the service which originates the message
	 * @param destServices
	 *            the services which are the destination of this message
	 * @param ttl
	 *            the TTL of the message
	 * @param remainingTime
	 *            the remaining time for this message to expire
	 * @param peerID
	 *            the peerID which created the message
	 */
	public BCompositionMessage(final SearchID searchID, final Service sourceService, final Set<ServiceDistance> destServices, final int ttl, final long remainingTime, final PeerID peerID) {
		super(MessageTypes.BCOMPOSITION_MESSAGE, sourceService.getPeerID(), null, Collections.<PeerID> emptySet());
		this.searchID = searchID;
		this.destServices.addAll(destServices);
		this.sourceService = sourceService;
		this.ttl = (byte)ttl;
		this.remainingTime = (short)remainingTime;
		this.messagePart = new MessagePart(peerID);

		compositionServices.add(sourceService);

		ancestorDistances.put(sourceService, destServices);
	}

	private BCompositionMessage(final SearchID searchID, final Service sourceService, final Set<ServiceDistance> destServices, final int ttl, final long remainingTime, final MessagePart newMessagePart) {
		super(MessageTypes.BCOMPOSITION_MESSAGE, sourceService.getPeerID(), null, Collections.<PeerID> emptySet());
		this.searchID = searchID;
		this.destServices.addAll(destServices);
		this.sourceService = sourceService;
		this.ttl = (byte)ttl;
		this.remainingTime = (short)remainingTime;
		this.messagePart = newMessagePart;

		compositionServices.add(sourceService);

		ancestorDistances.put(sourceService, destServices);
	}

	/**
	 * Gets the identifier of the current search
	 * 
	 * @return the identifier of the associated search
	 */
	public SearchID getSearchID() {
		return searchID;
	}

	/**
	 * The services which participate in the composition
	 * 
	 * @return the collection of services which participate in the composition
	 */
	public Set<Service> getComposition() {
		return compositionServices;
	}

	/**
	 * The service which originates the message
	 * 
	 * @return the service which originates the message
	 */
	public Service getSourceService() {
		return sourceService;
	}

	/**
	 * Gets the current TTL of the message
	 * 
	 * @return the current TTL of the message
	 */
	public int getTTL() {
		return ttl;
	}

	/**
	 * Gets the remaining time for this message to expire
	 * 
	 * @return the remaining time for the expiration of this message
	 */
	public long getRemainingTime() {
		return remainingTime;
	}

	/**
	 * The set of services which are the destinations of this message
	 * 
	 * @return the destination services
	 */
	public Set<ServiceDistance> getDestServices() {
		return destServices;
	}

	public Map<Service, Set<ServiceDistance>> getAncestorDistances() {
		return ancestorDistances;
	}

	@Override
	public String toString() {
		return super.toString() + " searchID: " + searchID + " services: " + compositionServices.toString();
	}

	public MessageID getRootID() {
		return messagePart.getRootID();
	}

	public MessagePart getMessagePart() {
		return messagePart;
	}

	public Map<Service, BCompositionMessage> split(final Service service, final Set<ServiceDistance> destinationServices, final PeerID peerID, final long messagesRemainingTime) {
		final Map<Service, BCompositionMessage> messages = new HashMap<Service, BCompositionMessage>();
		final Set<MessagePart> messageParts = this.messagePart.split(destinationServices.size(), peerID);
		final Iterator<MessagePart> it = messageParts.iterator();
		for (final ServiceDistance antecessor : destinationServices) {
			final MessagePart newMessagePart = it.next();
			final BCompositionMessage message = new BCompositionMessage(this.searchID, service, destinationServices, this.ttl - 1, messagesRemainingTime, newMessagePart);
			// add the ancestor
			message.getComposition().add(antecessor.getService());
			// add the current composition
			message.getComposition().addAll(this.getComposition());
			messages.put(antecessor.getService(), message);

			copyDistances(message.ancestorDistances, this.ancestorDistances);

			// add new distances
			final Map<Service, Set<ServiceDistance>> newDistances = new HashMap<Service, Set<ServiceDistance>>();
			newDistances.put(service, destinationServices);
			copyDistances(message.ancestorDistances, newDistances);
		}
		return messages;
	}

	@Override
	public BroadcastMessage copy() {
		final BCompositionMessage bCompositionMessage = new BCompositionMessage(getSearchID(), getSourceService(), getDestServices(), getTTL(), getRemainingTime(), getMessagePart());
		bCompositionMessage.compositionServices.addAll(compositionServices);
		copyDistances(bCompositionMessage.ancestorDistances, this.ancestorDistances);
		return bCompositionMessage;
	}

	private void copyDistances(final Map<Service, Set<ServiceDistance>> destination, final Map<Service, Set<ServiceDistance>> original) {
		for (final Entry<Service, Set<ServiceDistance>> entry : original.entrySet()) {
			final Service service = entry.getKey();
			if (!destination.containsKey(service))
				destination.put(service, new HashSet<ServiceDistance>());
			destination.get(service).addAll(entry.getValue());
		}
	}
	
	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		SerializationUtils.readServices(compositionServices, in);
		SerializationUtils.readServiceDistances(destServices, in);
		SerializationUtils.readServiceMap(ancestorDistances, in);
		sourceService.read(in);
		searchID.read(in);
		SerializationUtils.setFinalField(BCompositionMessage.class, this, "ttl", in.readByte());
		messagePart.read(in);
		SerializationUtils.setFinalField(BCompositionMessage.class, this, "remainingTime", in.readShort());
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.writeCollection(compositionServices, out);
		SerializationUtils.writeCollection(destServices, out);

		SerializationUtils.writeServiceMap(ancestorDistances, out);
		sourceService.write(out);
		searchID.write(out);
		out.writeByte(ttl);
		messagePart.write(out);
		out.writeShort(remainingTime);
	}
}
