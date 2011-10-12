package graphcreation.services;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

public class Service implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String id;

	private final PeerID peer;

	private final Set<Parameter> params = new HashSet<Parameter>();
	
	public Service() {
		id = null;
		peer = null;
	}

	public Service(final String id, final PeerID peer) {
		this.id = id;
		this.peer = peer;
	}

	public void addParameter(final Parameter p) {
		params.add(p);
	}

	public Set<InputParameter> getInputParams() {
		final Set<InputParameter> inputParams = new HashSet<InputParameter>();
		for (final Parameter p : params)
			if (p instanceof InputParameter)
				inputParams.add((InputParameter) p);
		return inputParams;
	}

	public Set<OutputParameter> getOutputParams() {
		final Set<OutputParameter> outputParams = new HashSet<OutputParameter>();
		for (final Parameter p : params)
			if (p instanceof OutputParameter)
				outputParams.add((OutputParameter) p);
		return outputParams;
	}

	public Set<Parameter> getParameters() {
		return params;
	}

	public PeerID getPeerID() {
		return peer;
	}

	public String getID() {
		return id + ":" + peer;
	}

	public String getName() {
		return id;
	}

	@Override
	public String toString() {
		return getID();
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 37 * result + id.hashCode();
		result = 37 * result + peer.hashCode();
		return result;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Service))
			return false;

		final Service s = (Service) o;
		return this.id.equals(s.id) && this.peer.equals(s.peer);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(Service.class, this, "id", in.readUTF());
		UnserializationUtils.setFinalField(Service.class, this, "peer", in.readObject());
		params.addAll(Arrays.asList((Parameter[])in.readObject()));
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(id);
		out.writeObject(peer);
		out.writeObject(params.toArray(new Parameter[0]));
	}
}
