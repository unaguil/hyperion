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

package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import multicast.SearchedParameter;
import peer.message.BroadcastMessage;
import peer.message.MessageTypes;
import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;
import taxonomy.Taxonomy;
import taxonomy.parameter.Parameter;

/**
 * This class defines a parameter search message. This message can search
 * various parameters simultaneously Each searched parameter is treated
 * separately meaning that it has its own TTL. It can contain another message as
 * payload.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class SearchMessage extends RemoteMessage {

	// this class is used to represent a searched parameter
	private static class ParameterEntry implements BSerializable {

		// the searched parameter
		private final Parameter parameter;

		// the TTL of the search for this parameter
		private final byte ttl;
		
		public ParameterEntry() {
			parameter = null;
			ttl = 0;
		}

		// constructs a entry for a parameter search
		public ParameterEntry(final Parameter p, final int ttl) {
			this.parameter = p;
			this.ttl = (byte)ttl;
		}

		// gets the searched parameter
		public Parameter getParameter() {
			return parameter;
		}

		// gets the TTL for this parameter
		public int getTTL() {
			return ttl;
		}

		@Override
		public boolean equals(final Object o) {
			if (!(o instanceof ParameterEntry))
				return false;

			final ParameterEntry pEntry = (ParameterEntry) o;
			return this.parameter.equals(pEntry.parameter);
		}

		@Override
		public int hashCode() {
			return parameter.hashCode();
		}

		@Override
		public void read(ObjectInputStream in) throws IOException {
			try {
				final Parameter p = Parameter.readParameter(in);
				SerializationUtils.setFinalField(ParameterEntry.class, this, "parameter", p);
				SerializationUtils.setFinalField(ParameterEntry.class, this, "ttl", in.readByte());
			} catch (UnsupportedTypeException e) {
				throw new IOException(e);
			}
		}

		@Override
		public void write(ObjectOutputStream out) throws IOException {
			parameter.write(out);
			out.writeByte(ttl);
		}
	}

	// payload of the message
	private final BroadcastMessage payload;

	// searched parameters
	private final Map<Parameter, ParameterEntry> parameterEntries = new HashMap<Parameter, ParameterEntry>(); 

	// the type of this search
	private final SearchType searchType;

	public enum SearchType {
		Exact, Generic
	}

	// the previous sender of the search message
	private final PeerID previousSender;
	
	public SearchMessage() {
		super(MessageTypes.SEARCH_MESSAGE);
		payload = null;
		searchType = SearchType.Exact;
		previousSender = new PeerID();
	}
	
	public SearchMessage(final PeerID source, final Set<PeerID> expectedDestinations, final Set<SearchedParameter> searchedParameters, final BroadcastMessage payload, final int distance, final SearchType searchType) {
		super(MessageTypes.SEARCH_MESSAGE, source, payload, expectedDestinations, distance);
		this.payload = payload;
		this.searchType = searchType;
		this.previousSender = PeerID.VOID_PEERID;

		for (final SearchedParameter searchedParameter : searchedParameters)
			parameterEntries.put(searchedParameter.getParameter(), new ParameterEntry(searchedParameter.getParameter(), searchedParameter.getMaxTTL()));
	}

	/**
	 * Constructor of the search message. It uses another search message as
	 * base.
	 * 
	 * @param sender
	 *            the new sender of the message
	 * @param respondingTo
	 *            the message this one responds to
	 */
	public SearchMessage(final SearchMessage searchMessage, final PeerID sender, final Set<PeerID> expectedDestinations, final int newDistance) {
		super(searchMessage, sender, expectedDestinations, newDistance);
		this.payload = searchMessage.payload;
		this.searchType = searchMessage.searchType;
		this.previousSender = searchMessage.getSender();

		for (final ParameterEntry pEntry : searchMessage.parameterEntries.values())
			this.parameterEntries.put(pEntry.getParameter(), new ParameterEntry(pEntry.getParameter(), pEntry.getTTL()));
	}

	/**
	 * Gets the previous sender of this search message
	 * 
	 * @return the previous sender of this message
	 */
	public PeerID getPreviousSender() {
		return previousSender;
	}

	/**
	 * Gets the searched parameters
	 * 
	 * @return a set containing the searched parameters
	 */
	public Set<Parameter> getSearchedParameters() {
		return new HashSet<Parameter>(parameterEntries.keySet());
	}

	/**
	 * Gets the parameter TTL.
	 * 
	 * @param parameter
	 *            the parameter whose TTL is gotten of
	 * @return the TTL of the passed parameter, 0 if the parameter is not
	 *         searched by this message
	 */
	public int getTTL(final Parameter parameter) {
		final ParameterEntry pEntry = parameterEntries.get(parameter);
		if (pEntry != null)
			return pEntry.getTTL();

		return 0;
	}

	/**
	 * Decrements the TTL associated to a particular parameter. Those parameters
	 * which have a TTL value of zero after decrementing are removed
	 * 
	 * @param parameter
	 *            the parameter whose TTL is decremented
	 */
	public void decTTL(final Parameter parameter) {
		final ParameterEntry pEntry = parameterEntries.get(parameter);
		if (pEntry != null) {
			final int newTTL = pEntry.getTTL() - 1;
			if (newTTL > 0)
				parameterEntries.put(parameter, new ParameterEntry(parameter, newTTL));
			else
				parameterEntries.remove(parameter);
		}
	}

	/**
	 * Checks if the message has a global TTL grater than 0. At least one
	 * parameter has a TTL greater than 0.
	 * 
	 * @return true if at least one parameter has a TTL greater than 0, false
	 *         otherwise
	 */
	public boolean hasTTL() {
		removeZeroTTL();
		return parameterEntries.size() > 0;
	}

	/**
	 * Gets the type of the search
	 * 
	 * @return the type of the search
	 */
	public SearchType getSearchType() {
		return searchType;
	}

	@Override
	public String toString() {
		return super.toString() + " (F:" + getSource() + " P:" + getSearchedParameters() + ")";
	}

	@Override
	public boolean hasPayload() {
		return payload != null;
	}

	public Set<Parameter> removeParameters(final Set<Parameter> parameters) {
		final Set<Parameter> removedParameters = new HashSet<Parameter>();
		for (final Parameter p : parameters)
			if (parameterEntries.remove(p) != null)
				removedParameters.add(p);
		return removedParameters;
	}

	public Map<Parameter, Parameter> generalizeParameters(final Set<Parameter> generalizations, final Taxonomy taxonomy) {
		final Map<Parameter, Parameter> generalizedParameters = new HashMap<Parameter, Parameter>();
		if (getSearchType().equals(SearchType.Generic))
			for (final Parameter generalization : generalizations)
				for (final Parameter p : parameterEntries.keySet())
					// Exclude equality
					if (taxonomy.areRelated(generalization.getID(), p.getID()) && !generalization.equals(p))
						generalizedParameters.put(p, generalization);

		for (final Entry<Parameter, Parameter> entry : generalizedParameters.entrySet()) {
			// Change parameter for generalization
			final ParameterEntry pEntry = parameterEntries.remove(entry.getKey());
			parameterEntries.put(entry.getValue(), pEntry);
		}
		return generalizedParameters;
	}

	// all entries which have a TTL of 0 are removed
	private void removeZeroTTL() {
		for (final Iterator<Parameter> it = parameterEntries.keySet().iterator(); it.hasNext();) {
			final Parameter parameter = it.next();
			if (parameterEntries.get(parameter).getTTL() == 0)
				it.remove();
		}
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		super.read(in);
		
		try {
			final byte nEntries = in.readByte();
			for (int i = 0; i < nEntries; i++) {
				final Parameter p = Parameter.readParameter(in);
				final ParameterEntry pEntry = new ParameterEntry();
				pEntry.read(in);
				parameterEntries.put(p, pEntry);
			}
		} catch (UnsupportedTypeException e) {
			throw new IOException(e);
		}
		
		SearchType type = SearchType.values()[in.readByte()];
		SerializationUtils.setFinalField(SearchMessage.class, this, "searchType", type);
		previousSender.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		super.write(out);
		
		SerializationUtils.<Parameter, ParameterEntry>writeMap(parameterEntries, out);
		
		out.writeByte(searchType.ordinal());
		previousSender.write(out);
	}

	public void removeParameter(Parameter p) {
		parameterEntries.remove(p);
	}

	@Override
	public BroadcastMessage copy() {
		return new SearchMessage(this, getSender(), getExpectedDestinations(), getDistance());
	}
}
