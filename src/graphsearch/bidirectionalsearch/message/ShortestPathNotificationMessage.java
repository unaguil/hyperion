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

package graphsearch.bidirectionalsearch.message;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;
import graphsearch.SearchID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import multicast.search.message.RemoteMessage;
import peer.peerid.PeerID;
import serialization.binary.SerializationUtils;

public abstract class ShortestPathNotificationMessage extends RemoteMessage {

	private final SearchID searchID;

	protected final Map<Service, Set<ServiceDistance>> serviceDistances = new HashMap<Service, Set<ServiceDistance>>();

	protected final List<Service> notificationPath = new ArrayList<Service>();

	protected final Service destination;
	
	public ShortestPathNotificationMessage(final byte mType) {
		super(mType);
		searchID = new SearchID();
		destination = new Service();
	}

	public ShortestPathNotificationMessage(final byte mType, final PeerID source, final SearchID searchID, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath) {
		super(mType, source, null, Collections.<PeerID> emptySet());

		this.searchID = searchID;
		this.serviceDistances.putAll(serviceDistances);
		this.notificationPath.addAll(notificationPath);
		this.destination = notificationPath.get(notificationPath.size() - 1);
	}

	protected ShortestPathNotificationMessage(final byte mType, final PeerID source, final SearchID searchID, final Map<Service, Set<ServiceDistance>> serviceDistances, final List<Service> notificationPath, final Service destination) {
		super(mType, source, null, Collections.<PeerID> emptySet());

		this.searchID = searchID;
		this.serviceDistances.putAll(serviceDistances);
		this.notificationPath.addAll(notificationPath);
		this.destination = destination;
	}

	public SearchID getSearchID() {
		return searchID;
	}

	public boolean hasMoreElements() {
		return !notificationPath.isEmpty();
	}

	public Service nextService() {
		final Service service = notificationPath.get(0);
		notificationPath.remove(0);
		return service;
	}

	public Service getDestination() {
		return destination;
	}

	private int getDistance(final Service currentService, final Service nextService) {
		if (serviceDistances.containsKey(currentService))
			for (final ServiceDistance sDistance : serviceDistances.get(currentService))
				if (sDistance.getService().equals(nextService)) {
					final int distance = sDistance.getDistance().intValue();
					return distance;
				}
		return 0;
	}

	public int getPathDistance() {
		if (notificationPath.size() > 1) {
			int distance = 0;

			int current = 0;
			do {
				final Service currentService = notificationPath.get(current);
				final Service nextService = notificationPath.get(current + 1);

				distance += getDistance(currentService, nextService);
				distance += getDistance(nextService, currentService);

				current++;
			} while (current < notificationPath.size() - 1);

			return distance;
		}

		return 0;
	}

	@Override
	public String toString() {
		return "searchID: " + searchID + " notificationPath: " + notificationPath;
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		searchID.read(in);
		SerializationUtils.readServiceMap(serviceDistances, in);
		SerializationUtils.readServices(notificationPath, in);		
		destination.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		out.writeObject(searchID);
		SerializationUtils.writeServiceMap(serviceDistances, out);
		SerializationUtils.writeCollection(notificationPath, out);
		destination.write(out);
	}
}
