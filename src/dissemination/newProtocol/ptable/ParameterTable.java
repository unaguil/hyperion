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

package dissemination.newProtocol.ptable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import peer.peerid.PeerID;
import serialization.xml.XMLSerializable;
import taxonomy.Taxonomy;
import taxonomy.parameter.InputParameter;
import taxonomy.parameter.InvalidParameterIDException;
import taxonomy.parameter.OutputParameter;
import taxonomy.parameter.Parameter;
import taxonomy.parameter.ParameterFactory;
import util.logger.Logger;

public class ParameterTable implements XMLSerializable {

	private final static String PARAMETER_TABLE = "parameterTable";
	private final static String PARAMETER = "parameter";
	private final static String PARAMETER_ENTRY = "entry";
	private final static String PARAMETER_ID_ATTRIB = "id";
	private final static String PARAMETER_DISTANCE_ATTRIB = "distance";
	private final static String PARAMETER_NEIGHBOR_ATTRIB = "neighbor";
	private final static String OPTIONAL_ENTRY = "optional";

	// map which contains the parameter table
	private final Map<ParameterGroup, EstimatedDistanceList> table = new HashMap<ParameterGroup, EstimatedDistanceList>();

	// local parameters
	private final Set<Parameter> localParameters = new HashSet<Parameter>();

	// the maximum dissemination distance
	private final DisseminationDistanceInfo disseminationInfo;

	// the peer hosting this parameter table
	private final PeerID host;

	// the taxonomy used for parameter grouping
	private final Taxonomy taxonomy;

	private final Logger logger = Logger.getLogger(ParameterTable.class);

	/**
	 * Constructs an empty parameter table
	 * 
	 * @param dDistance
	 *            is the maximum dissemination distance
	 * @param host
	 *            is the peer hosting this parameter table.
	 * @param taxonomy
	 *            the taxonomy used for parameter grouping
	 */
	public ParameterTable(final DisseminationDistanceInfo disseminationInfo, final PeerID host, final Taxonomy taxonomy) {
		this.disseminationInfo = disseminationInfo;
		this.host = host;
		this.taxonomy = taxonomy;
	}

	// finds the group to which this parameter belongs to
	private ParameterGroup findGroup(final Parameter p) {
		for (final ParameterGroup pGroup : table.keySet())
			if (pGroup.belongs(p))
				return pGroup;
		return null;
	}

	/**
	 * Adds a set of parameters as local parameters.
	 * 
	 * @param parameters
	 *            parameters to add to the parameter table as local parameter
	 * @return an update table containing all modifications
	 */
	public UpdateTable addLocalParameters(final Set<Parameter> parameters) {
		final UpdateTable updateTable = new UpdateTable();

		for (final Parameter p : parameters) {
			insertParameter(host, updateTable, p, new EstimatedDistance(disseminationInfo.getMaxDistance(), host), true);

			localParameters.add(p);
		}

		logger.debug("Peer " + host + " added local parameters: " + parameters);

		return updateTable;
	}

	private EstimatedDistanceList updateTable(final Parameter p) {
		EstimatedDistanceList list;
		// Obtain or create the estimated list for parameter p
		final ParameterGroup relatedGroup = findGroup(p);
		if (relatedGroup != null) {
			// Remove from the map
			list = table.remove(relatedGroup);
			// Add the new parameter to the group
			relatedGroup.add(p);
			// Reinsert the parameter group again
			table.put(relatedGroup, list);
		} else {
			list = new EstimatedDistanceList();
			final ParameterGroup pGroup = new ParameterGroup(p, taxonomy);
			table.put(pGroup, list);
		}
		return list;
	}

	/**
	 * Removes a set of local parameters
	 * 
	 * @param parameters
	 *            parameters to be removed
	 * @return an update table containing all modifications
	 */
	public UpdateTable removeLocalParameters(final Set<Parameter> parameters) {
		final UpdateTable updateTable = new UpdateTable();

		for (final Parameter p : parameters) {
			final ParameterGroup relatedGroup = findGroup(p);
			localParameters.remove(p);
			if (relatedGroup != null) {
				final EstimatedDistanceList list = table.get(relatedGroup);
				// Remove all parameters which are originated in the host node
				// only if the removed parameter was the last parameter of the
				// group
				if (localGroupCount(p) == 0) {
					final EstimatedDistance previousEstimatedDistance = list.removeEstimatedDistanceFrom(host);
					// if entries were removed
					if (previousEstimatedDistance != null)
						updateTable.setDelete(p, host);

					// If the effective distance is not 0 notify neighbors
					if (getEstimatedDistance(p) != 0)
						updateTable.setAddition(p, getEffectiveDistance(p).getDistance(), getEffectiveDistance(p).getNeighbor());
				}
			}
		}

		logger.debug("Peer " + host + " removed local parameters: " + parameters);

		// Clean empty entries
		cleanTable();

		return updateTable;
	}

