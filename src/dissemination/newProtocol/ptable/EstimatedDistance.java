package dissemination.newProtocol.ptable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import peer.peerid.PeerID;
import serialization.binary.BSerializable;

/**
 * This class represents an element in the estimated distance list of each
 * parameter contained in the table.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
class EstimatedDistance implements Comparable<EstimatedDistance>, BSerializable {


	// the distance estimated for a parameter
	private byte distance;

	// the neighbor which propagated this distance estimation
	private final PeerID neighbor;

	// tells if the estimated distance is optional. Sometimes the order of
	// messages is different and secondary entries on the list change.
	private final boolean optional;
	
	public EstimatedDistance() {
		neighbor = new PeerID();
		optional = false;
	}

	/**
	 * Constructor of the class.
	 * 
	 * @param distance
	 *            the estimated distance for this element.
	 * @param neighbor
	 *            the neighbor which propagated this distance estimation
	 */
	public EstimatedDistance(final int distance, final PeerID neighbor) {
		this.distance = (byte)distance;
		this.neighbor = neighbor;
		this.optional = false;
	}

	/**
	 * Constructor of the class.
	 * 
	 * @param distance
	 *            the estimated distance for this element.
	 * @param neighbor
	 *            the neighbor which propagated this distance estimation
	 * @param optional
	 *            tells if the entry is optional or not
	 */
	public EstimatedDistance(final int distance, final PeerID neighbor, final boolean optional) {
		this.distance = (byte)distance;
		this.neighbor = neighbor;
		this.optional = optional;
	}

	/**
	 * Gets the distance.
	 * 
	 * @return the distance
	 */
	public int getDistance() {
		return distance;
	}

	/**
	 * Decrements the estimated distance
	 */
	public void decrement() {
		distance--;
	}

	/**
	 * Gets the neighbor which provided this distance.
	 * 
	 * @return the neighbor which provided this distance.
	 */
	public PeerID getNeighbor() {
		return neighbor;
	}

	/**
	 * Tells if the estimated distance is optional
	 * 
	 * @return true if the estimated distance is optional.
	 */
	public boolean isOptional() {
		return optional;
	}

	@Override
	public String toString() {
		return "[D:" + distance + ",N:" + neighbor + "]";
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof EstimatedDistance))
			return false;

		final EstimatedDistance eDistance = (EstimatedDistance) o;
		return this.distance == eDistance.distance && this.neighbor.equals(eDistance.neighbor);
	}

	@Override
	public int hashCode() {
		int result = 17;

		result = result * 31 + distance;
		result = result * 31 + neighbor.hashCode();

		return result;
	}

	@Override
	public int compareTo(final EstimatedDistance eDistance) {
		final int distanceComparison = this.distance - eDistance.distance;

		// Distances are not equal
		if (distanceComparison != 0)
			return distanceComparison;

		// Compare neighbors
		return this.neighbor.compareTo(eDistance.neighbor);
	}

	@Override
	public void read(ObjectInputStream in) throws IOException {
		distance = in.readByte();
		neighbor.read(in);
	}

	@Override
	public void write(ObjectOutputStream out) throws IOException {
		out.writeByte(distance);
		neighbor.write(out);
	}
}