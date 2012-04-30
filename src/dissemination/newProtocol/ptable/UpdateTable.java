package dissemination.newProtocol.ptable;

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

import peer.peerid.PeerID;
import serialization.binary.UnserializationUtils;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;

/**
 * This class represents the updates sent to neighbors.
 * 
 * @author Unai Aguilera (unai.aguilera@gmail.com)
 * 
 */
public class UpdateTable implements Externalizable {

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
	public void add(final UpdateTable updateTable) {
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
	
	private String prettyMap(final Map<Parameter, EstimatedDistance> map, final Taxonomy taxonomy) {
		final StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("{");
		int counter = 0;
		for (final Entry<Parameter, EstimatedDistance> entry : map.entrySet()) {
			if (counter > 0)
				strBuffer.append(" ,");
			strBuffer.append(entry.getKey().pretty(taxonomy) + "=" + entry.getValue());
			counter++;
		}
		strBuffer.append("}");
		return strBuffer.toString();
	}
	
	public String pretty(final Taxonomy taxonomy) {
		return "Deletions: " + prettyMap(deletions, taxonomy) + " Additions: " + prettyMap(additions, taxonomy); 
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		UnserializationUtils.readMap(additions, in);	
		UnserializationUtils.readMap(deletions, in);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(additions.keySet().toArray(new Parameter[0]));
		out.writeObject(additions.values().toArray(new EstimatedDistance[0]));
		
		out.writeObject(deletions.keySet().toArray(new Parameter[0]));
		out.writeObject(deletions.values().toArray(new EstimatedDistance[0]));
	}
	
	private boolean areRelated(final Parameter parameterA, final Parameter parameterB, final Taxonomy taxonomy) {
		return taxonomy.areRelated(parameterA.getID(), parameterB.getID()) && ((parameterA instanceof InputParameter && parameterB instanceof InputParameter) || (parameterA instanceof OutputParameter && parameterB instanceof OutputParameter)); 
	}

	public void merge(final UpdateTable updateTable, final Taxonomy taxonomy) {
		for (final Parameter p : updateTable.deletions.keySet())
			additions.remove(p);
		
		deletions.putAll(updateTable.deletions);
		
		for (final Iterator<Entry<Parameter, EstimatedDistance>> postIterator = updateTable.additions.entrySet().iterator(); postIterator.hasNext(); ) {
			boolean used = false;
			final Entry<Parameter, EstimatedDistance> postAddition = postIterator.next();
			if (additions.containsKey(postAddition.getKey())) {
				if (postAddition.getValue().getDistance() > additions.get(postAddition.getKey()).getDistance())
					additions.put(postAddition.getKey(), postAddition.getValue());
				used = true;
			} else if (!additions.containsKey(postAddition.getKey())) {
				final Map<Parameter, EstimatedDistance> newEntries = new HashMap<Parameter, EstimatedDistance>();
				for (final Iterator<Entry<Parameter, EstimatedDistance>> additionsIterator = additions.entrySet().iterator(); additionsIterator.hasNext(); ) {
					final Entry<Parameter, EstimatedDistance> entry = additionsIterator.next();
					final EstimatedDistance newValue = getNewValue(entry.getValue(), postAddition.getValue());
					if (areRelated(postAddition.getKey(), entry.getKey(), taxonomy)) {
						if (taxonomy.subsumes(postAddition.getKey().getID(), entry.getKey().getID())) {
							additionsIterator.remove();
							newEntries.put(postAddition.getKey(), newValue);
						} else
							newEntries.put(entry.getKey(), newValue);
						used = true;
					}
				}
				additions.putAll(newEntries);
			}
			
			if (used)
				postIterator.remove();
		}
		
		additions.putAll(updateTable.additions);
	}
	
	private EstimatedDistance getNewValue(final EstimatedDistance currentValue, final EstimatedDistance postValue) {
		if (postValue.getDistance() > currentValue.getDistance())
			return postValue;
		return currentValue;
	}
}
