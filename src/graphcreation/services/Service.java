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

package graphcreation.services;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import peer.message.UnsupportedTypeException;
import peer.peerid.PeerID;
import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

public class Service implements BSerializable {
	
	private final String id;
	private final PeerID peer;

	private final Set<Parameter> params = new HashSet<Parameter>();
	
	public Service() {
		id = new String();
		peer = new PeerID();
	}

	public Service(final String id, final PeerID peer) {
		this.id = id;
		this.peer = peer;
	}
	
	public boolean isLocal(final PeerID peerID) {
		return getPeerID().equals(peerID);
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
	public void read(ObjectInputStream in) throws IOException {
		SerializationUtils.setFinalField(Service.class, this, "id", in.readUTF());
		peer.read(in);
		
		try {
			final byte nParameters = in.readByte();
			for (int i = 0; i < nParameters; i++) {
				final Parameter p = Parameter.readParameter(in);
				params.add(p);
			}		
		} catch (UnsupportedTypeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		out.writeUTF(id);
		peer.write(out);
		SerializationUtils.writeCollection(params, out);
	}
}
