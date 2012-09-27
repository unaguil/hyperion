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

package serialization.binary;

import graphcreation.collisionbased.ServiceDistance;
import graphcreation.services.Service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import peer.message.MessageID;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import taxonomy.parameter.Parameter;

public class SerializationUtils {

	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, final Object value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
	
	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, final long value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.setLong(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
	
	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, final int value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.setInt(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
	
	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, final short value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.setShort(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
	
	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, final byte value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.setByte(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
	
	public static void setFinalField(Class<?> clazz, Object obj, final String fieldName, final boolean value) throws IOException {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.setBoolean(obj, value);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new IOException(clazz + ". " + e.getMessage());
		}
	}
	
	public static <A, B> void fillMap(Map<A, B> map, A keys[], B values[]) {
		List<A> keyList = Arrays.asList(keys);
		List<B> valueList = Arrays.asList(values);
		
		fillMap(map, keyList, valueList);
	}

	public static <B, A> void fillMap(Map<A, B> map, List<A> keyList, List<B> valueList) {
		Iterator<A> itKeys = keyList.iterator();
		Iterator<B> itValues = valueList.iterator();
		
		while (itKeys.hasNext()) {
			map.put(itKeys.next(), itValues.next());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <A, B> void readMap(Map<A, B> map, ObjectInput in) throws ClassNotFoundException, IOException {
		A[] keys = (A[]) in.readObject();
		B[] values = (B[]) in.readObject();
		
		fillMap(map, keys, values);
	}
	
	public static <A extends BSerializable, B extends BSerializable> void writeMap(final Map<A, B> map, final ObjectOutputStream out) throws IOException {
		out.writeByte(map.size());
		for (final Entry<A, B> entry : map.entrySet()) {
			final A key  = entry.getKey();
			final B value = entry.getValue();
			key.write(out);
			value.write(out);
		}
	}
	
	public static <A extends BSerializable> void writeCollection(final Collection<A> collection, ObjectOutputStream out) throws IOException {
		out.writeByte(collection.size());
		for (final A value : collection)
			value.write(out);
	}
	
	public static void writeBytes(final Collection<Byte> bytes, ObjectOutputStream out) throws IOException {
		out.writeByte(bytes.size());
		for (final Byte value : bytes)
			out.writeByte(value.byteValue());
	}
	
	public static <A extends BSerializable> void writeByteMap(final Map<A, Byte> map, final ObjectOutputStream out) throws IOException {
		out.writeByte(map.size());
		for (final Entry<A, Byte> entry : map.entrySet()) {
			final A key  = entry.getKey();
			final Byte value = entry.getValue();
			key.write(out);
			out.writeByte(value.byteValue());
		}
	}
	
	public static void writeServiceMap(final Map<Service, Set<ServiceDistance>> map, final ObjectOutputStream out) throws IOException {
		out.writeByte(map.size());
		for (final Entry<Service, Set<ServiceDistance>> entry : map.entrySet()) {
			final Service key = entry.getKey();
			final Set<ServiceDistance> value = entry.getValue();
			key.write(out);
			SerializationUtils.writeCollection(value, out);
		}
	}
	
	public static void writeParametersMap(final Map<MessageID, Set<Parameter>> map, final ObjectOutputStream out) throws IOException {
		out.writeByte(map.size());
		for (final Entry<MessageID, Set<Parameter>> entry : map.entrySet()) {
			final MessageID key = entry.getKey();
			final Set<Parameter> value = entry.getValue();
			key.write(out);
			SerializationUtils.writeCollection(value, out);
		}
	}
	
	public static void readServiceMap(final Map<Service, Set<ServiceDistance>> map, final ObjectInputStream in) throws IOException {		
		final byte entrySize = in.readByte();
		for (int i = 0; i < entrySize; i++) {
			final Service service = new Service();
			service.read(in);
			final int nSDistances = in.readByte();
			final Set<ServiceDistance> sDistances = new HashSet<ServiceDistance>();
			for (int j = 0; j < nSDistances; j++) {
				final ServiceDistance sDistance = new ServiceDistance();
				sDistance.read(in);
				sDistances.add(sDistance);
			}
			map.put(service, sDistances);
		}
	}
	
	public static void readParametersMap(final Map<MessageID, Set<Parameter>> map, final ObjectInputStream in) throws IOException, UnsupportedTypeException {		
		final byte entrySize = in.readByte();
		for (int i = 0; i < entrySize; i++) {
			final MessageID messageID = new MessageID();
			messageID.read(in);
			final int nParameters = in.readByte();
			final Set<Parameter> parameters = new HashSet<Parameter>();
			for (int j = 0; j < nParameters; j++) {
				final Parameter parameter = Parameter.readParameter(in);
				parameters.add(parameter);
			}
			map.put(messageID, parameters);
		}
	}
	
	public static void readServices(final Collection<Service> services, final ObjectInputStream in) throws IOException {
		final byte sServices = in.readByte();
		for (int i = 0; i < sServices; i++) {
			final Service service = new Service();
			service.read(in);
			services.add(service);
		}
	}
	
	public static void readPeers(final Set<PeerID> peers, final ObjectInputStream in) throws IOException {
		final byte nDestinations = in.readByte();
		for (int i = 0; i < nDestinations; i++) {
			final PeerID peerID = new PeerID();
			peerID.read(in);
			peers.add(peerID);
		}
	}

	public static void readMessageIDs(final Set<MessageID> messageIDs, final ObjectInputStream in) throws IOException {
		final byte size = in.readByte();
		for (int i = 0; i < size; i++) {
			final MessageID messageID = new MessageID();
			messageID.read(in);
			messageIDs.add(messageID);
		}
	}
	
	public static void readServiceDistances(final Set<ServiceDistance> sDistances, final ObjectInputStream in) throws IOException {
		final int size = in.readByte();
		for (int i = 0; i < size; i++) {
			final ServiceDistance sDistance = new ServiceDistance();
			sDistance.read(in);
			sDistances.add(sDistance);
		}
	}
}
