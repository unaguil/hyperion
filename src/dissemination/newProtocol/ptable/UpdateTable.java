package dissemination.newProtocol.ptable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import peer.PeerID;
import taxonomy.parameter.Parameter;

/**
 * This class represents the updates sent to neighbors.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class UpdateTable implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Map<Parameter, EstimatedDistance> deletions = new HashMap<Parameter, EstimatedDistance>();
	private final Map<Parameter, EstimatedDistance> additions = new HashMap<Parameter, EstimatedDistance>();

	/**
	 * Constructor of the class which creates an empty update table
	 */
	public UpdateTable() {
	}

	/**
	 * Tells if the table is empty
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return deletions.isEmpty() && additions.isEmpty();
	}

	/**
	 * Sets the delete for the passed parameter
	 * 
	 * @param p
	 *            the parameter which the deletion refers to
	 * @param peer
	 *            the peer whose entry needs to be delete
	 */
	public void setDelete(final Parameter p, final PeerID peer) {
		deletions.put(p, new EstimatedDistance(0, peer));
	}

	/**
	 * Sets the addition for the passed parameter
	 * 
	 * @param p
	 *            the parameter which the deletion refers to
	 * @param peer
	 *            the peer where the information comes from
	 */
	public void setAddition(final Parameter p, final int distance, final PeerID peer) {
		additions.put(p, new EstimatedDistance(distance, peer));
	}

	/**
	 * Gets the deletion for the passed parameter.
	 * 
	 * @param the
	 *            parameter whose deletion is obtained
	 * @return the deletion for the passed parameter, null if does not exist
	 */
	public EstimatedDistance getDeletion(final Parameter p) {
		return deletions.get(p);
	}

	/**
	 * Gets the addition for the passed parameter.
	 * 
	 * @param the
	 *            parameter whose addition is obtained
	 * @return the addition for the passed parameter, null if does not exist
	 */
	public EstimatedDistance getAddition(final Parameter p) {
		return additions.get(p);
	}

	/**
	 * Removes the entries for the specified parameter
	 * 
	 * @param the
	 *            parameter to remove
	 */
	public void removeParameter(final Parameter p) {
		deletions.remove(p);
		additions.remove(p);
	}

	/**
	 * Merges the current table with the passed one. Entries are copied.
	 * 
	 * @param the
	 *            update table to merge
	 */
	public void merge(final UpdateTable updateTable) {
		deletions.putAll(updateTable.deletions);
		additions.putAll(updateTable.additions);
	}

	/**
	 * Gets all the parameters with updates.
	 * 
	 * @param the
	 *            parameters with updates
	 */
	public Set<Parameter> getParameters() {
		final Set<Parameter> parameters = new HashSet<Parameter>();
		parameters.addAll(deletions.keySet());
		parameters.addAll(additions.keySet());
		return parameters;
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof UpdateTable))
			return false;

		final UpdateTable updateTable = (UpdateTable) o;

		return this.deletions.equals(updateTable.deletions) && this.additions.equals(updateTable.additions);
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = result * 31 + deletions.hashCode();
		result = result * 31 + additions.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Deletions: " + deletions.toString() + " Additions: " + additions.toString();
	}
}
