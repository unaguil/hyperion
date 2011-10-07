package dissemination.newProtocol.ptable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import peer.peerid.PeerID;

/**
 * This class contains the list of estimated distances for a parameter. It does
 * not contain duplicates.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
class EstimatedDistanceList implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// the list if estimated distances implemented through.
	private final List<EstimatedDistance> estimatedDistances = new ArrayList<EstimatedDistance>();

	/**
	 * Creates an empty estimated distances list.
	 */
	public EstimatedDistanceList() {
	}

	/**
	 * Creates a copy of the passed distance list. It is NOT a deep copy.
	 * 
	 * @param eDistanceList
	 *            the list to copy.
	 */
	public EstimatedDistanceList(final EstimatedDistanceList eDistanceList) {
		this.estimatedDistances.addAll(eDistanceList.estimatedDistances);
	}

	/**
	 * Updates the list with the passed estimated distance. If an entry with the
	 * same neighbor that the passed one already exists it is substituted. If
	 * the passed entry has the value 0 for distance means to remove the entry
	 * with the same neighbor from the list.
	 * 
	 * @param eDistance
	 *            the estimated distance to insert.
	 * @returns the previous value for the passed neighbor if it existed, null
	 *          otherwise.
	 */
	public EstimatedDistance updateEstimatedDistance(final EstimatedDistance eDistance) {
		// Check if a value from the same neighbor exists
		final EstimatedDistance previousEstimatedDistance = search(eDistance.getNeighbor());

		// If was previously inserted do nothing and return false
		if (previousEstimatedDistance != null && previousEstimatedDistance.equals(eDistance))
			return previousEstimatedDistance;

		// if there was another estimated value from the same neighbor remove it
		// and add the new one
		if (previousEstimatedDistance != null)
			estimatedDistances.remove(previousEstimatedDistance);

		estimatedDistances.add(eDistance);

		// Sort list
		Collections.sort(estimatedDistances);

		return previousEstimatedDistance;
	}

	/**
	 * Removes values with the specified neighbor
	 * 
	 * @param neighbor
	 *            the neighbor whose estimated distances are removed
	 * @returns the previous removed value, or null if nothing was removed
	 */
	public EstimatedDistance removeEstimatedDistanceFrom(final PeerID neighbor) {
		final EstimatedDistance previousEstimatedDistance = search(neighbor);

		if (previousEstimatedDistance != null) {
			estimatedDistances.remove(previousEstimatedDistance);
			return previousEstimatedDistance;
		}

		return null;
	}

	/**
	 * Gets the effective estimated distance (the element with the greater
	 * distance).
	 * 
	 * @return the effective estimated distance. Null if the list is empty.
	 */
	public EstimatedDistance getEffectiveDistance() {
		if (estimatedDistances.isEmpty())
			return null;

		return estimatedDistances.get(estimatedDistances.size() - 1);
	}

	/**
	 * Tells if the estimated distance list is empty.
	 * 
	 * @return true if empty, false otherwise.
	 */
	public boolean isEmpty() {
		return estimatedDistances.isEmpty();
	}

	/**
	 * Gets a list which is a copy of the contains of the estimated distance
	 * list
	 * 
	 * @return a copy of the estimated distance list
	 */
	public List<EstimatedDistance> getList() {
		return new ArrayList<EstimatedDistance>(estimatedDistances);
	}

	/**
	 * Decrements the distances of all elements contained in the list
	 */
	public void decEstimatedDistances() {
		for (final EstimatedDistance eDistance : estimatedDistances)
			eDistance.decrement();
	}

	/**
	 * Gets the estimated distance which was obtained from the passed neighbor
	 * 
	 * @param neighbor
	 *            the neighbor the estimated distance was obtained from
	 * @return the estimated distance of the passes neighbor, null if does not
	 *         exist
	 */
	public EstimatedDistance search(final PeerID neighbor) {
		for (final EstimatedDistance eDistance : estimatedDistances)
			if (eDistance.getNeighbor().equals(neighbor))
				return eDistance;

		return null;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof EstimatedDistanceList))
			return false;

		final EstimatedDistanceList list = (EstimatedDistanceList) o;

		return this.equalOptional(list) && list.equalOptional(this);
	}

	private boolean equalOptional(final EstimatedDistanceList list) {
		for (final EstimatedDistance eDistance : estimatedDistances)
			if (!list.estimatedDistances.contains(eDistance) && !eDistance.isOptional())
				return false;

		return true;
	}

	@Override
	public int hashCode() {
		return estimatedDistances.hashCode();
	}

	@Override
	public String toString() {
		return estimatedDistances.toString();
	}

	public int getOptionalEntriesSize() {
		int counter = 0;
		for (final EstimatedDistance eDistance : estimatedDistances)
			if (eDistance.isOptional())
				counter++;
		return counter;
	}

	public int size() {
		return estimatedDistances.size();
	}
}
