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
*   Author: Unai Aguilera <gkalgan@gmail.com>
*/

package peer.message;

import floodsearch.message.FloodCompositionMessage;
import graphcreation.collisionbased.message.CollisionResponseMessage;
import graphcreation.collisionbased.message.ConnectServicesMessage;
import graphcreation.collisionbased.message.DisconnectServicesMessage;
import graphcreation.collisionbased.message.ForwardMessage;
import graphcreation.collisionbased.message.InhibeCollisionsMessage;
import graphcreation.collisionbased.message.RemovedServicesMessage;
import graphsearch.backward.message.BCompositionMessage;
import graphsearch.forward.message.FCompositionMessage;

import java.io.IOException;
import java.io.ObjectInputStream;

import multicast.search.message.GeneralizeSearchMessage;
import multicast.search.message.RemoteMulticastMessage;
import multicast.search.message.RemoveParametersMessage;
import multicast.search.message.RemoveRouteMessage;
import multicast.search.message.SearchMessage;
import multicast.search.message.SearchResponseMessage;
import detection.message.BeaconMessage;
import dissemination.newProtocol.message.TableMessage;

public class MessageTypes {

	public static final byte BEACON_MESSAGE = 0x01;
	public static final byte BUNDLE_MESSAGE = 0x02;
	public static final byte ACK_MESSAGE = 0x03;
	public static final byte MESSAGE_STRING = 0x04;
	
	public static final byte TABLE_MESSAGE = 0x05;
	
	public static final byte SEARCH_MESSAGE = 0x06;
	public static final byte REMOVE_ROUTE_MESSAGE = 0x07;
	public static final byte REMOVE_PARAM_MESSAGE = 0x08;
	public static final byte GENERALIZE_SEARCH_MESSAGE = 0x09;
	public static final byte SEARCH_RESPONSE_MESSAGE = 0x0A;
	public static final byte REMOTE_MULTICAST_MESSAGE = 0x0B;
	
	public static final byte REMOVED_SERVICE_MESSAGE = 0x0C;
	public static final byte INHIBE_COLLISIONS_MESSAGE = 0x0D;
	public static final byte FORWARD_MESSAGE = 0x0E;
	public static final byte DISCONNECT_SERVICES_MESSAGE = 0x0F;
	public static final byte CONNECT_SERVICES_MESSAGE = 0x10;
	public static final byte COLLISION_RESPONSE_MESSAGE = 0x11;
	
	public static final byte FCOMPOSITION_MESSAGE = 0x12;
	
	public static final byte BCOMPOSITION_MESSAGE = 0x13;
	public static final byte COMPOSITION_NOTIFICATION_MESSAGE = 0x14;
	
	public static final byte FLOOD_COMPOSITION_MESSAGE = 0x15;
	
	private static BroadcastMessage getInstance(final byte mType) throws UnsupportedTypeException {
		switch (mType) {
			case BEACON_MESSAGE:
				return new BeaconMessage();
			case BUNDLE_MESSAGE:
				return new BundleMessage();
			case ACK_MESSAGE:
				return new ACKMessage();
			case MESSAGE_STRING:
				return new MessageString();
			case TABLE_MESSAGE:
				return new TableMessage();
			case REMOTE_MULTICAST_MESSAGE:
				return new RemoteMulticastMessage();
			case SEARCH_MESSAGE:
				return new SearchMessage();
			case REMOVE_ROUTE_MESSAGE:
				return new RemoveRouteMessage();
			case REMOVE_PARAM_MESSAGE:
				return new RemoveParametersMessage();
			case GENERALIZE_SEARCH_MESSAGE:
				return new GeneralizeSearchMessage();
			case SEARCH_RESPONSE_MESSAGE:
				return new SearchResponseMessage();
			case REMOVED_SERVICE_MESSAGE:
				return new RemovedServicesMessage();
			case INHIBE_COLLISIONS_MESSAGE:
				return new InhibeCollisionsMessage();
			case FORWARD_MESSAGE:
				return new ForwardMessage();
			case DISCONNECT_SERVICES_MESSAGE:
				return new DisconnectServicesMessage();
			case CONNECT_SERVICES_MESSAGE:
				return new ConnectServicesMessage();
			case COLLISION_RESPONSE_MESSAGE:
				return new CollisionResponseMessage();
			case FCOMPOSITION_MESSAGE:
				return new FCompositionMessage();
			case BCOMPOSITION_MESSAGE:
				return new BCompositionMessage();
			case FLOOD_COMPOSITION_MESSAGE:
				return new FloodCompositionMessage();
		}
		
		throw new UnsupportedTypeException("Message type " + mType + " not supported");
	}
	
	public static BroadcastMessage readBroadcastMessage(final ObjectInputStream in) throws IOException, UnsupportedTypeException {
		final byte mType = in.readByte();
		final BroadcastMessage bMessage = MessageTypes.getInstance(mType);
		bMessage.read(in);
		return bMessage;
	}
}
