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

package dissemination;

import java.util.Set;

import peer.peerid.PeerID;
import serialization.xml.XMLSerializable;
import taxonomy.Taxonomy;
import taxonomy.parameter.Parameter;
import dissemination.newProtocol.ptable.DisseminationDistanceInfo;

public interface ParameterDisseminator extends XMLSerializable, DisseminationDistanceInfo {

	/**
	 * Adds a local parameter to the local parameter table. After finalizing a
	 * change set commit() must called in order to propagate the changes.
	 * 
	 * @param parameter
	 *            the parameter to add.
	 * @return true if the parameter can be added, false otherwise
	 */
	public boolean addLocalParameter(Parameter parameter);

	/**
	 * Removes a local parameter from the local parameter table. After
	 * finalizing a change set commit() must called in order to propagate the
	 * changes.
	 * 
	 * @param parameter
	 *            the parameter to remove
	 * @return true if the parameter can be removed, false otherwise
	 */
	public boolean removeLocalParameter(Parameter parameter);

	/**
	 * Propagates the changes performed using addLocalEntry() or
	 * removeLocalEntry() methods.
	 */
	public void commit();

	/**
	 * Gets the associated taxonomy
	 */
	public Taxonomy getTaxonomy();

	/**
	 * Tells if the passed parameter is a local one (i.e. it is provided by the
	 * current peer)
	 * 
	 * @param parameter
	 *            the parameter to check if it is local
	 * @return true if the parameter is provided by the current node, false
	 *         otherwise
	 */
	public boolean isLocalParameter(Parameter parameter);

	/**
	 * Checks if the specified parameter subsumes a local parameter.
	 * 
	 * @param parameter
	 *            the parameter to check
	 * @return the set of subsumed local parameters
	 */
	public Set<Parameter> subsumesLocalParameter(Parameter parameter);

	/**
	 * Gets the estimated distance, according to the current node's table, to
	 * the passed parameter
	 * 
	 * @param parameter
	 *            the parameter whose distance is estimated
	 * @return the estimated distance for the parameter
	 */
	public int getEstimatedDistance(Parameter parameter);

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
	public int getDistance(Parameter p, PeerID neighbor);

	/**
	 * Gets a set of the known parameters by the dissemination layer.
	 * 
	 * @return a set containing the known parameters
	 */
	public Set<Parameter> getParameters();

	/**
	 * Gets a set of the local parameters.
	 * 
	 * @return a set containing the local parameters
	 */
	public Set<Parameter> getLocalParameters();

	public int getDistanceTo(Parameter p);

	public void setDisseminationTTL(int sameTTL);

}