	// gets the number of local parameters which belong to the group of the
	// passed parameter
	private int localGroupCount(final Parameter p) {
		int counter = 0;
		for (final Parameter localParameter : localParameters)
			// check that the parameter has the same type that the current one
			// (input/output)
			if ((isInput(localParameter) && isInput(p)) || (isOutput(localParameter) && isOutput(p)))
				if (taxonomy.areRelated(localParameter.getID(), p.getID()))
					counter++;
		return counter;
	}

	private boolean isInput(final Parameter p) {
		return p instanceof InputParameter;
	}

	private boolean isOutput(final Parameter p) {
		return p instanceof OutputParameter;
	}

	/**
	 * Gets the table which is sent to new detected neighbors. It is a table
	 * which contains those parameters which a effective distance greater than
	 * 1.
	 * 
	 * @return the update table for neighbors
	 */
	public UpdateTable getNewNeighborTable() {
		final UpdateTable updateTable = new UpdateTable();

		for (final ParameterGroup pGroup : table.keySet()) {
			final EstimatedDistance effectiveDistance = table.get(pGroup).getEffectiveDistance();

			// Check if effective distance is greater than 1.
			if (effectiveDistance.getDistance() > 1)
				updateTable.setAddition(pGroup.getCurrentParameter(), effectiveDistance.getDistance(), effectiveDistance.getNeighbor());
		}

		return updateTable;
	}

	/**
	 * Gets the estimated distance for the specified parameter. Parameters which
	 * are not located in the table get a value of 0.
	 * 
	 * @param p
	 *            the parameter whose distance is obtained
	 * @param distance
	 *            the estimated distance of the parameter
	 */
	public int getEstimatedDistance(final Parameter p) {
		final ParameterGroup pGroup = findGroup(p);
		if (table.containsKey(pGroup) && !table.get(pGroup).isEmpty())
			return table.get(pGroup).getEffectiveDistance().getDistance();

		return 0;
	}

	/**
	 * Gets the distance for passed parameter which comes from the specified
	 * neighbor
	 * 
	 * @param p
	 *            the parameter whose distance is obtained
	 * @param neighbor
	 *            the neighbor whose distance is obtained
	 * @return the distance to parameter according to the specified neighbor
	 */
	public int getDistance(final Parameter p, final PeerID neighbor) {
		final ParameterGroup pGroup = findGroup(p);
		if (table.containsKey(pGroup))
			for (final EstimatedDistance eDistance : table.get(pGroup).getList())
				if (eDistance.getNeighbor().equals(neighbor))
					return eDistance.getDistance();
		return 0;
	}

	/**
	 * Gets the set of parameters contained in the table.
	 * 
	 * @return the parameters contained in the table.
	 */
	public Set<Parameter> getParameters() {
		final Set<Parameter> parameters = new HashSet<Parameter>();
		for (final ParameterGroup pGroup : table.keySet())
			parameters.add(pGroup.getCurrentParameter());
		return parameters;
	}

	/**
	 * Gets parameters which have entries coming from the passed neighbor
	 * 
	 * @param the
	 *            neighbor whose parameters are get
	 * @return the set of parameters
	 */
	public Set<Parameter> getParameters(final PeerID neighbor) {
		final Set<Parameter> parameters = new HashSet<Parameter>();
		for (final ParameterGroup pGroup : table.keySet()) {
			final EstimatedDistanceList list = table.get(pGroup);
			if (list.search(neighbor) != null)
				parameters.add(pGroup.getCurrentParameter());
		}

		return parameters;
	}

	/**
	 * Gets the set of local parameters.
	 * 
	 * @return the set of local parameters.
	 */
	public Set<Parameter> getLocalParameters() {
		return new HashSet<Parameter>(localParameters);
	}

	/**
	 * Tells if a parameter is a local parameter.
	 * 
	 * @param p
	 *            the parameter to check
	 * @return true if the parameter is local, false otherwise
	 */
	public boolean isLocalParameter(final Parameter p) {
		return localParameters.contains(p);
	}

	/**
	 * Checks if the specified parameter subsumes a local parameter
	 * 
	 * @param parameter
	 *            the checked parameter
	 * @return the set of subsumes parameters
	 */
	public Set<Parameter> subsumesLocalParameter(final Parameter p) {
		final Set<Parameter> subsumedParameters = new HashSet<Parameter>();
		for (final Parameter localParameter : localParameters)
			if (taxonomy.subsumes(p.getID(), localParameter.getID()))
				subsumedParameters.add(localParameter);
		return subsumedParameters;
	}

	/**
	 * Decrements all estimated distances in one unit.
	 */
	public void decEstimatedDistances() {
		for (final EstimatedDistanceList list : table.values())
			list.decEstimatedDistances();
	}

