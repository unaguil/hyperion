package graphcreation.collisionbased;

import graphcreation.services.Service;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import serialization.binary.UnserializationUtils;

public class ServiceDistance implements Externalizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Service service;
	private final byte distance;
	
	public ServiceDistance() {
		service = null;
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
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.setFinalField(ServiceDistance.class, this, "service", in.readObject());
		UnserializationUtils.setFinalField(ServiceDistance.class, this, "distance", in.readByte());
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(service);
		out.writeByte(distance);
	}
}
