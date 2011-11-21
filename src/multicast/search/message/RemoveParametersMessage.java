package multicast.search.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import peer.message.MessageID;
import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;
import taxonomy.parameter.Parameter;

/**
 * This class defines a message used to remove a parameter route.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class RemoveParametersMessage extends RemoteMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<MessageID, Set<Parameter>> removedParameters = new HashMap<MessageID, Set<Parameter>>();

	public RemoveParametersMessage() {
		
	}
	
	/**
	 * Constructor of the class.
	 * 
	 * @param parameters
	 *            the removed parameters
	 * @param routeIDs
	 *            the identifier of the routes parameters are removed from
	 * @param source
	 *            the source of the message
	 */
	public RemoveParametersMessage(final Map<MessageID, Set<Parameter>> removedParameters, final PeerID source) {
		super(source);
		this.removedParameters.putAll(removedParameters);
	}

	/**
	 * Constructor of the message using another message as base.
	 * 
	 * @param removeParamRouteMessage
	 *            the message to use as base
	 * @param sender
	 *            the new sender of the message
	 * @param through
	 *            the neighbor used to send the message
	 * @param newDistance
	 *            the new distance for this message
	 */
	public RemoveParametersMessage(final RemoveParametersMessage removeParamRouteMessage, final PeerID sender, final int newDistance) {
		super(removeParamRouteMessage, sender, newDistance);
		this.removedParameters.putAll(removeParamRouteMessage.removedParameters);
	}

	public Map<MessageID, Set<Parameter>> getRemovedParameters() {
		return removedParameters;
	}

	@Override
	public String toString() {
		return super.toString() + " removedParameters: " + removedParameters;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		
		readMap(removedParameters, in);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		
		writeMap(removedParameters, out);
	}
	
	private void writeMap(Map<MessageID, Set<Parameter>> map, ObjectOutput out) throws IOException {
		out.writeObject(map.keySet().toArray(new MessageID[0]));
		out.writeInt(map.values().size());
		for (Set<Parameter> set : map.values())
			out.writeObject(set.toArray(new Parameter[0]));
	}
	
	private void readMap(Map<MessageID, Set<Parameter>> map, ObjectInput in) throws ClassNotFoundException, IOException {
		List<MessageID> keys = Arrays.asList((MessageID[])in.readObject());
		int size = in.readInt();
		List<Set<Parameter>> values = new ArrayList<Set<Parameter>>();
		for (int i = 0; i < size; i++) {
			Set<Parameter> value = new HashSet<Parameter>(Arrays.asList((Parameter[])in.readObject()));
			values.add(value);
		}
		
		UnserializationUtils.fillMap(map, keys, values);
	}
}
