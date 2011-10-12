package multicast.search.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import peer.message.EnvelopeMessage;
import peer.message.PayloadMessage;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;
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
public class SearchMessage extends RemoteMessage implements EnvelopeMessage {

	// this class is used to represent a searched parameter
	private static class ParameterEntry implements Externalizable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5041859241963577106L;

		// the searched parameter
		private final Parameter parameter;

		// the TTL of the search for this parameter
		private final int ttl;

		// the distance to this parameter as specified by the last consulted
		// table
		private final int previousDistance;
		
		@SuppressWarnings("unused")
		public ParameterEntry() {
			parameter = null;
			ttl = 0;
			previousDistance = 0;
		}

		// constructs a entry for a parameter search
		public ParameterEntry(final Parameter p, final int ttl, final int previousDistance) {
			this.parameter = p;
			this.ttl = ttl;
			this.previousDistance = previousDistance;
		}

		// gets the searched parameter
		public Parameter getParameter() {
			return parameter;
		}

		// gets the TTL for this parameter
		public int getTTL() {
			return ttl;
		}

		// gets the previous distance for this parameter
		public int getPreviousDistance() {
			return previousDistance;
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
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			UnserializationUtils.setFinalField(ParameterEntry.class, this, "parameter", in.readObject());
			UnserializationUtils.setFinalField(ParameterEntry.class, this, "ttl", in.readInt());
			UnserializationUtils.setFinalField(ParameterEntry.class, this, "previousDistance", in.readInt());
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(parameter);
			out.writeInt(ttl);
			out.writeInt(previousDistance);
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// payload of the message
	private final PayloadMessage payload;

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
		payload = null;
		searchType = null;
		previousSender = null;
	}

	/**
	 * Constructor of the search message.
	 * 
	 * @param parameters
	 *            the searched parameters
	 * @param payload
	 *            the contained payload
	 * @param source
	 *            the peer which sends the message
	 * @param maxTTL
	 *            the maximum TTL of the message
	 * @param distance
	 *            the distance previously traversed by the message
	 * @param searchType
	 *            the type of the search (exact, generic)
	 */
	public SearchMessage(final Set<Parameter> parameters, final PayloadMessage payload, final PeerID source, final int maxTTL, final int distance, final SearchType searchType) {
		super(source, distance);
		this.payload = payload;
		this.searchType = searchType;
		this.previousSender = PeerID.VOID_PEERID;

		for (final Parameter p : parameters)
			parameterEntries.put(p, new ParameterEntry(p, maxTTL, 0));
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
	public SearchMessage(final SearchMessage searchMessage, final PeerID sender, final int newDistance) {
		super(searchMessage, sender, newDistance);
		this.payload = searchMessage.payload;
		this.searchType = searchMessage.searchType;
		this.previousSender = searchMessage.getSender();

		for (final ParameterEntry pEntry : searchMessage.parameterEntries.values())
			this.parameterEntries.put(pEntry.getParameter(), new ParameterEntry(pEntry.getParameter(), pEntry.getTTL(), pEntry.getPreviousDistance()));
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

	@Override
	public PayloadMessage getPayload() {
		return payload;
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
				parameterEntries.put(parameter, new ParameterEntry(parameter, newTTL, pEntry.getPreviousDistance()));
			else
				parameterEntries.remove(parameter);
		}
	}

	/**
	 * Restores the parameter TTL to the passed value
	 * 
	 * @param parameter
	 *            the parameter whose TTL value is restored
	 * @param ttl
	 *            the value used to restore the TTL of the parameter
	 */
	public void restoreTTL(final Parameter parameter, final int ttl) {
		final ParameterEntry pEntry = parameterEntries.get(parameter);
		if (pEntry != null)
			parameterEntries.put(parameter, new ParameterEntry(parameter, ttl, pEntry.getPreviousDistance()));
	}

	/**
	 * Gets the previous distance for the passed parameter. It is the value of
	 * the distance as seen in the previous visited table.
	 * 
	 * @param parameter
	 *            the parameter whose previous distance is obtained
	 * @return the previous distance for the parameter, 0 if the parameter is
	 *         not searched by this message
	 */
	public int getPreviousDistance(final Parameter parameter) {
		final ParameterEntry pEntry = parameterEntries.get(parameter);
		if (pEntry != null)
			return pEntry.getPreviousDistance();

		return 0;
	}

	/**
	 * Sets the parameter new distance. If the parameter does not exist in the
	 * search message the value is ignored
	 * 
	 * @param parameter
	 *            the parameter whose distance is set
	 * @param newDistance
	 *            the new distance for the parameter
	 */
	public void setCurrentDistance(final Parameter parameter, final int newDistance) {
		final ParameterEntry pEntry = parameterEntries.get(parameter);
		if (pEntry != null)
			parameterEntries.put(parameter, new ParameterEntry(parameter, pEntry.getTTL(), newDistance));
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
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		UnserializationUtils.readMap(parameterEntries, in);
		
		SearchType type = SearchType.valueOf(in.readUTF());
		UnserializationUtils.setFinalField(SearchMessage.class, this, "searchType", type);
		UnserializationUtils.setFinalField(SearchMessage.class, this, "previousSender", in.readObject());
		UnserializationUtils.setFinalField(SearchMessage.class, this, "payload", in.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		out.writeObject(parameterEntries.keySet().toArray(new Parameter[0]));
		out.writeObject(parameterEntries.values().toArray(new ParameterEntry[0]));
		
		out.writeUTF(searchType.toString());
		out.writeObject(previousSender);
		out.writeObject(payload);
	}
}