	/**
	 * Checks if the parameter table is empty.
	 * 
	 * @return true if table is empty, false otherwise
	 */
	public boolean isEmpty() {
		return table.isEmpty();
	}

	public class UpdateResult {
		private final UpdateTable updateTable;
		private final Set<Parameter> addedEParameters = new HashSet<Parameter>();
	
		public UpdateResult(final UpdateTable updateTable, final Set<Parameter> addedEntries) {
			this.updateTable = updateTable;
			this.addedEParameters.addAll(addedEntries);
		}

		public UpdateTable getUpdateTable() {
			return updateTable;
		}

		public Set<Parameter> getAddedParameters() {
			return addedEParameters;
		}
	}
	
	public UpdateResult updateTable(final UpdateTable updates, final PeerID neighbor) {
		final UpdateTable updateTable = new UpdateTable();
		final Set<Parameter> addedParameters = new HashSet<Parameter>();

		// Add each parameter to the current table
		for (final Parameter p : updates.getParameters()) {
			// Save the current status of the estimated distance list
			final EstimatedDistanceList currentList = getEffectiveDistanceList(p);
			EstimatedDistanceList listBackup;
			if (currentList != null)
				listBackup = new EstimatedDistanceList(getEffectiveDistanceList(p));
			else
				listBackup = new EstimatedDistanceList();

			// Get elements
			final EstimatedDistance delete = updates.getDeletion(p);
			final EstimatedDistance insert = updates.getAddition(p);

			// First, process the deletion element, if it exists
			if (delete != null) {
				// Delete those elements coming from the specified neighbor from
				// the local table
				final int effectiveDistance = getEstimatedDistance(p);
				// Check that the parameter exists in the local list
				if (effectiveDistance > 0) {
					final ParameterGroup pGroup = findGroup(p);
					final EstimatedDistanceList localList = getEffectiveDistanceList(p);
					// Remove entry coming specified neighbor
					final EstimatedDistance removedDistance = localList.removeEstimatedDistanceFrom(delete.getNeighbor());

					// Remove parameter p if its list is empty
					if (getEffectiveDistanceList(p).isEmpty())
						table.remove(pGroup);

					// Check if an elements was eliminated
					if (removedDistance != null) {
						// If the removed element has a distance greater than
						// one generate a delete message for neighbors (0, N)
						if (removedDistance.getDistance() > 1)
							updateTable.setDelete(pGroup.getCurrentParameter(), host);

						// Generate an addition if current effective distance is
						// greater than one
						if (getEstimatedDistance(p) > 1)
							updateTable.setAddition(pGroup.getCurrentParameter(), getEffectiveDistance(p).getDistance(), getEffectiveDistance(p).getNeighbor());
					}
				}
			}
			
			boolean parameterChanged = false;
			// Secondly, process the insertion.
			// Insertion is only processed if information does not
			// originally comes from the current node
			if (insert != null && !insert.getNeighbor().equals(host))
				parameterChanged = insertParameter(neighbor, updateTable, p, insert, false);

			// If the estimated distance list has not changed remove current
			// parameter from the updateTable
			final ParameterGroup pGroup = findGroup(p);
			if (pGroup != null) {
				if (listBackup.equals(getEffectiveDistanceList(p)) && !parameterChanged) {
					updateTable.removeParameter(pGroup.getCurrentParameter());
				} else {
					//only if is not shorter -> not elements were removed
					if (getEffectiveDistanceList(p).getList().size() >= listBackup.size())
						addedParameters.add(pGroup.getCurrentParameter());
				}
			}
		}

		return new UpdateResult(updateTable, addedParameters);
	}

	private boolean insertParameter(final PeerID neighbor, final UpdateTable updateTable, final Parameter p, final EstimatedDistance insert, final boolean local) {
		final int effectiveDistance = getEstimatedDistance(p);

		// Insert the received distance decrementing its value and using the id
		// of the node which sent the information
		boolean parameterChanged = false;
		if (local)
			parameterChanged = addEntry(p, new EstimatedDistance(insert.getDistance(), neighbor));
		else
			parameterChanged = addEntry(p, new EstimatedDistance(insert.getDistance() - 1, neighbor));

		if ((getEstimatedDistance(p) != effectiveDistance && getEstimatedDistance(p) > 1) || (parameterChanged && getEstimatedDistance(p) > 1)) {
			final ParameterGroup pGroup = findGroup(p);
			updateTable.setAddition(pGroup.getCurrentParameter(), getEffectiveDistance(p).getDistance(), getEffectiveDistance(p).getNeighbor());
		}

		return parameterChanged;
	}

