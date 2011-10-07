package graphcreation.collisionbased;

import graphcreation.services.Service;

import java.io.Serializable;

public class ServiceDistance implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Service service;
	private final Integer distance;

	public ServiceDistance(final Service service, final Integer distance) {
		this.service = service;
		this.distance = distance;
	}

	public Service getService() {
		return service;
	}

	public Integer getDistance() {
		return distance;
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
}
