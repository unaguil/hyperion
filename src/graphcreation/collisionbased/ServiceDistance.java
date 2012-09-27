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

package graphcreation.collisionbased;

import graphcreation.services.Service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import serialization.binary.BSerializable;
import serialization.binary.SerializationUtils;

public class ServiceDistance implements BSerializable {

	private final Service service;
	private final byte distance;
	
	public ServiceDistance() {
		service = new Service();
		distance = 0;
	}

	public ServiceDistance(final Service service, final Integer distance) {
		this.service = service;
		this.distance = (byte)distance.intValue();
	}

	public Service getService() {
		return service;
	}

	public Integer getDistance() {
		return new Integer(distance);
	}

	@Override
	public String toString() {
		return service.toString() + "," + distance;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ServiceDistance))
			return false;

		final ServiceDistance serviceDistance = (ServiceDistance) o;
		return this.service.equals(serviceDistance.service);
	}

	@Override
	public int hashCode() {
		return service.hashCode();
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		service.read(in);
		SerializationUtils.setFinalField(ServiceDistance.class, this, "distance", in.readByte());
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		service.write(out);
		out.writeByte(distance);
	}
}