	// Adds an entry directly to the table
	boolean addEntry(final Parameter p, final EstimatedDistance eDistance) {
		EstimatedDistanceList list;

		final ParameterGroup priorGroup = findGroup(p);
		Parameter parameter;
		if (priorGroup == null)
			parameter = null;
		else
			parameter = priorGroup.getCurrentParameter();

		list = updateTable(p);
		final ParameterGroup pGroup = findGroup(p);

		list.updateEstimatedDistance(eDistance);

		if (parameter == null)
			return false;

		return !pGroup.getCurrentParameter().equals(parameter);
	}

	@Override
	public String toString() {
		return table.toString();
	}

	// gets the estimated distance of a parameter
	EstimatedDistance getEffectiveDistance(final Parameter p) {
		final EstimatedDistanceList list = getEffectiveDistanceList(p);
		if (list != null)
			return list.getEffectiveDistance();

		return null;
	}

	// gets the list of estimated distances for the passed parameter
	EstimatedDistanceList getEffectiveDistanceList(final Parameter p) {
		final ParameterGroup pGroup = findGroup(p);
		return table.get(pGroup);
	}

	// Removes empty entries from parameter table
	private void cleanTable() {
		for (final Iterator<ParameterGroup> it = table.keySet().iterator(); it.hasNext();) {
			final ParameterGroup p = it.next();
			if (table.get(p).isEmpty())
				it.remove();
		}
	}

	@Override
	public void saveToXML(final OutputStream os) throws IOException {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		final Element root = doc.createElement(PARAMETER_TABLE);
		doc.appendChild(root);

		for (final ParameterGroup pGroup : table.keySet()) {
			final Element parameter = doc.createElement(PARAMETER);
			parameter.setAttribute(PARAMETER_ID_ATTRIB, pGroup.getCurrentParameter().pretty(taxonomy));

			for (final EstimatedDistance eDistance : table.get(pGroup).getList()) {
				final Element parameterEntry = doc.createElement(PARAMETER_ENTRY);
				parameterEntry.setAttribute(PARAMETER_DISTANCE_ATTRIB, String.valueOf(eDistance.getDistance()));
				parameterEntry.setAttribute(PARAMETER_NEIGHBOR_ATTRIB, eDistance.getNeighbor().toString());
				parameterEntry.setAttribute(OPTIONAL_ENTRY, String.valueOf(eDistance.isOptional()));
				parameter.appendChild(parameterEntry);
				root.appendChild(parameter);
			}
		}

		Transformer transformer = null;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (final TransformerConfigurationException tce) {
			throw new IOException(tce);
		}

		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(os);
		try {
			transformer.transform(source, result);
		} catch (final TransformerException te) {
			throw new IOException(te);
		}
	}

	@Override
	public void readFromXML(final InputStream is) throws IOException {
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException pce) {
			throw new IOException(pce);
		}

		Document document = null;
		try {
			document = docBuilder.parse(is);
		} catch (final SAXException e) {
			throw new IOException(e);
		}

		final NodeList parameters = document.getElementsByTagName(PARAMETER);
		for (int i = 0; i < parameters.getLength(); i++) {
			final Element parameter = (Element) parameters.item(i);
			final String parameterID = parameter.getAttribute(PARAMETER_ID_ATTRIB);
			final NodeList parameterEntries = parameter.getElementsByTagName(PARAMETER_ENTRY);
			for (int j = 0; j < parameterEntries.getLength(); j++) {
				final Element parameterEntry = (Element) parameterEntries.item(j);
				final String neighbor = parameterEntry.getAttribute(PARAMETER_NEIGHBOR_ATTRIB);
				final String distance = parameterEntry.getAttribute(PARAMETER_DISTANCE_ATTRIB);
				final String optional = parameterEntry.getAttribute(OPTIONAL_ENTRY);
				// By default entries are not optional
				try {
					if (optional.isEmpty() || optional.equals("false"))
						addEntry(ParameterFactory.createParameter(parameterID, taxonomy), new EstimatedDistance(Integer.parseInt(distance), new PeerID(neighbor), false));
					else
						addEntry(ParameterFactory.createParameter(parameterID, taxonomy), new EstimatedDistance(Integer.parseInt(distance), new PeerID(neighbor), true));
				} catch (final InvalidParameterIDException ie) {
					throw new IOException(ie);
				}
			}
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof ParameterTable))
			return false;

		final ParameterTable pTable = (ParameterTable) o;
		return this.equalsOptional(pTable) && pTable.equalsOptional(this);
	}

	private boolean equalsOptional(final ParameterTable pTable) {
		for (final ParameterGroup pGroup : table.keySet()) {
			final EstimatedDistanceList list = table.get(pGroup);
			// check if all entries are optional
			if (list.getOptionalEntriesSize() != list.size()) {
				if (!pTable.table.containsKey(pGroup))
					return false;
				final EstimatedDistanceList otherList = pTable.table.get(pGroup);
				if (!list.equals(otherList))
					return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return this.table.hashCode();
	}
}
